package com.crm.platform.users.dto;

import com.crm.platform.users.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for bulk user operations
 */
public class BulkUserOperationRequest {
    
    @NotEmpty(message = "User IDs list cannot be empty")
    @JsonProperty("user_ids")
    private List<UUID> userIds;
    
    @NotNull(message = "Operation type is required")
    @JsonProperty("operation")
    private BulkOperation operation;
    
    @JsonProperty("status")
    private User.UserStatus status;
    
    @JsonProperty("department")
    private String department;
    
    @JsonProperty("manager_id")
    private UUID managerId;
    
    @JsonProperty("team_id")
    private UUID teamId;

    public BulkUserOperationRequest() {}

    // Getters and Setters
    public List<UUID> getUserIds() { return userIds; }
    public void setUserIds(List<UUID> userIds) { this.userIds = userIds; }

    public BulkOperation getOperation() { return operation; }
    public void setOperation(BulkOperation operation) { this.operation = operation; }

    public User.UserStatus getStatus() { return status; }
    public void setStatus(User.UserStatus status) { this.status = status; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public UUID getManagerId() { return managerId; }
    public void setManagerId(UUID managerId) { this.managerId = managerId; }

    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }

    public enum BulkOperation {
        UPDATE_STATUS,
        UPDATE_DEPARTMENT,
        UPDATE_MANAGER,
        UPDATE_TEAM,
        ACTIVATE,
        DEACTIVATE,
        SUSPEND
    }
}