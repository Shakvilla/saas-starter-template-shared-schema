package cloud.norgha.multi_tenant_saas_starter_template.multitenancy.security;


import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.persistence.HibernateTenantFilterConfigurer;
import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant.TenantFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HibernateTenantFilterConfigurer filterConfigurer) throws Exception{
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth ->
                        auth.anyRequest().authenticated())
                .addFilterBefore(new TenantFilter(filterConfigurer),
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
                        )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
