package com.crm.platform.common.util;

import java.util.UUID;

/**
 * Thread-local tenant context for multi-tenant operations
 */
public class TenantContext {
    
    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_NAME = new ThreadLocal<>();
    
    public static void setTenantId(UUID tenantId) {
        TENANT_ID.set(tenantId);
    }
    
    public static UUID getTenantId() {
        return TENANT_ID.get();
    }
    
    public static void setTenantName(String tenantName) {
        TENANT_NAME.set(tenantName);
    }
    
    public static String getTenantName() {
        return TENANT_NAME.get();
    }
    
    public static void clear() {
        TENANT_ID.remove();
        TENANT_NAME.remove();
    }
    
    public static boolean hasTenantId() {
        return TENANT_ID.get() != null;
    }
}