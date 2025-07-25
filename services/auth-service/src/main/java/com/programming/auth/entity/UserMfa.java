package com.programming.auth.entity;

import com.programming.common.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "user_mfa", 
       uniqueConstraints = {
           @UniqueConstraint(name = "user_mfa_user_type_unique", columnNames = {"user_id", "mfaType"})
       },
       indexes = {
           @Index(name = "idx_user_mfa_user_id", columnList = "user_id"),
           @Index(name = "idx_user_mfa_type", columnList = "mfaType"),
           @Index(name = "idx_user_mfa_enabled", columnList = "user_id, isEnabled"),
           @Index(name = "idx_user_mfa_tenant_id", columnList = "tenant_id")
       })
public class UserMfa extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_type", nullable = false, length = 20)
    private MfaType mfaType;

    @Size(max = 255)
    @Column(name = "secret_key")
    private String secretKey;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "backup_codes", columnDefinition = "text[]")
    private List<String> backupCodes;

    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Size(max = 50)
    @Column(name = "phone_number")
    private String phoneNumber;

    @Email
    @Size(max = 255)
    @Column(name = "email")
    private String email;

    @NotNull
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = false;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "trusted_device_ids", columnDefinition = "TEXT")
    private String trustedDeviceIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mfa_metadata", columnDefinition = "jsonb")
    private Map<String, Object> mfaMetadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false, updatable = false, insertable = false)
    private User createdByUser;

    public enum MfaType {
        TOTP, SMS, EMAIL, BACKUP_CODES
    }

    // Constructors
    public UserMfa() {
        super();
    }

    public UserMfa(User user, MfaType mfaType) {
        super(user.getTenantId());
        this.user = user;
        this.tenant = user.getTenant();
        this.mfaType = mfaType;
    }

    // Business methods
    public boolean isVerified() {
        return verifiedAt != null;
    }

    public void markAsVerified() {
        this.verifiedAt = Instant.now();
        this.isEnabled = true;
    }

    public void markAsUsed() {
        this.lastUsedAt = Instant.now();
    }

    public boolean isActive() {
        return isEnabled && isVerified();
    }

    // Getters and Setters
    public MfaType getMfaType() {
        return mfaType;
    }

    public void setMfaType(MfaType mfaType) {
        this.mfaType = mfaType;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public List<String> getBackupCodes() {
        return backupCodes;
    }

    public void setBackupCodes(List<String> backupCodes) {
        this.backupCodes = backupCodes;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public String getTrustedDeviceIds() {
        return trustedDeviceIds;
    }

    public void setTrustedDeviceIds(String trustedDeviceIds) {
        this.trustedDeviceIds = trustedDeviceIds;
    }

    public Map<String, Object> getMfaMetadata() {
        return mfaMetadata;
    }

    public void setMfaMetadata(Map<String, Object> mfaMetadata) {
        this.mfaMetadata = mfaMetadata;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.tenant = user.getTenant();
            setTenantId(user.getTenantId());
        }
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }
}