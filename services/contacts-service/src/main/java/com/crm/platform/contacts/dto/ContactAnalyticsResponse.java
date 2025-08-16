package com.crm.platform.contacts.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for contact analytics
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactAnalyticsResponse {
    
    @JsonProperty("totalContacts")
    private Long totalContacts;
    
    @JsonProperty("newContactsThisPeriod")
    private Long newContactsThisPeriod;
    
    @JsonProperty("growthRate")
    private Double growthRate;
    
    @JsonProperty("averageLeadScore")
    private Double averageLeadScore;
    
    @JsonProperty("contactsByStatus")
    private Map<String, Long> contactsByStatus;
    
    @JsonProperty("contactsBySource")
    private Map<String, Long> contactsBySource;
    
    @JsonProperty("contactsByCompany")
    private Map<String, Long> contactsByCompany;
    
    @JsonProperty("contactsByIndustry")
    private Map<String, Long> contactsByIndustry;
    
    @JsonProperty("contactsByOwner")
    private Map<String, Long> contactsByOwner;
    
    @JsonProperty("timeSeriesData")
    private List<TimeSeriesDataPoint> timeSeriesData;
    
    @JsonProperty("topCompanies")
    private List<CompanyStats> topCompanies;
    
    @JsonProperty("conversionMetrics")
    private ConversionMetrics conversionMetrics;
    
    @JsonProperty("engagementMetrics")
    private EngagementMetrics engagementMetrics;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TimeSeriesDataPoint {
        @JsonProperty("date")
        private LocalDate date;
        
        @JsonProperty("newContacts")
        private Long newContacts;
        
        @JsonProperty("totalContacts")
        private Long totalContacts;
        
        @JsonProperty("averageLeadScore")
        private Double averageLeadScore;
        
        // Constructors
        public TimeSeriesDataPoint() {}
        
        public TimeSeriesDataPoint(LocalDate date, Long newContacts, Long totalContacts) {
            this.date = date;
            this.newContacts = newContacts;
            this.totalContacts = totalContacts;
        }
        
        // Getters and setters
        public LocalDate getDate() {
            return date;
        }
        
        public void setDate(LocalDate date) {
            this.date = date;
        }
        
        public Long getNewContacts() {
            return newContacts;
        }
        
        public void setNewContacts(Long newContacts) {
            this.newContacts = newContacts;
        }
        
        public Long getTotalContacts() {
            return totalContacts;
        }
        
        public void setTotalContacts(Long totalContacts) {
            this.totalContacts = totalContacts;
        }
        
        public Double getAverageLeadScore() {
            return averageLeadScore;
        }
        
        public void setAverageLeadScore(Double averageLeadScore) {
            this.averageLeadScore = averageLeadScore;
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompanyStats {
        @JsonProperty("companyName")
        private String companyName;
        
        @JsonProperty("contactCount")
        private Long contactCount;
        
        @JsonProperty("averageLeadScore")
        private Double averageLeadScore;
        
        @JsonProperty("conversionRate")
        private Double conversionRate;
        
        // Constructors
        public CompanyStats() {}
        
        public CompanyStats(String companyName, Long contactCount) {
            this.companyName = companyName;
            this.contactCount = contactCount;
        }
        
        // Getters and setters
        public String getCompanyName() {
            return companyName;
        }
        
        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }
        
        public Long getContactCount() {
            return contactCount;
        }
        
        public void setContactCount(Long contactCount) {
            this.contactCount = contactCount;
        }
        
        public Double getAverageLeadScore() {
            return averageLeadScore;
        }
        
        public void setAverageLeadScore(Double averageLeadScore) {
            this.averageLeadScore = averageLeadScore;
        }
        
        public Double getConversionRate() {
            return conversionRate;
        }
        
        public void setConversionRate(Double conversionRate) {
            this.conversionRate = conversionRate;
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConversionMetrics {
        @JsonProperty("leadToContactRate")
        private Double leadToContactRate;
        
        @JsonProperty("contactToOpportunityRate")
        private Double contactToOpportunityRate;
        
        @JsonProperty("opportunityToCustomerRate")
        private Double opportunityToCustomerRate;
        
        @JsonProperty("averageConversionTime")
        private Double averageConversionTime;
        
        // Getters and setters
        public Double getLeadToContactRate() {
            return leadToContactRate;
        }
        
        public void setLeadToContactRate(Double leadToContactRate) {
            this.leadToContactRate = leadToContactRate;
        }
        
        public Double getContactToOpportunityRate() {
            return contactToOpportunityRate;
        }
        
        public void setContactToOpportunityRate(Double contactToOpportunityRate) {
            this.contactToOpportunityRate = contactToOpportunityRate;
        }
        
        public Double getOpportunityToCustomerRate() {
            return opportunityToCustomerRate;
        }
        
        public void setOpportunityToCustomerRate(Double opportunityToCustomerRate) {
            this.opportunityToCustomerRate = opportunityToCustomerRate;
        }
        
        public Double getAverageConversionTime() {
            return averageConversionTime;
        }
        
        public void setAverageConversionTime(Double averageConversionTime) {
            this.averageConversionTime = averageConversionTime;
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EngagementMetrics {
        @JsonProperty("averageActivitiesPerContact")
        private Double averageActivitiesPerContact;
        
        @JsonProperty("emailEngagementRate")
        private Double emailEngagementRate;
        
        @JsonProperty("callConnectRate")
        private Double callConnectRate;
        
        @JsonProperty("meetingScheduleRate")
        private Double meetingScheduleRate;
        
        // Getters and setters
        public Double getAverageActivitiesPerContact() {
            return averageActivitiesPerContact;
        }
        
        public void setAverageActivitiesPerContact(Double averageActivitiesPerContact) {
            this.averageActivitiesPerContact = averageActivitiesPerContact;
        }
        
        public Double getEmailEngagementRate() {
            return emailEngagementRate;
        }
        
        public void setEmailEngagementRate(Double emailEngagementRate) {
            this.emailEngagementRate = emailEngagementRate;
        }
        
        public Double getCallConnectRate() {
            return callConnectRate;
        }
        
        public void setCallConnectRate(Double callConnectRate) {
            this.callConnectRate = callConnectRate;
        }
        
        public Double getMeetingScheduleRate() {
            return meetingScheduleRate;
        }
        
        public void setMeetingScheduleRate(Double meetingScheduleRate) {
            this.meetingScheduleRate = meetingScheduleRate;
        }
    }
    
    // Getters and setters
    public Long getTotalContacts() {
        return totalContacts;
    }
    
    public void setTotalContacts(Long totalContacts) {
        this.totalContacts = totalContacts;
    }
    
    public Long getNewContactsThisPeriod() {
        return newContactsThisPeriod;
    }
    
    public void setNewContactsThisPeriod(Long newContactsThisPeriod) {
        this.newContactsThisPeriod = newContactsThisPeriod;
    }
    
    public Double getGrowthRate() {
        return growthRate;
    }
    
    public void setGrowthRate(Double growthRate) {
        this.growthRate = growthRate;
    }
    
    public Double getAverageLeadScore() {
        return averageLeadScore;
    }
    
    public void setAverageLeadScore(Double averageLeadScore) {
        this.averageLeadScore = averageLeadScore;
    }
    
    public Map<String, Long> getContactsByStatus() {
        return contactsByStatus;
    }
    
    public void setContactsByStatus(Map<String, Long> contactsByStatus) {
        this.contactsByStatus = contactsByStatus;
    }
    
    public Map<String, Long> getContactsBySource() {
        return contactsBySource;
    }
    
    public void setContactsBySource(Map<String, Long> contactsBySource) {
        this.contactsBySource = contactsBySource;
    }
    
    public Map<String, Long> getContactsByCompany() {
        return contactsByCompany;
    }
    
    public void setContactsByCompany(Map<String, Long> contactsByCompany) {
        this.contactsByCompany = contactsByCompany;
    }
    
    public Map<String, Long> getContactsByIndustry() {
        return contactsByIndustry;
    }
    
    public void setContactsByIndustry(Map<String, Long> contactsByIndustry) {
        this.contactsByIndustry = contactsByIndustry;
    }
    
    public Map<String, Long> getContactsByOwner() {
        return contactsByOwner;
    }
    
    public void setContactsByOwner(Map<String, Long> contactsByOwner) {
        this.contactsByOwner = contactsByOwner;
    }
    
    public List<TimeSeriesDataPoint> getTimeSeriesData() {
        return timeSeriesData;
    }
    
    public void setTimeSeriesData(List<TimeSeriesDataPoint> timeSeriesData) {
        this.timeSeriesData = timeSeriesData;
    }
    
    public List<CompanyStats> getTopCompanies() {
        return topCompanies;
    }
    
    public void setTopCompanies(List<CompanyStats> topCompanies) {
        this.topCompanies = topCompanies;
    }
    
    public ConversionMetrics getConversionMetrics() {
        return conversionMetrics;
    }
    
    public void setConversionMetrics(ConversionMetrics conversionMetrics) {
        this.conversionMetrics = conversionMetrics;
    }
    
    public EngagementMetrics getEngagementMetrics() {
        return engagementMetrics;
    }
    
    public void setEngagementMetrics(EngagementMetrics engagementMetrics) {
        this.engagementMetrics = engagementMetrics;
    }
}