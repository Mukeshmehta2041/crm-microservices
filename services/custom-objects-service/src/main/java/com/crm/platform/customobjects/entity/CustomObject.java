package com.crm.platform.customobjects.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Custom Object entity representing user-defined business objects
 */
@Entity
@Table(name = "custom_objects", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"tenant_id", "name"}),
           @UniqueConstraint(columnNames = {"tenant_id", "api_name"})
       })
@EntityListeners(AuditingEntityListener.class)
public class CustomObject {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank
    @Size(max = 255)
    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @NotBlank
    @Size(max = 255)
    @Column(name = "plural_label", nullable = false, length = 255)
    private String pluralLabel;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[a-z][a-z0-9_]*[a-z0-9]$", message = "API name must be lowercase with underscores")
    @Column(name = "api_name", nullable = false, length = 100)
    private String apiName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "allow_reports", nullable = false)
    private Boolean allowReports = true;

    @Column(name = "allow_activities", nullable = false)
    private Boolean allowActivities = true;

    @Size(max = 100)
    @Column(name = "record_name_field", length = 100)
    private String recordNameField;

    @Size(max = 50)
    @Column(name = "icon", length = 50)
    private String icon;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color")
    @Column(name = "color", length = 7)
    private String color;

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

    @OneToMany(mappedBy = "customObject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<CustomField> customFields = new ArrayList<>();

    @OneToMany(mappedBy = "customObject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<CustomObjectRecord> records = new ArrayList<>();

    // Constructors
    public CustomObject() {}

    public CustomObject(UUID tenantId, String name, String label, String pluralLabel, 
                       String apiName, UUID createdBy) {
        this.tenantId = tenantId;
        this.name = name;
        this.label = label;
        this.pluralLabel = pluralLabel;
        this.apiName = apiName;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPluralLabel() {
        return pluralLabel;
    }

    public void setPluralLabel(String pluralLabel) {
        this.pluralLabel = pluralLabel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getAllowReports() {
        return allowReports;
    }

    public void setAllowReports(Boolean allowReports) {
        this.allowReports = allowReports;
    }

    public Boolean getAllowActivities() {
        return allowActivities;
    }

    public void setAllowActivities(Boolean allowActivities) {
        this.allowActivities = allowActivities;
    }

    public String getRecordNameField() {
        return recordNameField;
    }

    public void setRecordNameField(String recordNameField) {
        this.recordNameField = recordNameField;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
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

    public List<CustomField> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(List<CustomField> customFields) {
        this.customFields = customFields;
    }

    public List<CustomObjectRecord> getRecords() {
        return records;
    }

    public void setRecords(List<CustomObjectRecord> records) {
        this.records = records;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomObject)) return false;
        CustomObject that = (CustomObject) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "CustomObject{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", name='" + name + '\'' +
                ", label='" + label + '\'' +
                ", apiName='" + apiName + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}