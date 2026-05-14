package com.workrh.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.workrh.common.tenant.TenantContext;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(null, true);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void rejectsAuthenticatedRequestWhenTenantHeaderDiffersFromToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/employees");
        request.addHeader("Authorization", "Bearer demo|user@tenant-a.test|tenant-a|HR|42");
        request.addHeader("X-Tenant-Id", "tenant-b");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void usesTokenTenantAsTenantContextWhenHeaderMatches() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/employees");
        request.addHeader("Authorization", "Bearer demo|user@tenant-a.test|tenant-a|HR|42");
        request.addHeader("X-Tenant-Id", "tenant-a");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(TenantContext.getTenantId()).isEqualTo("tenant-a");
    }
}
