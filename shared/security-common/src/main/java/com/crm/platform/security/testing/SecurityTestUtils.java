package com.crm.platform.security.testing;

import com.crm.platform.security.context.SecurityContext;
import com.crm.platform.security.context.SecurityContextHolder;
import com.crm.platform.security.jwt.JwtTokenProvider;
import com.crm.platform.security.rbac.Permission;
import com.crm.platform.security.rbac.Role;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Utility class for security testing
 */
public class SecurityTestUtils {
    
    /**
     * Create a mock security context for testing
     */
    public static SecurityContext createMockSecurityContext(UUID userId, UUID tenantId, 
                                                           Set<Role> roles, Set<Permission> permissions) {
        return new SecurityContext(
            userId, tenantId, "testuser", "test@example.com",
            roles, permissions, "test-session", "test-device",
            "127.0.0.1", "Test-Agent",
            System.currentTimeMillis(), System.currentTimeMillis() + 3600000
        );
    }
    
    /**
     * Create a mock security context with admin privileges
     */
    public static SecurityContext createAdminSecurityContext() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Set<Role> roles = Set.of(Role.SUPER_ADMIN);
        Set<Permission> permissions = Role.SUPER_ADMIN.getPermissions();
        
        return createMockSecurityContext(userId, tenantId, roles, permissions);
    }
    
    /**
     * Create a mock security context with specific role
     */
    public static SecurityContext createSecurityContextWithRole(Role role) {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Set<Role> roles = Set.of(role);
        Set<Permission> permissions = role.getPermissions();
        
        return createMockSecurityContext(userId, tenantId, roles, permissions);
    }
    
    /**
     * Create a mock security context with specific permissions
     */
    public static SecurityContext createSecurityContextWithPermissions(Permission... permissions) {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Set<Role> roles = Set.of();
        Set<Permission> permissionSet = Set.of(permissions);
        
        return createMockSecurityContext(userId, tenantId, roles, permissionSet);
    }
    
    /**
     * Set up security context for testing
     */
    public static void setupSecurityContext(SecurityContext context) {
        SecurityContextHolder.setContext(context);
    }
    
    /**
     * Set up admin security context for testing
     */
    public static void setupAdminSecurityContext() {
        setupSecurityContext(createAdminSecurityContext());
    }
    
    /**
     * Set up security context with specific role for testing
     */
    public static void setupSecurityContextWithRole(Role role) {
        setupSecurityContext(createSecurityContextWithRole(role));
    }
    
    /**
     * Set up security context with specific permissions for testing
     */
    public static void setupSecurityContextWithPermissions(Permission... permissions) {
        setupSecurityContext(createSecurityContextWithPermissions(permissions));
    }
    
    /**
     * Clear security context after testing
     */
    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
    
    /**
     * Create a test JWT token
     */
    public static String createTestToken(JwtTokenProvider jwtTokenProvider, UUID userId, UUID tenantId,
                                       List<String> roles, List<String> permissions) {
        return jwtTokenProvider.createAccessToken(userId, tenantId, roles, permissions);
    }
    
    /**
     * Create a test JWT token with admin privileges
     */
    public static String createAdminTestToken(JwtTokenProvider jwtTokenProvider) {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        List<String> roles = List.of(Role.SUPER_ADMIN.getCode());
        List<String> permissions = Role.SUPER_ADMIN.getPermissions().stream()
                                                   .map(Permission::getCode)
                                                   .toList();
        
        return createTestToken(jwtTokenProvider, userId, tenantId, roles, permissions);
    }
    
    /**
     * Create a test JWT token with specific role
     */
    public static String createTestTokenWithRole(JwtTokenProvider jwtTokenProvider, Role role) {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        List<String> roles = List.of(role.getCode());
        List<String> permissions = role.getPermissions().stream()
                                      .map(Permission::getCode)
                                      .toList();
        
        return createTestToken(jwtTokenProvider, userId, tenantId, roles, permissions);
    }
}