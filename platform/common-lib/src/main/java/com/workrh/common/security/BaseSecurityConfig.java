package com.workrh.common.security;

import com.workrh.common.tenant.TenantFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@EnableMethodSecurity
public class BaseSecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            TenantFilter tenantFilter)
            throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                new AntPathRequestMatcher("/actuator/health"),
                                new AntPathRequestMatcher("/actuator/health/**"),
                                new AntPathRequestMatcher("/actuator/info"),
                                new AntPathRequestMatcher("/error"),
                                new AntPathRequestMatcher("/swagger-ui/**"),
                                new AntPathRequestMatcher("/v3/api-docs/**"),
                                new AntPathRequestMatcher("/api/auth/**"),
                                new AntPathRequestMatcher("/api/subscriptions/plans", "GET"),
                                new AntPathRequestMatcher("/api/subscriptions/bootstrap", "POST"),
                                new AntPathRequestMatcher("/api/subscriptions/webhooks/stripe", "POST")
                        ).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(tenantFilter, AuthorizationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, AuthorizationFilter.class)
                .build();
    }
}
