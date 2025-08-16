package com.crm.platform.users.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity representing a permission with resource-action mapping
 */
@Entity
@Table(name = "permissions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"resource", "action"}),
       indexes = {
    @Index(name = "idx_permissions_resource", columnList = "resource"),
    @Index(name = "idx_permissions_action", columnList = "action"),
    @Index(name = "idx_permissions_category", columnList = "category"),
    @Index(name = "idx_permissions_resource_action", columnList = "resource, action")
})
@EntityListeners(AuditingEntityListener.class)
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(name = "resource", nullable = false, length = 100)
    private String resource; // e.g., "users", "contacts", "deals"

    @NotBlank
    @Size(min = 2, max = 50)
    @Column(name = "action", nullable = false, length = 50)
    private String action; // e.g., "create", "read", "update", "delete"

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @Size(max = 50)
    @Column(name = "category", length = 50)
    private String category; // e.g., "user_management", "sales", "marketing"

    @Column(name = "is_system_permission")
    private Boolean isSystemPermission = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "priority")
    private Integer priority = 0;

    // Resource constraints (JSON format for flexible constraints)
    @Column(name = "constraints", columnDefinition = "TEXT")
    private String constraints; // JSON string for additional constraints

    // Many-to-many relationship with roles (mapped by roles)
    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    private Set<Role> roles = new HashSet<>();

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
    public Permission() {}

    public Permission(String resource, String action, String description, String category) {
        this.resource = resource;
        this.action = action;
        this.description = description;
        this.category = category;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Boolean getIsSystemPermission() { return isSystemPermission; }
    public void setIsSystemPermission(Boolean isSystemPermission) { this.isSystemPermission = isSystemPermission; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getConstraints() { return constraints; }
    public void setConstraints(String constraints) { this.constraints = constraints; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public String getPermissionKey() {
        return resource + ":" + action;
    }

    public boolean matches(String resource, String action) {
        return this.resource.equals(resource) && this.action.equals(action);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Permission that = (Permission) obj;
        return resource.equals(that.resource) && action.equals(that.action);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(resource, action);
    }

    @Override
    public String toString() {
        return "Permission{" +
                "resource='" + resource + '\'' +
                ", action='" + action + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    // Common permission actions
    public static final String ACTION_CREATE = "create";
    public static final String ACTION_READ = "read";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_LIST = "list";
    public static final String ACTION_EXPORT = "export";
    public static final String ACTION_IMPORT = "import";
    public static final String ACTION_MANAGE = "manage";
    public static final String ACTION_ADMIN = "admin";

    // Common resources
    public static final String RESOURCE_USERS = "users";
    public static final String RESOURCE_CONTACTS = "contacts";
    public static final String RESOURCE_DEALS = "deals";
    public static final String RESOURCE_LEADS = "leads";
    public static final String RESOURCE_ACCOUNTS = "accounts";
    public static final String RESOURCE_ACTIVITIES = "activities";
    public static final String RESOURCE_REPORTS = "reports";
    public static final String RESOURCE_SETTINGS = "settings";
    public static final String RESOURCE_ROLES = "roles";
    public static final String RESOURCE_PERMISSIONS = "permissions";
}