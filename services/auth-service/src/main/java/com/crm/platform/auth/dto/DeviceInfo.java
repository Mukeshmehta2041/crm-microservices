package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class DeviceInfo {

    @JsonProperty("device_type")
    private String deviceType;

    @JsonProperty("browser_info")
    private BrowserInfo browserInfo;

    @JsonProperty("os_info")
    private OSInfo osInfo;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("user_agent")
    private String userAgent;

    private String fingerprint;

    @JsonProperty("location_info")
    private LocationInfo locationInfo;

    private LocalDateTime timestamp;

    // Constructors
    public DeviceInfo() {}

    public DeviceInfo(String deviceType, BrowserInfo browserInfo, OSInfo osInfo, 
                     String ipAddress, String userAgent, String fingerprint, 
                     LocationInfo locationInfo) {
        this.deviceType = deviceType;
        this.browserInfo = browserInfo;
        this.osInfo = osInfo;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.fingerprint = fingerprint;
        this.locationInfo = locationInfo;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public BrowserInfo getBrowserInfo() { return browserInfo; }
    public void setBrowserInfo(BrowserInfo browserInfo) { this.browserInfo = browserInfo; }

    public OSInfo getOsInfo() { return osInfo; }
    public void setOsInfo(OSInfo osInfo) { this.osInfo = osInfo; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public LocationInfo getLocationInfo() { return locationInfo; }
    public void setLocationInfo(LocationInfo locationInfo) { this.locationInfo = locationInfo; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    // Nested classes for browser and OS information
    public static class BrowserInfo {
        private String name;
        private String version;

        public BrowserInfo() {}

        public BrowserInfo(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        @Override
        public String toString() {
            return name + " " + version;
        }
    }

    public static class OSInfo {
        private String name;
        private String version;

        public OSInfo() {}

        public OSInfo(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        @Override
        public String toString() {
            return name + " " + version;
        }
    }
}