package com.workrh.common.tenant;

import com.workrh.common.web.BadRequestException;

public final class TenantContext {

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return TENANT.get();
    }

    public static String requireTenantId() {
        String tenantId = getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new BadRequestException("Tenant context is required");
        }
        return tenantId;
    }

    public static void clear() {
        TENANT.remove();
    }
}
