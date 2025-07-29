package com.crm.platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class RegistrationRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;
    
    @JsonProperty("first_name")
    private String firstName;
    
    @JsonProperty("last_name")
    private String lastName;
    
    private String company;
    
    @JsonProperty("tenant_id")
    private UUID tenantId;
    
    @JsonProperty("invitation_token")
    private String invitationToken;
    
    @JsonProperty("terms_accepted")
    private boolean termsAccepted;
    
    @JsonProperty("privacy_policy_accepted")
    private boolean privacyPolicyAccepted;

    public RegistrationRequest() {}

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getInvitationToken() { return invitationToken; }
    public void setInvitationToken(String invitationToken) { this.invitationToken = invitationToken; }

    public boolean isTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(boolean termsAccepted) { this.termsAccepted = termsAccepted; }

    public boolean isPrivacyPolicyAccepted() { return privacyPolicyAccepted; }
    public void setPrivacyPolicyAccepted(boolean privacyPolicyAccepted) { this.privacyPolicyAccepted = privacyPolicyAccepted; }
}