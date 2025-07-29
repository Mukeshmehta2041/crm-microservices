package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class TrustedDeviceRequest {

    @NotBlank(message = "Device name is required")
    @JsonProperty("device_name")
    private String deviceName;

    @JsonProperty("device_type")
    private String deviceType;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("trust_duration_days")
    private Integer trustDurationDays;

    // Constructors
    public TrustedDeviceRequest() {}

    public TrustedDeviceRequest(String deviceName, String deviceType) {
        this.deviceName = deviceName;
        this.deviceType = deviceType;
    }

    // Getters and Setters
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public Integer getTrustDurationDays() { return trustDurationDays; }
    public void setTrustDurationDays(Integer trustDurationDays) { this.trustDurationDays = trustDurationDays; }
}