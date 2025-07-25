package com.programming.auth.entity;

import com.programming.common.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tenants", indexes = {
    @Index(name = "idx_tenants_domain", columnList = "domain"),
    @Index(name = "idx_tenants_is_active", columnList = "isActive")
})
public class Tenant extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?$", 
             message = "Invalid domain format")
    @Column(name = "domain", nullable = false, unique = true)
    private String domain;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OAuthApplication> oauthApplications = new ArrayList<>();

    // Constructors
    public Tenant() {
        super();
    }

    public Tenant(String name, String domain) {
        super();
        this.name = name;
        this.domain = domain;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public List<OAuthApplication> getOauthApplications() {
        return oauthApplications;
    }

    public void setOauthApplications(List<OAuthApplication> oauthApplications) {
        this.oauthApplications = oauthApplications;
    }
}