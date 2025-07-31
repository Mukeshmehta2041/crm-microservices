package com.crm.platform.auth.dto;

/**
 * DTO for OAuth2 user information from external providers
 */
public class OAuth2UserInfo {

    private String id;
    private String email;
    private String name;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private String provider;
    private boolean emailVerified;

    // Constructors
    public OAuth2UserInfo() {}

    public OAuth2UserInfo(String id, String email, String name, String provider) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.provider = provider;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
}