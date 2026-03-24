package com.workrh.common.tenant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class TenantFilterConfig {

    @Bean
    TenantFilter tenantRequestFilter() {
        return new TenantFilter();
    }
}
