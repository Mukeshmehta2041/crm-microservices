package com.crm.platform.security.rbac;

/**
 * Enumeration of all system permissions for granular access control
 */
public enum Permission {
    // User Management
    USER_CREATE("user:create", "Create new users"),
    USER_READ("user:read", "View user information"),
    USER_UPDATE("user:update", "Update user information"),
    USER_DELETE("user:delete", "Delete users"),
    USER_MANAGE_ROLES("user:manage_roles", "Manage user roles and permissions"),
    
    // Contact Management
    CONTACT_CREATE("contact:create", "Create new contacts"),
    CONTACT_READ("contact:read", "View contact information"),
    CONTACT_UPDATE("contact:update", "Update contact information"),
    CONTACT_DELETE("contact:delete", "Delete contacts"),
    CONTACT_IMPORT("contact:import", "Import contacts from external sources"),
    CONTACT_EXPORT("contact:export", "Export contact data"),
    CONTACT_MERGE("contact:merge", "Merge duplicate contacts"),
    
    // Deal Management
    DEAL_CREATE("deal:create", "Create new deals"),
    DEAL_READ("deal:read", "View deal information"),
    DEAL_UPDATE("deal:update", "Update deal information"),
    DEAL_DELETE("deal:delete", "Delete deals"),
    DEAL_MOVE_STAGE("deal:move_stage", "Move deals between pipeline stages"),
    DEAL_FORECAST("deal:forecast", "View deal forecasting data"),
    
    // Lead Management
    LEAD_CREATE("lead:create", "Create new leads"),
    LEAD_READ("lead:read", "View lead information"),
    LEAD_UPDATE("lead:update", "Update lead information"),
    LEAD_DELETE("lead:delete", "Delete leads"),
    LEAD_CONVERT("lead:convert", "Convert leads to contacts/deals"),
    LEAD_ASSIGN("lead:assign", "Assign leads to users"),
    LEAD_SCORE("lead:score", "Manage lead scoring"),
    
    // Account Management
    ACCOUNT_CREATE("account:create", "Create new accounts"),
    ACCOUNT_READ("account:read", "View account information"),
    ACCOUNT_UPDATE("account:update", "Update account information"),
    ACCOUNT_DELETE("account:delete", "Delete accounts"),
    ACCOUNT_MANAGE_HIERARCHY("account:manage_hierarchy", "Manage account hierarchies"),
    
    // Activity Management
    ACTIVITY_CREATE("activity:create", "Create new activities"),
    ACTIVITY_READ("activity:read", "View activity information"),
    ACTIVITY_UPDATE("activity:update", "Update activity information"),
    ACTIVITY_DELETE("activity:delete", "Delete activities"),
    ACTIVITY_ASSIGN("activity:assign", "Assign activities to users"),
    
    // Pipeline Management
    PIPELINE_CREATE("pipeline:create", "Create new pipelines"),
    PIPELINE_READ("pipeline:read", "View pipeline information"),
    PIPELINE_UPDATE("pipeline:update", "Update pipeline configuration"),
    PIPELINE_DELETE("pipeline:delete", "Delete pipelines"),
    PIPELINE_MANAGE_STAGES("pipeline:manage_stages", "Manage pipeline stages"),
    PIPELINE_MANAGE_AUTOMATION("pipeline:manage_automation", "Manage pipeline automation rules"),
    
    // Custom Objects
    CUSTOM_OBJECT_CREATE("custom_object:create", "Create custom objects"),
    CUSTOM_OBJECT_READ("custom_object:read", "View custom objects"),
    CUSTOM_OBJECT_UPDATE("custom_object:update", "Update custom objects"),
    CUSTOM_OBJECT_DELETE("custom_object:delete", "Delete custom objects"),
    CUSTOM_FIELD_MANAGE("custom_field:manage", "Manage custom fields"),
    
    // Analytics and Reporting
    ANALYTICS_VIEW("analytics:view", "View analytics dashboards"),
    ANALYTICS_CREATE_REPORTS("analytics:create_reports", "Create custom reports"),
    ANALYTICS_EXPORT("analytics:export", "Export analytics data"),
    
    // Workflow Management
    WORKFLOW_CREATE("workflow:create", "Create workflows"),
    WORKFLOW_READ("workflow:read", "View workflows"),
    WORKFLOW_UPDATE("workflow:update", "Update workflows"),
    WORKFLOW_DELETE("workflow:delete", "Delete workflows"),
    WORKFLOW_EXECUTE("workflow:execute", "Execute workflows"),
    
    // System Administration
    SYSTEM_ADMIN("system:admin", "Full system administration access"),
    TENANT_MANAGE("tenant:manage", "Manage tenant settings"),
    INTEGRATION_MANAGE("integration:manage", "Manage external integrations"),
    SECURITY_AUDIT("security:audit", "View security audit logs"),
    
    // API Access
    API_READ("api:read", "Read access via API"),
    API_WRITE("api:write", "Write access via API"),
    API_ADMIN("api:admin", "Administrative API access");
    
    private final String code;
    private final String description;
    
    Permission(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getDomain() {
        return code.split(":")[0];
    }
    
    public String getAction() {
        return code.split(":")[1];
    }
    
    public static Permission fromCode(String code) {
        for (Permission permission : values()) {
            if (permission.code.equals(code)) {
                return permission;
            }
        }
        throw new IllegalArgumentException("Unknown permission code: " + code);
    }
}