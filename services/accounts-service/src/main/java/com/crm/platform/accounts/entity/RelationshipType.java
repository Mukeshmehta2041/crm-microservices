package com.crm.platform.accounts.entity;

public enum RelationshipType {
    PARENT_CHILD("Parent-Child"),
    SUBSIDIARY("Subsidiary"),
    PARTNER("Partner"),
    VENDOR("Vendor"),
    CUSTOMER("Customer"),
    COMPETITOR("Competitor"),
    ALLIANCE("Alliance"),
    JOINT_VENTURE("Joint Venture"),
    ACQUISITION_TARGET("Acquisition Target"),
    MERGER("Merger"),
    SUPPLIER("Supplier"),
    DISTRIBUTOR("Distributor"),
    RESELLER("Reseller"),
    INTEGRATOR("Integrator"),
    CONSULTANT("Consultant"),
    REFERRAL_SOURCE("Referral Source"),
    STRATEGIC_PARTNER("Strategic Partner"),
    TECHNOLOGY_PARTNER("Technology Partner"),
    CHANNEL_PARTNER("Channel Partner"),
    OTHER("Other");

    private final String displayName;

    RelationshipType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Helper methods to determine relationship characteristics
    public boolean isHierarchical() {
        return this == PARENT_CHILD || this == SUBSIDIARY;
    }

    public boolean isBusinessPartnership() {
        return this == PARTNER || this == ALLIANCE || this == JOINT_VENTURE || 
               this == STRATEGIC_PARTNER || this == TECHNOLOGY_PARTNER || this == CHANNEL_PARTNER;
    }

    public boolean isCommercial() {
        return this == VENDOR || this == CUSTOMER || this == SUPPLIER || 
               this == DISTRIBUTOR || this == RESELLER;
    }

    public boolean isCompetitive() {
        return this == COMPETITOR;
    }
}