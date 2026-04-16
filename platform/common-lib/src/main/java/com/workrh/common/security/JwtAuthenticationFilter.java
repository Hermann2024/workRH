package com.workrh.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
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
        if (applyDemoAuthentication(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        var claims = jwtService.extractAllClaims(token);
        List<String> roles = claims.get("roles", List.class);
        String username = claims.getSubject();
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                username,
                null,
                roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
        );
        Map<String, Object> details = new HashMap<>();
        details.put("tenantId", claims.get("tenantId", String.class));
        Object employeeId = claims.get("employeeId");
        if (employeeId != null) {
            details.put("employeeId", employeeId);
        }
        authenticationToken.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request, response);
    }

    private boolean applyDemoAuthentication(String token) {
        if (!token.startsWith("demo|")) {
            return false;
        }

        String[] parts = token.split("\\|", 5);
        if (parts.length < 4) {
            return false;
        }

        String username = parts[1];
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
        details.put("tenantId", parts[2]);
        if (parts.length == 5 && !parts[4].isBlank()) {
            try {
                details.put("employeeId", Long.parseLong(parts[4]));
            } catch (NumberFormatException ignored) {
                // Keep demo authentication usable even if the optional employee id is absent or malformed.
            }
        }
        authenticationToken.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        return true;
    }
}
