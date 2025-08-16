package com.crm.platform.accounts.entity;

public enum AccountType {
    PROSPECT("Prospect"),
    CUSTOMER("Customer"),
    PARTNER("Partner"),
    VENDOR("Vendor"),
    COMPETITOR("Competitor"),
    RESELLER("Reseller"),
    INTEGRATOR("Integrator"),
    INVESTOR("Investor"),
    OTHER("Other");

    private final String displayName;

    AccountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}