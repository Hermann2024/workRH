package com.workrh.common.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.workrh.common.tenant.TenantContext;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final boolean demoAuthenticationEnabled;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            @Value("${security.demo-authentication-enabled:false}") boolean demoAuthenticationEnabled
    ) {
        this.jwtService = jwtService;
        this.demoAuthenticationEnabled = demoAuthenticationEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        if (applyDemoAuthentication(request, response, token)) {
            filterChain.doFilter(request, response);
            return;
        }
        Claims claims;
        try {
            claims = jwtService.extractAllClaims(token);
            if (!jwtService.isTokenValid(claims)) {
                filterChain.doFilter(request, response);
                return;
            }
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired authentication token");
            return;
        }

        List<String> roles = claims.get("roles", List.class);
        String username = claims.getSubject();
        String tokenTenantId = claims.get("tenantId", String.class);
        if (!enforceTenantMatch(request, response, tokenTenantId)) {
            return;
        }
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                username,
                null,
                roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
        );
        Map<String, Object> details = new HashMap<>();
        details.put("tenantId", tokenTenantId);
        Object employeeId = claims.get("employeeId");
        if (employeeId != null) {
            details.put("employeeId", employeeId);
        }
        authenticationToken.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        TenantContext.setTenantId(tokenTenantId);
        filterChain.doFilter(request, response);
    }

    private boolean applyDemoAuthentication(HttpServletRequest request, HttpServletResponse response, String token)
            throws IOException {
        if (!demoAuthenticationEnabled) {
            return false;
        }
        if (!token.startsWith("demo|")) {
            return false;
        }

        String[] parts = token.split("\\|", 5);
        if (parts.length < 4) {
            return false;
        }

        String username = parts[1];
        String tokenTenantId = parts[2];
        if (!enforceTenantMatch(request, response, tokenTenantId)) {
            return true;
        }
        List<SimpleGrantedAuthority> authorities = List.of(parts[3].split(",")).stream()
                .filter(role -> !role.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                username,
                null,
                authorities
        );
        Map<String, Object> details = new HashMap<>();
        details.put("tenantId", tokenTenantId);
        if (parts.length == 5 && !parts[4].isBlank()) {
            try {
                details.put("employeeId", Long.parseLong(parts[4]));
            } catch (NumberFormatException ignored) {
                // Keep demo authentication usable even if the optional employee id is absent or malformed.
            }
        }
        authenticationToken.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        TenantContext.setTenantId(tokenTenantId);
        return true;
    }

    private boolean enforceTenantMatch(HttpServletRequest request, HttpServletResponse response, String tokenTenantId)
            throws IOException {
        if (tokenTenantId == null || tokenTenantId.isBlank()) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing tenant in authentication token");
            return false;
        }

        String headerTenantId = request.getHeader("X-Tenant-Id");
        if (headerTenantId != null
                && !headerTenantId.isBlank()
                && !headerTenantId.trim().equalsIgnoreCase(tokenTenantId.trim())) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant header does not match authentication token");
            return false;
        }

        return true;
    }
}
