package com.workrh.common.tenant;

import com.workrh.common.web.BadRequestException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs")
                || "/api/subscriptions/plans".equals(path)
                || "/api/subscriptions/catalog/readiness".equals(path)
                || "/api/subscriptions/webhooks/stripe".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String tenantId = request.getHeader("X-Tenant-Id");
            if (tenantId == null || tenantId.isBlank()) {
                String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (authorization != null && authorization.startsWith("Bearer ")) {
                    filterChain.doFilter(request, response);
                    return;
                }
                throw new BadRequestException("Missing X-Tenant-Id header");
            }
            TenantContext.setTenantId(tenantId.trim());
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
