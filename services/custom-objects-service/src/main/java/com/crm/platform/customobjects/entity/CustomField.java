package com.crm.platform.customobjects.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Custom Field entity representing dynamic fields for custom objects
 */
@Entity
@Table(name = "custom_fields",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"tenant_id", "object_type", "field_name"}),
           @UniqueConstraint(columnNames = {"tenant_id", "custom_object_id", "field_name"})
       })
@EntityListeners(AuditingEntityListener.class)
@TypeDef(name = "jsonb", typeClass = JsonType.class)
public class CustomField {

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
    @Column(name = "object_type", nullable = false, length = 100)
    private String objectType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_object_id")
    @JsonIgnore
    private CustomObject customObject;

    @NotBlank
    @Size(max = 100)
    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @NotBlank
    @Size(max = 255)
    @Column(name = "field_label", nullable = false, length = 255)
    private String fieldLabel;

    @NotBlank
    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 50)
    private FieldType fieldType;

    @NotBlank
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 50)
    private DataType dataType;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "is_unique", nullable = false)
    private Boolean isUnique = false;

    @Column(name = "is_indexed", nullable = false)
    private Boolean isIndexed = false;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(name = "help_text", columnDefinition = "TEXT")
    private String helpText;

    @Column(name = "field_order", nullable = false)
    private Integer fieldOrder = 0;

    @Type(type = "jsonb")
    @Column(name = "validation_rules", columnDefinition = "jsonb")
    private Map<String, Object> validationRules;

    @Type(type = "jsonb")
    @Column(name = "display_options", columnDefinition = "jsonb")
    private Map<String, Object> displayOptions;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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

    @OneToMany(mappedBy = "customField", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<CustomFieldOption> fieldOptions = new ArrayList<>();

    // Enums
    public enum FieldType {
        TEXT, TEXTAREA, NUMBER, DECIMAL, CURRENCY, PERCENT,
        DATE, DATETIME, BOOLEAN, PICKLIST, MULTIPICKLIST,
        EMAIL, PHONE, URL, LOOKUP, MASTER_DETAIL
    }

    public enum DataType {
        VARCHAR, TEXT, INTEGER, DECIMAL, BOOLEAN, DATE, 
        TIMESTAMP, JSONB, UUID
    }

    // Constructors
    public CustomField() {}

    public CustomField(UUID tenantId, String objectType, String fieldName, String fieldLabel,
                      FieldType fieldType, DataType dataType, UUID createdBy) {
        this.tenantId = tenantId;
        this.objectType = objectType;
        this.fieldName = fieldName;
        this.fieldLabel = fieldLabel;
        this.fieldType = fieldType;
        this.dataType = dataType;
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

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public CustomObject getCustomObject() {
        return customObject;
    }

    public void setCustomObject(CustomObject customObject) {
        this.customObject = customObject;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public Boolean getIsUnique() {
        return isUnique;
    }

    public void setIsUnique(Boolean isUnique) {
        this.isUnique = isUnique;
    }

    public Boolean getIsIndexed() {
        return isIndexed;
    }

    public void setIsIndexed(Boolean isIndexed) {
        this.isIndexed = isIndexed;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public Integer getFieldOrder() {
        return fieldOrder;
    }

    public void setFieldOrder(Integer fieldOrder) {
        this.fieldOrder = fieldOrder;
    }

    public Map<String, Object> getValidationRules() {
        return validationRules;
    }

    public void setValidationRules(Map<String, Object> validationRules) {
        this.validationRules = validationRules;
    }

    public Map<String, Object> getDisplayOptions() {
        return displayOptions;
    }

    public void setDisplayOptions(Map<String, Object> displayOptions) {
        this.displayOptions = displayOptions;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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

    public List<CustomFieldOption> getFieldOptions() {
        return fieldOptions;
    }

    public void setFieldOptions(List<CustomFieldOption> fieldOptions) {
        this.fieldOptions = fieldOptions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomField)) return false;
        CustomField that = (CustomField) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "CustomField{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", objectType='" + objectType + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", fieldLabel='" + fieldLabel + '\'' +
                ", fieldType=" + fieldType +
                ", dataType=" + dataType +
                '}';
    }
}