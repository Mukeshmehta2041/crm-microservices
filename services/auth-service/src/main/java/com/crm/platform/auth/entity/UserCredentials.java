package com.crm.platform.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_credentials", indexes = {
    @Index(name = "idx_user_credentials_username", columnList = "username"),
    @Index(name = "idx_user_credentials_email", columnList = "email"),
    @Index(name = "idx_user_credentials_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_user_credentials_user_id", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
public class UserCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId; // Reference to User in User Management Service

    @NotBlank
    @Size(min = 3, max = 100)
    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank
    @Size(min = 60, max = 60) // BCrypt hash length
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CredentialStatus status = CredentialStatus.ACTIVE;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "mfa_enabled")
    private Boolean mfaEnabled = false;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(name = "backup_codes", columnDefinition = "TEXT")
    private String backupCodes; // JSON array of backup codes

    @Column(name = "mfa_method", length = 20)
    @Enumerated(EnumType.STRING)
    private MfaMethod mfaMethod;

    @Column(name = "trusted_devices", columnDefinition = "TEXT")
    private String trustedDevices; // JSON array of trusted device fingerprints

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public UserCredentials() {}

    public UserCredentials(UUID userId, String username, String email, String passwordHash, UUID tenantId) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.tenantId = tenantId;
        this.passwordChangedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { 
        this.passwordHash = passwordHash;
        this.passwordChangedAt = LocalDateTime.now();
    }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public CredentialStatus getStatus() { return status; }
    public void setStatus(CredentialStatus status) { this.status = status; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }

    public Integer getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(Integer failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public LocalDateTime getAccountLockedUntil() { return accountLockedUntil; }
    public void setAccountLockedUntil(LocalDateTime accountLockedUntil) { this.accountLockedUntil = accountLockedUntil; }

    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    public Boolean getMfaEnabled() { return mfaEnabled; }
    public void setMfaEnabled(Boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }

    public String getMfaSecret() { return mfaSecret; }
    public void setMfaSecret(String mfaSecret) { this.mfaSecret = mfaSecret; }

    public String getBackupCodes() { return backupCodes; }
    public void setBackupCodes(String backupCodes) { this.backupCodes = backupCodes; }

    public MfaMethod getMfaMethod() { return mfaMethod; }
    public void setMfaMethod(MfaMethod mfaMethod) { this.mfaMethod = mfaMethod; }

    public String getTrustedDevices() { return trustedDevices; }
    public void setTrustedDevices(String trustedDevices) { this.trustedDevices = trustedDevices; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public boolean isAccountLocked() {
        return accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now());
    }

    public boolean isActive() {
        return status == CredentialStatus.ACTIVE && !isAccountLocked();
    }

    public enum CredentialStatus {
        ACTIVE, INACTIVE, SUSPENDED, DELETED
    }

    public enum MfaMethod {
        TOTP, SMS, EMAIL
    }
}