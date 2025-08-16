package com.crm.platform.security.rbac;

import java.util.Set;
import java.util.EnumSet;

/**
 * Predefined system roles with associated permissions
 */
public enum Role {
    SUPER_ADMIN("super_admin", "Super Administrator", 
        EnumSet.of(Permission.SYSTEM_ADMIN, Permission.TENANT_MANAGE, Permission.SECURITY_AUDIT,
                  Permission.INTEGRATION_MANAGE, Permission.API_ADMIN)),
    
    TENANT_ADMIN("tenant_admin", "Tenant Administrator",
        EnumSet.of(Permission.USER_CREATE, Permission.USER_READ, Permission.USER_UPDATE, 
                  Permission.USER_DELETE, Permission.USER_MANAGE_ROLES,
                  Permission.PIPELINE_CREATE, Permission.PIPELINE_READ, Permission.PIPELINE_UPDATE,
                  Permission.PIPELINE_DELETE, Permission.PIPELINE_MANAGE_STAGES, Permission.PIPELINE_MANAGE_AUTOMATION,
                  Permission.CUSTOM_OBJECT_CREATE, Permission.CUSTOM_OBJECT_READ, Permission.CUSTOM_OBJECT_UPDATE,
                  Permission.CUSTOM_OBJECT_DELETE, Permission.CUSTOM_FIELD_MANAGE,
                  Permission.WORKFLOW_CREATE, Permission.WORKFLOW_READ, Permission.WORKFLOW_UPDATE,
                  Permission.WORKFLOW_DELETE, Permission.WORKFLOW_EXECUTE,
                  Permission.ANALYTICS_VIEW, Permission.ANALYTICS_CREATE_REPORTS, Permission.ANALYTICS_EXPORT,
                  Permission.API_READ, Permission.API_WRITE)),
    
    SALES_MANAGER("sales_manager", "Sales Manager",
        EnumSet.of(Permission.CONTACT_CREATE, Permission.CONTACT_READ, Permission.CONTACT_UPDATE,
                  Permission.CONTACT_DELETE, Permission.CONTACT_IMPORT, Permission.CONTACT_EXPORT,
                  Permission.CONTACT_MERGE,
                  Permission.DEAL_CREATE, Permission.DEAL_READ, Permission.DEAL_UPDATE,
                  Permission.DEAL_DELETE, Permission.DEAL_MOVE_STAGE, Permission.DEAL_FORECAST,
                  Permission.LEAD_CREATE, Permission.LEAD_READ, Permission.LEAD_UPDATE,
                  Permission.LEAD_DELETE, Permission.LEAD_CONVERT, Permission.LEAD_ASSIGN,
                  Permission.LEAD_SCORE,
                  Permission.ACCOUNT_CREATE, Permission.ACCOUNT_READ, Permission.ACCOUNT_UPDATE,
                  Permission.ACCOUNT_DELETE, Permission.ACCOUNT_MANAGE_HIERARCHY,
                  Permission.ACTIVITY_CREATE, Permission.ACTIVITY_READ, Permission.ACTIVITY_UPDATE,
                  Permission.ACTIVITY_DELETE, Permission.ACTIVITY_ASSIGN,
                  Permission.PIPELINE_READ, Permission.PIPELINE_MANAGE_STAGES,
                  Permission.ANALYTICS_VIEW, Permission.ANALYTICS_CREATE_REPORTS,
                  Permission.API_READ, Permission.API_WRITE)),
    
    SALES_REP("sales_rep", "Sales Representative",
        EnumSet.of(Permission.CONTACT_CREATE, Permission.CONTACT_READ, Permission.CONTACT_UPDATE,
                  Permission.CONTACT_EXPORT,
                  Permission.DEAL_CREATE, Permission.DEAL_READ, Permission.DEAL_UPDATE,
                  Permission.DEAL_MOVE_STAGE,
                  Permission.LEAD_CREATE, Permission.LEAD_READ, Permission.LEAD_UPDATE,
                  Permission.LEAD_CONVERT,
                  Permission.ACCOUNT_READ, Permission.ACCOUNT_UPDATE,
                  Permission.ACTIVITY_CREATE, Permission.ACTIVITY_READ, Permission.ACTIVITY_UPDATE,
                  Permission.ACTIVITY_DELETE,
                  Permission.PIPELINE_READ,
                  Permission.ANALYTICS_VIEW,
                  Permission.API_READ, Permission.API_WRITE)),
    
    MARKETING_MANAGER("marketing_manager", "Marketing Manager",
        EnumSet.of(Permission.CONTACT_CREATE, Permission.CONTACT_READ, Permission.CONTACT_UPDATE,
                  Permission.CONTACT_IMPORT, Permission.CONTACT_EXPORT, Permission.CONTACT_MERGE,
                  Permission.LEAD_CREATE, Permission.LEAD_READ, Permission.LEAD_UPDATE,
                  Permission.LEAD_DELETE, Permission.LEAD_ASSIGN, Permission.LEAD_SCORE,
                  Permission.ACCOUNT_READ, Permission.ACCOUNT_UPDATE,
                  Permission.ACTIVITY_CREATE, Permission.ACTIVITY_READ, Permission.ACTIVITY_UPDATE,
                  Permission.WORKFLOW_CREATE, Permission.WORKFLOW_READ, Permission.WORKFLOW_UPDATE,
                  Permission.WORKFLOW_EXECUTE,
                  Permission.ANALYTICS_VIEW, Permission.ANALYTICS_CREATE_REPORTS, Permission.ANALYTICS_EXPORT,
                  Permission.API_READ, Permission.API_WRITE)),
    
    MARKETING_SPECIALIST("marketing_specialist", "Marketing Specialist",
        EnumSet.of(Permission.CONTACT_CREATE, Permission.CONTACT_READ, Permission.CONTACT_UPDATE,
                  Permission.CONTACT_IMPORT, Permission.CONTACT_EXPORT,
                  Permission.LEAD_CREATE, Permission.LEAD_READ, Permission.LEAD_UPDATE,
                  Permission.ACCOUNT_READ,
                  Permission.ACTIVITY_CREATE, Permission.ACTIVITY_READ, Permission.ACTIVITY_UPDATE,
                  Permission.WORKFLOW_READ, Permission.WORKFLOW_EXECUTE,
                  Permission.ANALYTICS_VIEW,
                  Permission.API_READ)),
    
    CUSTOMER_SUCCESS("customer_success", "Customer Success Manager",
        EnumSet.of(Permission.CONTACT_READ, Permission.CONTACT_UPDATE,
                  Permission.ACCOUNT_READ, Permission.ACCOUNT_UPDATE,
                  Permission.ACTIVITY_CREATE, Permission.ACTIVITY_READ, Permission.ACTIVITY_UPDATE,
                  Permission.ACTIVITY_DELETE,
                  Permission.ANALYTICS_VIEW,
                  Permission.API_READ)),
    
    ANALYST("analyst", "Business Analyst",
        EnumSet.of(Permission.CONTACT_READ, Permission.DEAL_READ, Permission.LEAD_READ,
                  Permission.ACCOUNT_READ, Permission.ACTIVITY_READ, Permission.PIPELINE_READ,
                  Permission.ANALYTICS_VIEW, Permission.ANALYTICS_CREATE_REPORTS, Permission.ANALYTICS_EXPORT,
                  Permission.API_READ)),
    
    VIEWER("viewer", "Read-Only Viewer",
        EnumSet.of(Permission.CONTACT_READ, Permission.DEAL_READ, Permission.LEAD_READ,
                  Permission.ACCOUNT_READ, Permission.ACTIVITY_READ, Permission.PIPELINE_READ,
                  Permission.ANALYTICS_VIEW, Permission.API_READ)),
    
    SYSTEM_SERVICE("system_service", "System Service",
        EnumSet.of(Permission.SYSTEM_ADMIN, Permission.API_ADMIN, Permission.API_READ, Permission.API_WRITE));
    
    private final String code;
    private final String displayName;
    private final Set<Permission> permissions;
    
    Role(String code, String displayName, Set<Permission> permissions) {
        this.code = code;
        this.displayName = displayName;
        this.permissions = EnumSet.copyOf(permissions);
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Set<Permission> getPermissions() {
        return EnumSet.copyOf(permissions);
    }
    
    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }
    
    public boolean hasAnyPermission(Permission... permissions) {
        for (Permission permission : permissions) {
            if (this.permissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasAllPermissions(Permission... permissions) {
        for (Permission permission : permissions) {
            if (!this.permissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }
    
    public static Role fromCode(String code) {
        for (Role role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role code: " + code);
    }
}