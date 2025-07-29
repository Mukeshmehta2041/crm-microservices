package com.crm.platform.security.context;

import com.crm.platform.security.rbac.Permission;
import com.crm.platform.security.rbac.Role;

import java.util.Set;
import java.util.UUID;

/**
 * Security context holding current user authentication and authorization information
 */
public class SecurityContext {
    
    private final UUID userId;
    private final UUID tenantId;
    private final String username;
    private final String email;
    private final Set<Role> roles;
    private final Set<Permission> permissions;
    private final String sessionId;
    private final String deviceId;
    private final String ipAddress;
    private final String userAgent;
    private final long tokenIssuedAt;
    private final long tokenExpiresAt;
    
    public SecurityContext(UUID userId, UUID tenantId, String username, String email,
                          Set<Role> roles, Set<Permission> permissions, String sessionId,
                          String deviceId, String ipAddress, String userAgent,
                          long tokenIssuedAt, long tokenExpiresAt) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.permissions = permissions;
        this.sessionId = sessionId;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.tokenIssuedAt = tokenIssuedAt;
        this.tokenExpiresAt = tokenExpiresAt;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public Set<Role> getRoles() {
        return roles;
    }
    
    public Set<Permission> getPermissions() {
        return permissions;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public long getTokenIssuedAt() {
        return tokenIssuedAt;
    }
    
    public long getTokenExpiresAt() {
        return tokenExpiresAt;
    }
    
    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
    
    public boolean hasAnyRole(Role... roles) {
        for (Role role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }
    
    public boolean hasAnyPermission(Permission... permissions) {
        for (Permission permission : permissions) {
            if (this.permissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasAllPermissions(Permission... permissions) {
        for (Permission permission : permissions) {
            if (!this.permissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean isTokenExpired() {
        return System.currentTimeMillis() > tokenExpiresAt;
    }
    
    public long getTokenRemainingTime() {
        return Math.max(0, tokenExpiresAt - System.currentTimeMillis());
    }
    
    @Override
    public String toString() {
        return "SecurityContext{" +
                "userId=" + userId +
                ", tenantId=" + tenantId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", roles=" + roles +
                ", permissions=" + permissions +
                ", sessionId='" + sessionId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", tokenIssuedAt=" + tokenIssuedAt +
                ", tokenExpiresAt=" + tokenExpiresAt +
                '}';
    }
}