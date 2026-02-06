# Part 2: The Security Foundation - JWTs & Tenant Isolation

In **Part 1**, we laid the architectural groundwork. We chose the **Shared Schema** database strategy, where all tenants live in the same database tables, distinguished only by a `tenant_id` column. We discussed how potentially dangerous this is—one missing `WHERE` clause means a massive data breach.

Now, we build the walls.

Security in a multi-tenant system is not just about "Logging In". It is about **Contextual Identity**. When a user sends a request, we need to know two things with absolute certainty:
1.  **Who are they?** (Authentication)
2.  **Where do they belong?** (Tenant Context)

In this part of the series, we will build a production-grade security layer using **Spring Security 6** and **JSON Web Tokens (JWT)**. We will implement a "Two-Lock" security system that prevents tenant spoofing and ensures that even if a user has a valid token, they cannot access another tenant's data.

---

## The "Two-Lock" Security Philosophy

In a single-tenant application, security is usually simple: IF user provides valid password THEN let them in. The application implicitly assumes that if you are a valid user, you belong here.

In a multi-tenant application, this assumption is dangerous. A common attack vector in poorly designed SaaS apps is simple ID manipulation, often called **Insecure Direct Object Reference (IDOR)** at the tenant level:
1.  Attacker signs up for `Tenant A`.
2.  Attacker logs in and gets a valid token.
3.  Attacker takes that token and tries to use it to call an API endpoint for `Tenant B`, perhaps by changing a URL parameter or a header.

If your security layer only checks "Is this token valid?", the attacker gets in. The system sees a valid signature and assumes trust.

We need a system that checks:
1.  **Lock 1: Is the Token Valid?** (Signature verification)
2.  **Lock 2: Does the Token match the Requested Tenant?** (Context verification)

We implement this using a chain of specific filters in Spring Security.

---

## 1. The Keymaster: `JwtTokenService`

Stateless authentication is the gold standard for modern microservices and SaaS. We don't want to store session data in our database or memory (which makes scaling hard). Instead, we issue a self-contained **JWT (JSON Web Token)** that proves the user's identity.

### Anatomy of our Multi-Tenant JWT

A standard JWT has three parts: Header, Payload, and Signature.
The **Payload** is where we transmit Identity. In a single-tenant app, the payload usually contains the `sub` (Subject/User ID) and maybe `exp` (Expiration).

But in our system, a standard JWT isn't enough. We need to burn the **Tenant ID** into the token itself, effectively sealing the user's identity to their organization. We add a custom claim: `tenant`.

Here is our `JwtTokenService`, designed to issue these tenant-aware tokens.

```java
@Service
public class JwtTokenService {

    private final SecretKey secretKey;
    private final long expirationSeconds;

    public JwtTokenService(@Value("${security.jwt.secret}") String secret, 
                           @Value("${security.jwt.expiration}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expiration;
    }

    public String generateToken(String userId, String tenantId, List<String> roles) {
        Instant now = Instant.now();
        
        return Jwts.builder()
                .subject(userId)
                .claim("tenant", tenantId) // <--- CRITICAL: The Tenant Seal
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }
    
    // ... validation methods
}
```

**Why this matters:**
By adding `.claim("tenant", tenantId)`, we are cryptographically binding the user to that tenant. 
The **Signature** (generated using our secret key) covers the entire payload. If the user tries to manually edit the Base64 payload to change `"tenant": "tenant-a"` to `"tenant": "tenant-b"`, the signature verification will strictly fail because they don't have our secret key to re-sign the token.
This means we can trust the `tenant` claim inside the token as absolute truth.

---

## 2. The Gatekeeper: `TenantFilter`

Before we even look at the user's credentials, we must establish **Context**. Which tenant is this request intended for?

We use a custom HTTP header: `X-Tenant-ID`.

The `TenantFilter` is the first line of defense. It sits before the authentication filter. Its job is simple but critical:
1.  Read the `X-Tenant-ID` header.
2.  Verify the tenant exists and is active in the database (`TenantResolver`).
3.  Set the `TenantContext` for the thread.
4.  **Enable the Hibernate Filter** we discussed in Part 1.

```java
@Component
public class TenantFilter extends OncePerRequestFilter {

    private final TenantResolver tenantResolver;
    private final HibernateTenantFilterConfigurer filterConfigurer;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain chain) {
        try {
            // 1. Resolve and Validate Format/Existence
            String tenantId = tenantResolver.resolveTenant(request);
            
            // 2. Set ThreadLocal Context
            TenantContext.setTenantId(tenantId);
            
            // 3. Enable Data Isolation
            filterConfigurer.enableTenantFilter();
            
            // 4. Proceed
            chain.doFilter(request, response);
            
        } catch (TenantNotFoundException e) {
             response.sendError(HttpServletResponse.SC_NOT_FOUND, "Tenant does not exist");
        } catch (TenantDeactivatedException e) {
             response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant subscription is inactive");
        } finally {
            // Cleanup is mandatory in ThreadLocal environments!
            TenantContext.clear();
        }
    }
}
```

**The `TenantResolver` Magic**:
The `TenantResolver` isn't just checking regex strings. It performs a **cached look-up** against a `tenants` table.
*   Does "netflix" exist? Yes.
*   Is "netflix" active? Yes.
*   Does "blockbuster" exist? Yes.
*   Is "blockbuster" active? No (Deactivated). 

If the tenant is deactivated (e.g., they stopped paying), the request is blocked **right here**. We don't even bother checking the user's token or hitting the database for further queries. This is an efficient "fail-fast" mechanism.

---

## 3. The Enforcer: `JwtAuthenticationFilter`

Now that the *context* is set (we know which room we are in), we check the *identity* (does the user have the key to THIS room?).

This is where the **"Two-Lock"** check happens.

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain chain) {
                                    
        String token = extractToken(request);
        
        if (token != null) {
            Claims claims = tokenService.parseAndValidate(token);
            
            // Lock 1: Token is valid (signature checked by parseAndValidate)
            
            String tokenTenant = claims.get("tenant", String.class);
            String requestTenant = TenantContext.getTenantId();
            
            // Lock 2: Context Match
            if (!tokenTenant.equalsIgnoreCase(requestTenant)) {
                 // ALERT: Possible attack or misconfiguration
                 log.warn("Tenant Spoofing Attempt: Token={} vs Request={}", tokenTenant, requestTenant);
                 response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant mismatch");
                 return;
            }
            
            // If we pass both locks, we authenticate the user
            setSecurityContext(claims);
        }
        
        chain.doFilter(request, response);
    }
}
```

This simple `if` statement is the most important security line in the entire application:
`if (!tokenTenant.equalsIgnoreCase(requestTenant))`

It prevents a user from `Tenant A` simply changing the `X-Tenant-ID` header to `Tenant B`. If they do, the token (which is signed by us) says "I belong to A", but the request says "I am visiting B". The filter instantly rejects this mismatch.

---

## 4. Wiring it Together: `SecurityConfig`

Spring Security is powerful, but its configuration using the `SecurityFilterChain` bean can be daunting. We need to assemble our filters in a precise order for this pipeline to work.

If `JwtAuthenticationFilter` runs *before* `TenantFilter`, it will fail logic because `TenantContext` will be null, and we can't perform the comparison check.
If `TenantFilter` runs *after* authentication, we might authenticate a user into a non-existent tenant.

The order matters:
1.  **RateLimitingFilter** (Protect the infrastructure first - drop DDOS attacks before they hit the DB).
2.  **TenantFilter** (Establish context - verify tenant exists).
3.  **AdminJwtAuthenticationFilter** (Check if it's a Super Admin).
4.  **JwtAuthenticationFilter** (Verify tenant user identity within context).

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            // 1. No Sessions. We are stateless.
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 2. Disable CSRF (Cross-Site Request Forgery)
            // CSRF protection relies on browser cookies and server-side sessions.
            // Since we are using stateless JWTs stored (ideally) in localStorage 
            // or correct headers, CSRF is not a relevant attack vector here.
            .csrf(AbstractHttpConfigurer::disable)
            
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            
            // 3. Define the Filter Chain Order
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(tenantFilter, RateLimitingFilter.class)
            // Admin filter runs next...
            .addFilterAfter(jwtAuthenticationFilter, TenantFilter.class) // <--- The Critical Logic
            
            // 4. Authorization Rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll() // Sign up / Login is public
                .requestMatchers("/api/v1/admin/**").hasRole("SYSTEM_ADMIN") // Protected Super Admin Area
                .anyRequest().authenticated() // Everything else requires a valid user
            );
            
        return http.build();
    }
}
```

### The "Super Admin" Backdoor
You'll notice we handle `/api/v1/admin/**` differently. System Admins (the owners of the SaaS platform) operate *outside* the context of a specific tenant. They need to manage the platform itself, create new tenants, and view global dashboards.

For them, we use a separate filter `AdminJwtAuthenticationFilter` (not fully shown here, but present in the codebase) which validates their specific "SYSTEM_ADMIN" token but *skips* the tenant matching check, because their token is global. The `TenantFilter` is also configured with a `shouldNotFilter` method to skip these paths, preventing "Missing X-Tenant-ID" errors for global admin actions.

---

## Security in Action: The Request Lifecycle

Let's trace a request to `GET /api/v1/projects` to see how this all comes together. This is the path a packet takes through our fortress.

### Step 1: Client Request
`Alice`, a user for `tenant-abc`, sends a request to list her projects.
```http
GET /api/v1/projects
Host: api.mysaas.com
X-Tenant-ID: tenant-abc
Authorization: Bearer <JWT_ISSUED_FOR_TENANT_ABC>
```

### Step 2: TenantFilter
1.  Sees `X-Tenant-ID: tenant-abc`.
2.  Checks Cache/DB: "Is tenant-abc valid?" -> YES.
3.  Sets `TenantContext.currentTenant = "tenant-abc"`.
4.  Calls `HibernateTenantFilterConfigurer.enable("tenant-abc")`.
    *   *Now, any database query run on this thread will legally only return data for tenant-abc.*

### Step 3: JwtAuthenticationFilter
1.  Decodes the JWT. Signature matches (Lock 1 passed).
2.  Reads claim inside JWT: `tenant = "tenant-abc"`.
3.  Reads `TenantContext`: `currentTenant = "tenant-abc"`.
4.  Match? YES (Lock 2 passed).
5.  Sets `SecurityContextHolder.authentication = Alice`.

### Step 4: Controller & Service
1.  Controller calls `projectRepository.findAll()`.

### Step 5: Database Query
1.  Hibernate generates SQL: `SELECT * FROM projects`.
2.  Hibernate **Interceptor** adds the `WHERE` clause from Step 2.
3.  Actual SQL sent to DB: `SELECT * FROM projects WHERE tenant_id = 'tenant-abc'`.

The result? Alice gets her data. The specific SQL query is safe, the context is safe, and the user identity is verified.

---

## Conclusion

We have now built the fortress walls.

*   **Part 1** gave us the shared database architecture.
*   **Part 2** (this post) secured the front door with a context-aware, "Two-Lock" JWT implementation using Spring Security.

This setup is robust. It covers the OWASP Top 10 vulnerabilities related to Broken Access Control. It handles tenant deactivation gracefully. It handles spoofing attempts.

But we still have a "Chicken and Egg" problem. How did `tenant-abc` get there in the first place? How did Alice get her user account? We can't insert SQL rows manually for every new customer.

In **Part 3**, we will build the **Tenant Management & Onboarding** module. We will look at how to programmatically create tenants, provision their initial admin users, and manage the lifecycle of a subscription via API.

See you in Part 3!
