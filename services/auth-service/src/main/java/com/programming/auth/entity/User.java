package com.programming.auth.entity;

import com.programming.common.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", 
       uniqueConstraints = {
           @UniqueConstraint(name = "users_username_tenant_unique", columnNames = {"username", "tenant_id"}),
           @UniqueConstraint(name = "users_email_tenant_unique", columnNames = {"email", "tenant_id"})
       },
       indexes = {
           @Index(name = "idx_users_tenant_id", columnList = "tenant_id"),
           @Index(name = "idx_users_email", columnList = "email")
       })
public class User extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "username", nullable = false)
    private String username;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "email", nullable = false)
    private String email;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserSession> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserMfa> mfaSettings = new ArrayList<>();

    // Constructors
    public User() {
        super();
    }

    public User(String username, String email, String passwordHash, Tenant tenant) {
        super(tenant.getId());
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.tenant = tenant;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
        if (tenant != null) {
            setTenantId(tenant.getId());
        }
    }

    public List<UserSession> getSessions() {
        return sessions;
    }

    public void setSessions(List<UserSession> sessions) {
        this.sessions = sessions;
    }

    public List<UserMfa> getMfaSettings() {
        return mfaSettings;
    }

    public void setMfaSettings(List<UserMfa> mfaSettings) {
        this.mfaSettings = mfaSettings;
    }
}