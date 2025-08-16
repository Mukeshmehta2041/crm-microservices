package com.crm.platform.users.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity representing a role with hierarchical support and tenant isolation
 */
@Entity
@Table(name = "roles", indexes = {
    @Index(name = "idx_roles_name", columnList = "name"),
    @Index(name = "idx_roles_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_roles_parent_id", columnList = "parent_role_id"),
    @Index(name = "idx_roles_type", columnList = "role_type")
})
@EntityListeners(AuditingEntityListener.class)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 20)
    private RoleType roleType = RoleType.CUSTOM;

    @Column(name = "parent_role_id")
    private UUID parentRoleId;

    @Column(name = "hierarchy_level")
    private Integer hierarchyLevel = 0;

    @Column(name = "hierarchy_path", length = 1000)
    private String hierarchyPath; // Materialized path for efficient queries

    @Column(name = "is_system_role")
    private Boolean isSystemRole = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "priority")
    private Integer priority = 0; // Higher number = higher priority

    @Column(name = "color", length = 7)
    private String color; // Hex color for UI

    @Column(name = "icon", length = 50)
    private String icon; // Icon identifier for UI

    // Many-to-many relationship with permissions
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id"),
        indexes = {
            @Index(name = "idx_role_permissions_role", columnList = "role_id"),
            @Index(name = "idx_role_permissions_permission", columnList = "permission_id")
        }
    )
    private Set<Permission> permissions = new HashSet<>();

    // Audit fields
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Role() {}

    public Role(String name, String description, UUID tenantId, RoleType roleType) {
        this.name = name;
        this.description = description;
        this.tenantId = tenantId;
        this.roleType = roleType;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public RoleType getRoleType() { return roleType; }
    public void setRoleType(RoleType roleType) { this.roleType = roleType; }

    public UUID getParentRoleId() { return parentRoleId; }
    public void setParentRoleId(UUID parentRoleId) { this.parentRoleId = parentRoleId; }

    public Integer getHierarchyLevel() { return hierarchyLevel; }
    public void setHierarchyLevel(Integer hierarchyLevel) { this.hierarchyLevel = hierarchyLevel; }

    public String getHierarchyPath() { return hierarchyPath; }
    public void setHierarchyPath(String hierarchyPath) { this.hierarchyPath = hierarchyPath; }

    public Boolean getIsSystemRole() { return isSystemRole; }
    public void setIsSystemRole(Boolean isSystemRole) { this.isSystemRole = isSystemRole; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Set<Permission> getPermissions() { return permissions; }
    public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission) {
        return this.permissions.contains(permission);
    }

    public boolean hasPermission(String resource, String action) {
        return this.permissions.stream()
                .anyMatch(p -> p.getResource().equals(resource) && p.getAction().equals(action));
    }

    public boolean isChildOf(Role parentRole) {
        return this.parentRoleId != null && this.parentRoleId.equals(parentRole.getId());
    }

    public boolean isDescendantOf(Role ancestorRole) {
        return this.hierarchyPath != null && 
               this.hierarchyPath.contains("/" + ancestorRole.getId() + "/");
    }

    public void updateHierarchyPath() {
        if (this.parentRoleId == null) {
            this.hierarchyPath = "/" + this.id + "/";
            this.hierarchyLevel = 0;
        } else {
            // This should be set by the service when parent hierarchy is known
        }
    }

    public enum RoleType {
        SYSTEM,     // Built-in system roles
        TENANT,     // Tenant-wide roles
        CUSTOM,     // Custom roles created by users
        INHERITED   // Roles inherited from parent roles
    }
}