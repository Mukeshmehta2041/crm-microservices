package com.crm.platform.security.service;

import com.crm.platform.security.jwt.JwtTokenProvider;
import com.crm.platform.security.rbac.Permission;
import com.crm.platform.security.rbac.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Provider for service-to-service authentication tokens
 */
@Component
public class ServiceAuthenticationProvider {
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Value("${service.auth.system-user-id:00000000-0000-0000-0000-000000000000}")
    private String systemUserId;
    
    @Value("${service.auth.system-tenant-id:00000000-0000-0000-0000-000000000000}")
    private String systemTenantId;
    
    /**
     * Create a service token for inter-service communication
     */
    public String createServiceToken(String serviceName, List<String> permissions) {
        UUID userId = UUID.fromString(systemUserId);
        UUID tenantId = UUID.fromString(systemTenantId);
        
        // Service tokens have system-level permissions
        List<String> roles = List.of(Role.SYSTEM_SERVICE.getCode());
        
        return jwtTokenProvider.createAccessToken(userId, tenantId, roles, permissions, 
                                                 "service-" + serviceName, serviceName);
    }
    
    /**
     * Create a service token with full system access
     */
    public String createSystemServiceToken(String serviceName) {
        return createServiceToken(serviceName, List.of(Permission.SYSTEM_ADMIN.getCode()));
    }
    
    /**
     * Create a service token with specific API permissions
     */
    public String createApiServiceToken(String serviceName, boolean readAccess, boolean writeAccess) {
        List<String> permissions = new java.util.ArrayList<>();
        
        if (readAccess) {
            permissions.add(Permission.API_READ.getCode());
        }
        
        if (writeAccess) {
            permissions.add(Permission.API_WRITE.getCode());
        }
        
        return createServiceToken(serviceName, permissions);
    }
    
    /**
     * Validate if a token is a valid service token
     */
    public boolean isValidServiceToken(String token) {
        try {
            if (!jwtTokenProvider.validateTokenForAccess(token)) {
                return false;
            }
            
            UUID userId = jwtTokenProvider.getUserIdFromToken(token);
            return systemUserId.equals(userId.toString());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get service name from service token
     */
    public String getServiceNameFromToken(String token) {
        return jwtTokenProvider.getDeviceIdFromToken(token);
    }
}