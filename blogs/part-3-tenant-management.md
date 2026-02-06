# Part 3: Tenant Management - The SaaS Control Plane

In the previous parts of this series, we have established a solid foundation.
*   **Part 1: The Strategy** defined our **Shared Schema** architecture for efficient data isolation using a discriminator column.
*   **Part 2: The Security Foundation** built the **Security Layer**, using JWTs and Filters to strictly enforce that isolation at the request level using a "Two-Lock" mechanism.

We have a secure, isolated database and a secure API pipeline. But we are missing one critical component: **The Tenants themselves.**

A SaaS application is not static code sitting on a server. It is a living, breathing ecosystem where customers (tenants) sign up, upgrade plans, change settings, get suspended for non-payment, and eventually churn. We need a way to manage this lifecycle programmatically. We need a **Control Plane**.

In this article, we will build the **Tenant Management Module**. We will move away from the "Tenant's data" for a moment and focus on the "Platform's data". This is the layer that sits *above* your customers.

---

## The "Meta-Data" Challenge: Platform vs. Tenant

In a Multi-Tenant system, one of the most confusing aspects for new developers is the distinction between the two types of data models. We must rigorously separate them in our minds and our code:

1.  **Tenant Data**: This is the data *owned* by the customer. The users, orders, products, and invoices that belong to `Netflix`. This data exists *inside* the isolation boundary. It requires a `tenant_id` to be accessed. ($ \text{Tenant Context} = \text{Required} $).
2.  **Platform Data**: This is the data *about* the customers. The list of tenants, their subscription tiers, their billing status, and system-wide configurations. This data exists *outside* the isolation boundary. ($ \text{Tenant Context} = \text{None} $).

If you recall from Part 2, our `TenantFilter` enforces that *every* request must have a valid `X-Tenant-ID`. But this creates a paradox:
*   If a new customer wants to sign up, they don't *have* a tenant ID yet. How do they call the API?
*   If a System Admin wants to list all tenants to see who is paying, they shouldn't be restricted to a single tenant's view. They need the "God View".

We need a dedicated slice of our application that operates **outside** the tenant context. We call this the **Admin Module**.

---

## 1. The Tenant Entity

First, we need to store the tenants. This is a special entity. Unlike `User` or `Product`, it does **not** extend `BaseTenantEntity`. It does not have a `tenant_id` column because it *is* the tenant. It is the root of the hierarchy.

```java
@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @NotBlank(message = "Tenant ID is required")
    // e.g., "google", "netflix", "acme-corp"
    private String id;

    @NotBlank(message = "Tenant name is required")
    @Column(nullable = false)
    // e.g., "Google Inc.", "Netflix Services"
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Tenant() {}
    
    // Constructors, Getters, Setters...
}
```

**Key Architectural Decisions:**
1.  **String ID (Slug)**: We use a human-readable String ID instead of a UUID. Why? Because this ID often appears in public-facing contexts. It might be in the subdomain (`netflix.mysaas.com`), the path (`mysaas.com/app/netflix`), or the headers. It is much easier to debug `X-Tenant-ID: netflix` than `X-Tenant-ID: 550e8400-e29b-41d4-a716-446655440000`. It also allows tenants to have a recognizable identity.
2.  **Active Flag**: The `active` boolean is our global "Kill Switch". As we saw in Part 2's `TenantResolver`, if this flag is false, the tenant is effectively erased from the system—no user can log in, no API calls will succeed. This allows for instant provisioning and de-provisioning without deleting data.

---

## 2. The Tenant Service & Caching Strategy

Querying the database for *every single HTTP request* to check if a tenant exists is a massive performance bottleneck. If you have 10,000 requests per second, that's 10,000 extra SELECT statements just for validation. That is `10,000 * 60 * 60 * 24` unnecessary queries a day.

We solve this with **Spring Cache**.

We need a strategy that prioritizes read speed (since we read 1000x more than we write) but ensures consistency.

```java
@Service
@Transactional(readOnly = true)
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;

    @Override
    @Transactional
    public TenantResponseDto createTenant(CreateTenantRequestDto request) {
        // Enforce uniqueness
        if (tenantRepository.existsById(request.id())) {
            throw new IllegalArgumentException("Tenant already exists with ID: " + request.id());
        }

        Tenant tenant = new Tenant(request.id(), request.name());
        Tenant saved = tenantRepository.save(tenant);
        
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tenants", key = "#id") // <--- CRITICAL: Cache Invalidation
    public void deactivateTenant(String id) {
        log.info("Deactivating tenant: {}", id);
        
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        tenant.setActive(false);
        tenantRepository.save(tenant);
        
        // The @CacheEvict annotation ensures that the entry is removed from Redis/Caffeine immediately.
        // The next time TenantResolver asks for this tenant, it will be forced to fetch the "false" status from DB.
    }
}
```

**The Caching Lifecycle**:
1.  **Request 1**: `GET /api/users` (Header: `X-Tenant-ID: apple`).
2.  **TenantResolver**: Checks Cache(`"apple"`). Miss. Calls DB. Gets `Tenant(active=true)`. Puts in Cache. Returns True.
3.  **Request 2...1000**: `GET /api/users`.
4.  **TenantResolver**: Checks Cache(`"apple"`). Hit. Returns True instantly. **Zero DB load.**
5.  **Admin Action**: `deactivateTenant("apple")`.
6.  **TenantService**: Updates DB `active=false`. Evicts Cache(`"apple"`).
7.  **Request 1001**: `GET /api/users`.
8.  **TenantResolver**: Checks Cache(`"apple"`). Miss (was evicted). Calls DB. Gets `Tenant(active=false)`. Puts in Cache. Throws `TenantDeactivatedException`. **Access Denied.**

This gives us the speed of in-memory checks with the consistency of database transactions.

---

## 3. The Super Admin API

Who calls `createTenant`? The **System Admin**.

We need a secured API endpoint that allows the distinct "System Admin" role to manage these resources. This API sits *beside* your tenant APIs, but it is guarded by a different set of permissions.

```java
@RestController
@RequestMapping("/api/v1/admin/tenants")
@Tag(name = "Tenant Management", description = "Platform-level tenant CRUD operations")
public class TenantAdminController {

    private final TenantService tenantService;

    // ... constructor

    @PostMapping
    @PreAuthorize("hasAuthority('manage_tenants')") // <--- RBAC Check
    public ResponseEntity<TenantResponseDto> createTenant(@Valid @RequestBody CreateTenantRequestDto request) {
        TenantResponseDto response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('manage_tenants')")
    public ResponseEntity<Void> deactivateTenant(@PathVariable String id) {
        tenantService.deactivateTenant(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping
    @PreAuthorize("hasAuthority('view_tenants')")
    public ResponseEntity<List<TenantResponseDto>> listTenants() {
        return ResponseEntity.ok(tenantService.listTenants());
    }
}
```

**Security Context**:
Notice the `@PreAuthorize("hasAuthority('manage_tenants')")`.
This is **Role-Based Access Control (RBAC)** in action.
*   System Admins login via a special flow.
*   They receive a JWT that contains `roles: ["SYSTEM_ADMIN"]` and permissions `["manage_tenants", "view_tenants"]`.
*   The `AdminJwtAuthenticationFilter` validates this token.
*   The `TenantFilter` sees the path starts with `/admin/` and **skips** the `X-Tenant-ID` check.

This allows the Admin to operate "above" the tenants, managing the container rather than the contents.

---

## 4. The Onboarding Flow

Now let's trace the full lifecycle of a new customer signing up. This is the **Onboarding Workflow**.

It's rarely as simple as just "Insert Tenant". Doing so creates an empty shell. A tenant with no users cannot be logged into. Usually, we need to:
1.  **Create Tenant**: The record in the `tenants` table.
2.  **Create Admin User**: The first user *inside* that tenant (e.g., the CEO or IT Manager) who will invite others.
3.  **Seed Data**: New tenants might need default settings, categories, notification templates, or welcome messages.

Here is how we orchestrate this.

```java
// Logic inside a higher-level "OnboardingService"

@Transactional
public void onboardNewCustomer(SignupRequest request) {
    log.info("Starting onboarding for company: {}", request.companyName());

    // 1. Create the Platform Tenant
    // This happens in the "Platform Context" (no tenant ID set yet)
    TenantResponseDto tenant = tenantService.createTenant(
        new CreateTenantRequestDto(request.tenantId(), request.companyName())
    );
    
    // 2. Switch Context to the specific Tenant to create their admin user
    // We must manually enter the "Tenant Mode" for the UserService to work correctly
    try {
        TenantContext.setTenantId(request.tenantId()); // <--- THE CONTEXT SWITCH
        
        // 3. Create the initial user
        // Because TenantContext is set, this user is automatically saved 
        // with tenant_id = request.tenantId() thanks to BaseTenantEntity logic!
        userService.createUser(new CreateUserDto(
            request.email(), 
            request.password(), 
            request.fullName(),
            "ROLE_ADMIN" // Tenant Admin, not System Admin
        ));
        
        // 4. Any other seeding...
        // preferenceService.createDefaultPreferences();
        
    } finally {
        // CRITICAL: Always clear context to avoid polluting the thread
        TenantContext.clear();
    }
    
    log.info("Onboarding complete for: {}", request.tenantId());
}
```

**The "Context Switch" Pattern**:
This is a powerful pattern. Since our `UserService` relies on `TenantContext` to know where to save users (via `BaseTenantEntity`'s `@PrePersist`), we can programmatically "impersonate" the new tenant for a brief millisecond to create their initial data.
To the `UserService`, it looks like a normal request from inside the tenant.
To the `SystemAdmin`, it is a coordinated orchestration step.

---

## 5. Operations: Life as a SaaS Operator

With this system in place, "Operations" becomes an API interaction. You are no longer SSH-ing into databases to fix things.

**Scenario A: A customer calls support saying "We can't login!"**
1.  Support Agent opens their internal dashboard.
2.  Dashboard calls: `GET /api/v1/admin/tenants/acme-corp`
3.  Response: `{"active": true, "name": "Acme Corp"}`.
4.  Agent sees `active: true`. Okay, it's not a billing suspension. "Have you forgotten your password?"

**Scenario B: A customer fails to pay their invoice.**
1.  **Automated Webhook**: Stripe sends a "payment.failed" webhook to our backend.
2.  **Deactivation**: Our webhook handler validates the event and calls `tenantService.deactivateTenant("acme-corp")`.
3.  **Result**: The cache is evicted. The very next request from *any* user at Acme Corp receives a `403 Forbidden: Tenant Inactive`. The entire organization is paused instantly.
4.  **UX**: The frontend catches the 403 and shows a "Please update your billing method" screen.

**Scenario C: Renaming a Tenant**
1.  "Acme Corp" rebrands to "Mega Corp".
2.  Admin calls `PUT /api/v1/admin/tenants/acme-corp` with `{"name": "Mega Corp"}`.
3.  The ID (`acme-corp`) stays the same (immutable IDs are important!), but the display name updates instantly across the platform.

---

## Conclusion

We have successfully moved "up the stack".
*   We started in the database (**Part 1**) defining the schema.
*   We secured the request pipeline (**Part 2**) ensuring valid packets.
*   We have now built the control plane (**Part 3**) to manage the ecosystem.

We have a functioning SaaS platform. We can create tenants, users can log in, and data is secure.

But there is one final piece of the puzzle. Throughout these three articles, we have constantly referred to "The Magic".
*   "Hibernate automatically adds the filter..."
*   "InheritableThreadLocal automatically passes context..."
*   "BaseTenantEntity automatically sets the ID..."

How exactly did we implement that? How do `InheritableThreadLocal` variables actually work in a high-concurrency web server? And what specific Hibernate configurations are required to make `@Filter` work seamlessly without crashing your connection pool?

In **Part 4: The Core**, we will take a microscope to the low-level code implementation. We will dissect the `BaseTenantEntity`, the `HibernateTenantFilterConfigurer`, and the `TenantContext` to understand the nuts and bolts of the engine we have built.

Get ready for some deep Java.
