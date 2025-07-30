package com.crm.platform.users.dto;

import com.crm.platform.users.entity.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/**
 * Request DTO for creating a role
 */
public class CreateRoleRequest {
    
    @NotBlank(message = "Role name is required")
    @Size(min = 2, max = 100, message = "Role name must be between 2 and 100 characters")
    @JsonProperty("name")
    private String name;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @JsonProperty("description")
    private String description;
    
    @NotNull(message = "Tenant ID is required")
    @JsonProperty("tenant_id")
    private UUID tenantId;
    
    @JsonProperty("role_type")
    private Role.RoleType roleType = Role.RoleType.CUSTOM;
    
    @JsonProperty("parent_role_id")
    private UUID parentRoleId;
    
    @JsonProperty("priority")
    private Integer priority = 0;
    
    @JsonProperty("color")
    private String color;
    
    @JsonProperty("icon")
    private String icon;
    
    @JsonProperty("permission_ids")
    private Set<UUID> permissionIds;

    public CreateRoleRequest() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public Role.RoleType getRoleType() { return roleType; }
    public void setRoleType(Role.RoleType roleType) { this.roleType = roleType; }

    public UUID getParentRoleId() { return parentRoleId; }
    public void setParentRoleId(UUID parentRoleId) { this.parentRoleId = parentRoleId; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Set<UUID> getPermissionIds() { return permissionIds; }
    public void setPermissionIds(Set<UUID> permissionIds) { this.permissionIds = permissionIds; }
}