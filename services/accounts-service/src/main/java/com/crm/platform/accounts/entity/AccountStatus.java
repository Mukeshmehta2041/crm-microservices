package com.crm.platform.accounts.entity;

public enum AccountStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    PROSPECT("Prospect"),
    CUSTOMER("Customer"),
    FORMER_CUSTOMER("Former Customer"),
    SUSPENDED("Suspended"),
    ARCHIVED("Archived");

    private final String displayName;

    AccountStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}