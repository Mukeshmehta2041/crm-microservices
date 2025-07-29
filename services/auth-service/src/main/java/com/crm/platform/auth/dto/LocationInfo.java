package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class LocationInfo {

    private String country;
    private String region;
    private String city;
    private String timezone;
    private double latitude;
    private double longitude;

    @JsonProperty("is_local")
    private boolean isLocal;

    @JsonProperty("accuracy_radius")
    private Integer accuracyRadius;

    // Constructors
    public LocationInfo() {}

    public LocationInfo(String country, String region, String city, String timezone, 
                       double latitude, double longitude) {
        this.country = country;
        this.region = region;
        this.city = city;
        this.timezone = timezone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isLocal = isLocalLocation(country);
    }

    // Getters and Setters
    public String getCountry() { return country; }
    public void setCountry(String country) { 
        this.country = country;
        this.isLocal = isLocalLocation(country);
    }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public boolean isLocal() { return isLocal; }
    public void setLocal(boolean local) { isLocal = local; }

    public Integer getAccuracyRadius() { return accuracyRadius; }
    public void setAccuracyRadius(Integer accuracyRadius) { this.accuracyRadius = accuracyRadius; }

    // Helper methods
    private boolean isLocalLocation(String country) {
        return "Local".equals(country) || "Unknown".equals(country);
    }

    public String getDisplayName() {
        if (isLocal) {
            return "Local";
        }
        
        StringBuilder display = new StringBuilder();
        if (city != null && !city.equals("Unknown")) {
            display.append(city);
        }
        if (region != null && !region.equals("Unknown")) {
            if (display.length() > 0) display.append(", ");
            display.append(region);
        }
        if (country != null && !country.equals("Unknown")) {
            if (display.length() > 0) display.append(", ");
            display.append(country);
        }
        
        return display.length() > 0 ? display.toString() : "Unknown Location";
    }

    /**
     * Calculate distance to another location in kilometers
     */
    public double distanceTo(LocationInfo other) {
        if (this.isLocal || other.isLocal) {
            return 0.0;
        }

        final double R = 6371; // Earth's radius in kilometers
        
        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double deltaLatRad = Math.toRadians(other.latitude - this.latitude);
        double deltaLonRad = Math.toRadians(other.longitude - this.longitude);

        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                  Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                  Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        LocationInfo that = (LocationInfo) obj;
        return Double.compare(that.latitude, latitude) == 0 &&
               Double.compare(that.longitude, longitude) == 0 &&
               Objects.equals(country, that.country) &&
               Objects.equals(region, that.region) &&
               Objects.equals(city, that.city);
    }

    @Override
    public int hashCode() {
        return Objects.hash(country, region, city, latitude, longitude);
    }
}