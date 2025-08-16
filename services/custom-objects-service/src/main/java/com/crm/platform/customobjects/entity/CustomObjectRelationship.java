package com.crm.platform.customobjects.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Custom Object Relationship entity for managing relationships between custom objects
 */
@Entity
@Table(name = "custom_object_relationships",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"tenant_id", "from_object_id", "to_object_id", "relationship_name"})
       })
@EntityListeners(AuditingEntityListener.class)
public class CustomObjectRelationship {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_object_id", nullable = false)
    @JsonIgnore
    private CustomObject fromObject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_object_id", nullable = false)
    @JsonIgnore
    private CustomObject toObject;

    @NotBlank
    @Size(max = 100)
    @Column(name = "relationship_name", nullable = false, length = 100)
    private String relationshipName;

    @NotBlank
    @Size(max = 255)
    @Column(name = "relationship_label", nullable = false, length = 255)
    private String relationshipLabel;

    @NotBlank
    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 50)
    private RelationshipType relationshipType;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "cascade_delete", nullable = false)
    private Boolean cascadeDelete = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @NotNull
    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Enums
    public enum RelationshipType {
        ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY, LOOKUP, MASTER_DETAIL
    }

    // Constructors
    public CustomObjectRelationship() {}

    public CustomObjectRelationship(UUID tenantId, CustomObject fromObject, CustomObject toObject,
                                  String relationshipName, String relationshipLabel,
                                  RelationshipType relationshipType, UUID createdBy) {
        this.tenantId = tenantId;
        this.fromObject = fromObject;
        this.toObject = toObject;
        this.relationshipName = relationshipName;
        this.relationshipLabel = relationshipLabel;
        this.relationshipType = relationshipType;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public CustomObject getFromObject() {
        return fromObject;
    }

    public void setFromObject(CustomObject fromObject) {
        this.fromObject = fromObject;
    }

    public CustomObject getToObject() {
        return toObject;
    }

    public void setToObject(CustomObject toObject) {
        this.toObject = toObject;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public void setRelationshipName(String relationshipName) {
        this.relationshipName = relationshipName;
    }

    public String getRelationshipLabel() {
        return relationshipLabel;
    }

    public void setRelationshipLabel(String relationshipLabel) {
        this.relationshipLabel = relationshipLabel;
    }

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public Boolean getCascadeDelete() {
        return cascadeDelete;
    }

    public void setCascadeDelete(Boolean cascadeDelete) {
        this.cascadeDelete = cascadeDelete;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomObjectRelationship)) return false;
        CustomObjectRelationship that = (CustomObjectRelationship) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "CustomObjectRelationship{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", relationshipName='" + relationshipName + '\'' +
                ", relationshipLabel='" + relationshipLabel + '\'' +
                ", relationshipType=" + relationshipType +
                '}';
    }
}