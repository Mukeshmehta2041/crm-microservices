package com.crm.platform.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for managing tenant context throughout the application
 */
@Service
public class TenantContextService {

    private static final Logger logger = LoggerFactory.getLogger(TenantContextService.class);

    private static final ThreadLocal<UUID> currentTenantId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentTenantSubdomain = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> isTenantValidated = new ThreadLocal<>();

    /**
     * Set the current tenant context
     */
    public void setCurrentTenant(UUID tenantId, String subdomain) {
        if (tenantId == null) {
            logger.warn("Attempting to set null tenant ID");
            return;
        }

        currentTenantId.set(tenantId);
        currentTenantSubdomain.set(subdomain);
        isTenantValidated.set(false);
        
        logger.debug("Set tenant context: {} ({})", tenantId, subdomain);
    }

    /**
     * Get the current tenant ID
     */
    public UUID getCurrentTenantId() {
        return currentTenantId.get();
    }

    /**
     * Get the current tenant subdomain
     */
    public String getCurrentTenantSubdomain() {
        return currentTenantSubdomain.get();
    }

    /**
     * Check if tenant context is set
     */
    public boolean hasTenantContext() {
        return currentTenantId.get() != null;
    }

    /**
     * Check if current tenant is validated
     */
    public boolean isTenantValidated() {
        return Boolean.TRUE.equals(isTenantValidated.get());
    }

    /**
     * Mark current tenant as validated
     */
    public void markTenantAsValidated() {
        isTenantValidated.set(true);
        logger.debug("Marked tenant as validated: {}", currentTenantId.get());
    }

    /**
     * Validate that the provided tenant ID matches the current context
     */
    public boolean validateTenantAccess(UUID tenantId) {
        UUID currentTenant = getCurrentTenantId();
        
        if (currentTenant == null) {
            logger.warn("No tenant context set for validation");
            return false;
        }

        if (tenantId == null) {
            logger.warn("Null tenant ID provided for validation");
            return false;
        }

        boolean isValid = currentTenant.equals(tenantId);
        
        if (!isValid) {
            logger.warn("Tenant access violation: current={}, requested={}", currentTenant, tenantId);
        }

        return isValid;
    }

    /**
     * Ensure tenant context is set, throw exception if not
     */
    public UUID requireTenantContext() {
        UUID tenantId = getCurrentTenantId();
        if (tenantId == null) {
            throw new TenantContextException("Tenant context is required but not set");
        }
        return tenantId;
    }

    /**
     * Ensure tenant is validated, throw exception if not
     */
    public void requireValidatedTenant() {
        requireTenantContext();
        if (!isTenantValidated()) {
            throw new TenantContextException("Tenant context is not validated");
        }
    }

    /**
     * Clear the current tenant context
     */
    public void clearTenantContext() {
        UUID tenantId = currentTenantId.get();
        if (tenantId != null) {
            logger.debug("Clearing tenant context: {}", tenantId);
        }
        
        currentTenantId.remove();
        currentTenantSubdomain.remove();
        isTenantValidated.remove();
    }

    /**
     * Execute code within a specific tenant context
     */
    public <T> T executeInTenantContext(UUID tenantId, String subdomain, TenantContextCallback<T> callback) {
        UUID previousTenantId = getCurrentTenantId();
        String previousSubdomain = getCurrentTenantSubdomain();
        Boolean previousValidated = isTenantValidated.get();

        try {
            setCurrentTenant(tenantId, subdomain);
            return callback.execute();
        } finally {
            // Restore previous context
            if (previousTenantId != null) {
                currentTenantId.set(previousTenantId);
                currentTenantSubdomain.set(previousSubdomain);
                isTenantValidated.set(previousValidated);
            } else {
                clearTenantContext();
            }
        }
    }

    /**
     * Get tenant context information for debugging
     */
    public TenantContextInfo getTenantContextInfo() {
        return new TenantContextInfo(
            getCurrentTenantId(),
            getCurrentTenantSubdomain(),
            isTenantValidated(),
            Thread.currentThread().getName()
        );
    }

    /**
     * Functional interface for tenant context callbacks
     */
    @FunctionalInterface
    public interface TenantContextCallback<T> {
        T execute();
    }

    /**
     * Tenant context information holder
     */
    public static class TenantContextInfo {
        private final UUID tenantId;
        private final String subdomain;
        private final boolean validated;
        private final String threadName;

        public TenantContextInfo(UUID tenantId, String subdomain, boolean validated, String threadName) {
            this.tenantId = tenantId;
            this.subdomain = subdomain;
            this.validated = validated;
            this.threadName = threadName;
        }

        public UUID getTenantId() { return tenantId; }
        public String getSubdomain() { return subdomain; }
        public boolean isValidated() { return validated; }
        public String getThreadName() { return threadName; }

        @Override
        public String toString() {
            return String.format("TenantContext{tenantId=%s, subdomain='%s', validated=%s, thread='%s'}", 
                               tenantId, subdomain, validated, threadName);
        }
    }

    /**
     * Exception thrown when tenant context is invalid or missing
     */
    public static class TenantContextException extends RuntimeException {
        public TenantContextException(String message) {
            super(message);
        }

        public TenantContextException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}