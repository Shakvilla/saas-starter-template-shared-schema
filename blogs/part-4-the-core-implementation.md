# Part 4: The Core - Implementing Shared-Schema Tenant Context & Data Isolation

Welcome to the finale of our deep-dive series on building a Production-Ready Multi-Tenant SaaS with Java and Spring Boot.

So far, we have been architects and managers.
*   **Part 1: The Strategy** - We investigated the database isolation models and chose the **Shared Schema** architecture for its cost-efficiency and operational agility.
*   **Part 2: The Security Foundation** - We designed the **Security Gates** using a "Two-Lock" JWT system to ensure request validity and context integrity.
*   **Part 3: Tenant Management** - We built the **Control Plane** to manage tenant lifecycles, onboarding, and global administration.

Now, we become **Engineers**.

In this final part, we are going to open the hood and look at the engine. We are going to write the low-level code that actually makes "Data Isolation" work locally, reliably, and invisibly. We will answer the hard questions:
*   How do we store "Context" effectively in a multi-threaded web server?
*   How do we use Hibernate Filters to invisibly rewrite SQL queries?
*   How do we prevent lazy developers (ourselves) from accidentally leaking data?
*   What happens when we spawn a new thread with `@Async`?
*   How do we test this without going insane?

Let's build the Core.

---

## 1. The Context Container: `TenantContext`

In a stateless web application (like a standard Spring Boot REST API), every HTTP request is handled by a thread. For the duration of that request—from the moment it hits the Controller to the moment the JSON response is serialized—that thread is "working for" a specific tenant.

We need a global place to store this information so that any class, anywhere in the code (Service, Repository, Utility), can ask: *"Who am I working for right now?"* ignoring the complexity of passing arguments down the stack.

We use `ThreadLocal`.

### The Implementation

```java
public final class TenantContext {

    // 1. Storage
    private static final InheritableThreadLocal<String> CURRENT_TENANT = new InheritableThreadLocal<>();

    private TenantContext() {} // Static access only

    /**
     * Set the tenant ID for the current thread and its children.
     * @param tenantId The validated tenant ID
     */
    public static void setTenantId(String tenantId) {
        log.trace("Setting TenantContext: {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Get the tenant ID for the current thread.
     * @return tenantId or null
     */
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * CRITICAL: Must be called in a finally block to prevent memory leaks.
     */
    public static void clear() {
        log.trace("Clearing TenantContext");
        CURRENT_TENANT.remove();
    }
    
    // 3. Safety Check
    public static String requireTenantId() {
        String tenantId = getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is missing! Are you running outside a request?");
        }
        return tenantId;
    }
}
```

### Why `InheritableThreadLocal`?
Standard `ThreadLocal` stores data for the *current* thread only.
`InheritableThreadLocal` is smarter. When a thread allows a child thread to be created, the child *automatically inherits* the values from the parent at the moment of creation.

**Scenario**: You are processing a signup request. You want to send a "Welcome Email" asynchronously to avoid blocking the user response.
1.  **Main Request Thread**: Sets `TenantContext="tenant-a"`.
2.  **Main Request Thread**: Spawns `EmailThread`.
3.  **EmailThread**: *Inherits* `TenantContext="tenant-a"`.

If we used simple `ThreadLocal`, the `EmailThread` would wake up with a `null` context. Your email service would try to load the "Welcome Email Template" and fail because it doesn't know *which* tenant's template to load. Or worse, it might load the default system template, leaking branding.

### The Danger Zone: Thread Pooling
⚠️ **Warning**: `InheritableThreadLocal` works great for *newly created* threads. But application servers (Tomcat/Jetty) and `@Async` executors use **Thread Pools**. They reuse old threads.
If you reuse a thread, it might still have the *old* tenant context from a previous job! This is called "Context Leakage".

We will solve this decisively later with the `TenantAwareTaskDecorator`.

---

## 2. The Database Guard: Hibernate Filters

This is the secret sauce of the Shared Schema approach. Standard JPA (Java Persistence API) does not have a comprehensive "Global Filter" concept; this is a specific, powerful feature of **Hibernate**, the default JPA provider in Spring Boot.

A Hibernate Filter is like a dynamic, global `WHERE` clause that you can turn on and off at will.

### Step A: Define the Filter
We can define this on a `package-info.java` or any entity. For clarity and encapsulation, we usually put it on the Base Entity.

```java
@FilterDef(
    name = "tenantFilter", 
    parameters = @ParamDef(name = "tenantId", type = String.class)
)
```
This tells Hibernate: "I have a filter named `tenantFilter` that accepts a String parameter called `tenantId`."

### Step B: The Configuration Bean
Defining the filter doesn't turn it on. We need a class that knows how to turn this filter *activate* it and feed it the correct ID from our `TenantContext`.

```java
@Component
public class HibernateTenantFilterConfigurer {

    @PersistenceContext
    private EntityManager entityManager;

    public void enableTenantFilter() {
        String tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            // This is a defense-in-depth check. 
            // The TenantFilter should have caught this, but we never trust just one layer.
            throw new IllegalStateException("TenantContext missing when enabling Hibernate Filter.");
        }

        // Unwrap standard JPA EntityManager to get the Hibernate Session
        Session session = entityManager.unwrap(Session.class);

        // ACTIVATE THE MAGIC
        Filter filter = session.enableFilter("tenantFilter");
        filter.setParameter("tenantId", tenantId);
    }
    
    public void disableTenantFilter() {
        Session session = entityManager.unwrap(Session.class);
        session.disableFilter("tenantFilter");
    }
}
```

This method `enableTenantFilter()` is called by our `TenantFilter` (the HTTP Filter from Part 2) at the very beginning of the request. Once enabled, **Hibernate rewrites every SQL query** generated by that Session.

`SELECT * FROM users` -> `SELECT * FROM users WHERE tenant_id = 'current-id'`

---

## 3. The Foundation: `BaseTenantEntity`

We don't want to type `private String tenantId` in every single class. And we definitely don't want to trust developers to manually set the ID when saving. A developer *will* forget, and that data *will* be orphaned or exposed.

We create a **Layer Supertype**.

```java
@MappedSuperclass
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId") // <--- The Enforcement
public class BaseTenantEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    // ...

    @PrePersist
    protected void assignTenant() {
        String currentTenant = TenantContext.getTenantId();
        
        if (currentTenant == null) {
            throw new TenantMissingException("Cannot save entity: Tenant Context is missing. Context is required for @PrePersist.");
        }
        
        this.tenantId = currentTenant;
    }
}
```

**The `@PrePersist` Hook**:
This is our safety net. Before Hibernate sends an `INSERT` statement to the database, it runs this method.
1.  Grab the ID from the context.
2.  Set it on the entity.

This makes the `tenantId` field effectively read-only for developers. You can't set it manually (or if you do, it might get overwritten or rejected). The *Context* dictates the *Data*.

**Note regarding `updatable = false`**:
This is crucial. It means that once a row is inserted, the `tenant_id` column can **never** be changed by an `UPDATE` statement. This prevents a catastrophic bug where an entity is accidentally "moved" to another tenant.

---

## 4. Solving the Async Problem

As mentioned earlier, `InheritableThreadLocal` is tricky with Thread Pools (like Spring's `@Async`).
When Spring executes an async task, it grabs a thread from the pool. That thread might be "clean" (no context), or it might carry garbage from a previous usage.

We need to explicitly **Decorate** the task to copy the context from the Main Thread to the Worker Thread safely.

```java
public class TenantAwareTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 1. Capture context from the Main Thread (Caller)
        String callerTenantId = TenantContext.getTenantId();
        
        return () -> {
            try {
                // 2. Restore context in the Worker Thread
                if (callerTenantId != null) {
                    TenantContext.setTenantId(callerTenantId);
                }
                // 3. Run the actual task
                runnable.run();
            } finally {
                // 4. CLEANUP (Crucial for thread pools!)
                TenantContext.clear();
            }
        };
    }
}
```

Now we just register this in our Spring Async config:

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // ... set core pool size etc ...
        executor.setTaskDecorator(new TenantAwareTaskDecorator()); // <--- HERE
        executor.initialize();
        return executor;
    }
}
```

Now, you can write:
```java
@Async
public void sendInvoiceEmail() {
    // This works!
    // The framework has automatically injected the correct tenant context.
    User admin = userRepository.findByRole("ADMIN"); 
}
```
Even though this runs in a separate thread, the `TaskDecorator` ensured `TenantContext` was copied over, so the `userRepository` correctly filters for the right tenant.

---

## 5. Potential Pitfalls: Where Dragons Live

Even with this robust setup, there are edge cases in Shared Schema that trips up even experienced engineers.

### A. The `@ManyToOne` Limitation
Hibernate Filters have a limitation: they usually apply to the **root entity** of a query.
If you do `userRepository.findAll()`, the filter applies to `User`.
But if you do:
```java
@OneToMany(mappedBy = "user")
List<Order> orders;
```
And you access `user.getOrders()`, Hibernate *does* apply the filter to the `Orders` query (because it generates a new SELECT).

However, if you write a Custom JPQL query:
`SELECT u FROM User u JOIN FETCH u.orders o`
Sometimes Hibernate's parser struggles to apply the filter to the joined `orders` table depending on the Hibernate version and join type.
**Solution**: Always ensure that *every* entity in your domain extends `BaseTenantEntity` and has the filter applied, even child entities. Test your deep fetch queries.

### B. Unique Constraints
We mentioned this in Part 1, but it bears repeating in the core implementation.
You cannot use standard `@Column(unique=true)`.
You must use `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "email"}))`.
If you forget this, Tenant B cannot register "admin@gmail.com" if Tenant A already has that email.

### C. Native Queries
The Hibernate Filter **does not** apply to Native SQL queries (`@Query(nativeQuery=true)`).
If you write `SELECT * FROM users`, you get **ALL USERS**.
**Solution**: Prohibit Native Queries in code reviews unless they manually include `AND tenant_id = :tenantId`.

---

## 6. How to Test It

Testing multi-tenancy is hard because unit tests usually single-threaded and context-free.

We create a test utility to simulate tenants.

```java
public class TenantTestUtils {

    public static void runAs(String tenantId, Runnable block) {
        try {
            TenantContext.setTenantId(tenantId);
            block.run();
        } finally {
            TenantContext.clear();
        }
    }
}
```

Now our tests are readable and safe:

```java
@Test
void testIsolation() {
    // 1. Setup Tenant A
    TenantTestUtils.runAs("tenant-a", () -> {
        userRepository.save(new User("alice"));
    });

    // 2. Setup Tenant B
    TenantTestUtils.runAs("tenant-b", () -> {
        userRepository.save(new User("bob"));
    });
    
    // 3. Verify Isolation
    TenantTestUtils.runAs("tenant-a", () -> {
        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getName()).isEqualTo("alice");
    });
}
```

---

## Series Conclusion

We started this series with a goal: **To build a production-ready Multi-Tenant SaaS.**

We didn't take shortcuts.
*   We didn't just add a `where` clause to our repositories. We architected a **Shared Schema** system (Part 1).
*   We didn't just use Basic Auth. We built a **Two-Lock JWT Security** system (Part 2).
*   We didn't just insert rows manually. We built a **Tenant Management Control Plane** (Part 3).
*   And today, we implemented the low-level **Core Context & Isolation Engine** (Part 4).

This architecture is scalable, secure, and maintainable. It is the exact architecture used by many of the successful B2B SaaS platforms you use today. It allows you to scale to thousands of tenants without managing thousands of databases.

The code is real. The strategy is proven. Now, it is up to you to build the next great SaaS product on top of it.

Happy Coding!
