package com.crm.platform.users.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing user-role association with expiration support
 */
@Entity
@Table(name = "user_roles", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id"}),
       indexes = {
    @Index(name = "idx_user_roles_user", columnList = "user_id"),
    @Index(name = "idx_user_roles_role", columnList = "role_id"),
    @Index(name = "idx_user_roles_tenant", columnList = "tenant_id"),
    @Index(name = "idx_user_roles_expires", columnList = "expires_at"),
    @Index(name = "idx_user_roles_active", columnList = "is_active")
})
@EntityListeners(AuditingEntityListener.class)
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_inherited")
    private Boolean isInherited = false; // True if inherited from parent role

    @Column(name = "inherited_from")
    private UUID inheritedFrom; // Parent role ID if inherited

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", length = 20)
    private AssignmentType assignmentType = AssignmentType.DIRECT;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "assignment_reason", length = 500)
    private String assignmentReason;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // JPA relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private Role role;

    // Constructors
    public UserRole() {}

    public UserRole(UUID userId, UUID roleId, UUID tenantId, UUID assignedBy) {
        this.userId = userId;
        this.roleId = roleId;
        this.tenantId = tenantId;
        this.assignedBy = assignedBy;
    }

    public UserRole(UUID userId, UUID roleId, UUID tenantId, LocalDateTime expiresAt, UUID assignedBy) {
        this.userId = userId;
        this.roleId = roleId;
        this.tenantId = tenantId;
        this.expiresAt = expiresAt;
        this.assignedBy = assignedBy;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getIsInherited() { return isInherited; }
    public void setIsInherited(Boolean isInherited) { this.isInherited = isInherited; }

    public UUID getInheritedFrom() { return inheritedFrom; }
    public void setInheritedFrom(UUID inheritedFrom) { this.inheritedFrom = inheritedFrom; }

    public AssignmentType getAssignmentType() { return assignmentType; }
    public void setAssignmentType(AssignmentType assignmentType) { this.assignmentType = assignmentType; }

    public UUID getAssignedBy() { return assignedBy; }
    public void setAssignedBy(UUID assignedBy) { this.assignedBy = assignedBy; }

    public String getAssignmentReason() { return assignmentReason; }
    public void setAssignmentReason(String assignmentReason) { this.assignmentReason = assignmentReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    // Helper methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return Boolean.TRUE.equals(isActive) && !isExpired();
    }

    public void expire() {
        this.isActive = false;
        this.expiresAt = LocalDateTime.now();
    }

    public void extend(LocalDateTime newExpiryDate) {
        this.expiresAt = newExpiryDate;
        this.isActive = true;
    }

    public long getDaysUntilExpiry() {
        if (expiresAt == null) {
            return Long.MAX_VALUE; // Never expires
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expiresAt);
    }

    public boolean isExpiringSoon(int warningDays) {
        if (expiresAt == null) {
            return false;
        }
        return getDaysUntilExpiry() <= warningDays && getDaysUntilExpiry() > 0;
    }

    public enum AssignmentType {
        DIRECT,     // Directly assigned to user
        INHERITED,  // Inherited from parent role
        AUTOMATIC,  // Automatically assigned based on rules
        TEMPORARY   // Temporary assignment with expiration
    }
}