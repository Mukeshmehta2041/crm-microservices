package com.crm.platform.tenant.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_configurations", 
       indexes = {
           @Index(name = "idx_tenant_config_tenant_id", columnList = "tenant_id"),
           @Index(name = "idx_tenant_config_key", columnList = "config_key"),
           @Index(name = "idx_tenant_config_category", columnList = "category")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_tenant_config_key", columnNames = {"tenant_id", "config_key"})
       })
@EntityListeners(AuditingEntityListener.class)
public class TenantConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotBlank
    @Size(min = 1, max = 255)
    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ConfigType configType = ConfigType.STRING;

    @Size(max = 100)
    @Column(length = 100)
    private String category;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @Column(name = "is_encrypted")
    private Boolean isEncrypted = false;

    @Column(name = "is_system")
    private Boolean isSystem = false;

    @Column(name = "is_editable")
    private Boolean isEditable = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public TenantConfiguration() {}

    public TenantConfiguration(UUID tenantId, String configKey, String configValue) {
        this.tenantId = tenantId;
        this.configKey = configKey;
        this.configValue = configValue;
    }

    public TenantConfiguration(UUID tenantId, String configKey, String configValue, ConfigType configType) {
        this.tenantId = tenantId;
        this.configKey = configKey;
        this.configValue = configValue;
        this.configType = configType;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }

    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }

    public ConfigType getConfigType() { return configType; }
    public void setConfigType(ConfigType configType) { this.configType = configType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsEncrypted() { return isEncrypted; }
    public void setIsEncrypted(Boolean isEncrypted) { this.isEncrypted = isEncrypted; }

    public Boolean getIsSystem() { return isSystem; }
    public void setIsSystem(Boolean isSystem) { this.isSystem = isSystem; }

    public Boolean getIsEditable() { return isEditable; }
    public void setIsEditable(Boolean isEditable) { this.isEditable = isEditable; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public Object getTypedValue() {
        if (configValue == null) {
            return null;
        }

        switch (configType) {
            case BOOLEAN:
                return Boolean.parseBoolean(configValue);
            case INTEGER:
                return Integer.parseInt(configValue);
            case LONG:
                return Long.parseLong(configValue);
            case DOUBLE:
                return Double.parseDouble(configValue);
            case JSON:
                return configValue; // Return as string, let the caller parse JSON
            case STRING:
            default:
                return configValue;
        }
    }

    public void setTypedValue(Object value) {
        if (value == null) {
            this.configValue = null;
            return;
        }

        this.configValue = value.toString();
    }

    public enum ConfigType {
        STRING, BOOLEAN, INTEGER, LONG, DOUBLE, JSON
    }

    // Common configuration keys as constants
    public static final String KEY_TIMEZONE = "timezone";
    public static final String KEY_DATE_FORMAT = "date_format";
    public static final String KEY_TIME_FORMAT = "time_format";
    public static final String KEY_CURRENCY = "currency";
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_THEME = "theme";
    public static final String KEY_LOGO_URL = "logo_url";
    public static final String KEY_PRIMARY_COLOR = "primary_color";
    public static final String KEY_SECONDARY_COLOR = "secondary_color";
    public static final String KEY_EMAIL_NOTIFICATIONS = "email_notifications";
    public static final String KEY_SMS_NOTIFICATIONS = "sms_notifications";
    public static final String KEY_MAX_FILE_SIZE_MB = "max_file_size_mb";
    public static final String KEY_ALLOWED_FILE_TYPES = "allowed_file_types";
    public static final String KEY_SESSION_TIMEOUT_MINUTES = "session_timeout_minutes";
    public static final String KEY_PASSWORD_POLICY = "password_policy";
    public static final String KEY_TWO_FACTOR_REQUIRED = "two_factor_required";
    public static final String KEY_API_RATE_LIMIT = "api_rate_limit";
}