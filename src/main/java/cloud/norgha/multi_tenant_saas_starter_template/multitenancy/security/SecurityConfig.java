package cloud.norgha.multi_tenant_saas_starter_template.multitenancy.security;


import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.persistence.HibernateTenantFilterConfigurer;
import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant.TenantFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration}")
    private long jwtExpiration;


    @Bean
    public JwtTokenService jwtTokenService(){
        return new JwtTokenService(jwtSecret, jwtExpiration);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HibernateTenantFilterConfigurer filterConfigurer) throws Exception{
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers("/auth/**").permitAll()
                                .anyRequest().authenticated())
                .addFilterBefore(
                        new TenantFilter(filterConfigurer),
                        UsernamePasswordAuthenticationFilter.class

                        )
                .addFilterAfter(new JwtAuthenticationFilter(jwtTokenService(), TenantFilter.class

                        ))
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

}
