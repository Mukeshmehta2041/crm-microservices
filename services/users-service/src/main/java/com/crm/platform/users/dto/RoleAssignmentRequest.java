package com.crm.platform.users.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for role assignment
 */
public class RoleAssignmentRequest {
    
    @NotNull(message = "User ID is required")
    @JsonProperty("user_id")
    private UUID userId;
    
    @NotNull(message = "Role ID is required")
    @JsonProperty("role_id")
    private UUID roleId;
    
    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;
    
    @JsonProperty("assignment_reason")
    private String assignmentReason;

    public RoleAssignmentRequest() {}

    public RoleAssignmentRequest(UUID userId, UUID roleId, LocalDateTime expiresAt, String assignmentReason) {
        this.userId = userId;
        this.roleId = roleId;
        this.expiresAt = expiresAt;
        this.assignmentReason = assignmentReason;
    }

    // Getters and Setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getAssignmentReason() { return assignmentReason; }
    public void setAssignmentReason(String assignmentReason) { this.assignmentReason = assignmentReason; }
}