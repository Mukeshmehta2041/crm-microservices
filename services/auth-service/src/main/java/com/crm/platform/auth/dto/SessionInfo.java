package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public class SessionInfo {
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("user_id")
    private UUID userId;
    
    @JsonProperty("tenant_id")
    private UUID tenantId;
    
    @JsonProperty("device_type")
    private String deviceType;
    
    private String browser;
    
    @JsonProperty("operating_system")
    private String operatingSystem;
    
    @JsonProperty("ip_address")
    private String ipAddress;
    
    private String location;
    
    private String status;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("last_accessed_at")
    private LocalDateTime lastAccessedAt;
    
    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;
    
    @JsonProperty("is_current")
    private boolean current;
    
    private Set<String> permissions;

    public SessionInfo() {}

    public SessionInfo(String sessionId, UUID userId, UUID tenantId, String deviceType, String browser, 
                      String operatingSystem, String ipAddress, String location, String status,
                      LocalDateTime createdAt, LocalDateTime lastAccessedAt, LocalDateTime expiresAt, 
                      boolean current, Set<String> permissions) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.tenantId = tenantId;
        this.deviceType = deviceType;
        this.browser = browser;
        this.operatingSystem = operatingSystem;
        this.ipAddress = ipAddress;
        this.location = location;
        this.status = status;
        this.createdAt = createdAt;
        this.lastAccessedAt = lastAccessedAt;
        this.expiresAt = expiresAt;
        this.current = current;
        this.permissions = permissions;
    }

    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }

    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) { this.permissions = permissions; }
}