-- Create databases for each service
CREATE DATABASE auth_db;
CREATE DATABASE tenant_db;
CREATE DATABASE users_db;
CREATE DATABASE contacts_db;
CREATE DATABASE deals_db;
CREATE DATABASE leads_db;
CREATE DATABASE accounts_db;
CREATE DATABASE activities_db;
CREATE DATABASE pipelines_db;
CREATE DATABASE custom_objects_db;
CREATE DATABASE analytics_db;
CREATE DATABASE workflow_db;

-- Grant permissions to crm_user
GRANT ALL PRIVILEGES ON DATABASE auth_db TO crm_user;
GRANT ALL PRIVILEGES ON DATABASE tenant_db TO crm_user;
GRANT ALL PRIVILEGES ON DATABASE users_db TO crm_user;
GRANT ALL PRIVILEGES ON DATABASE contacts_db TO crm_user;
GRANT ALL PRIVILEGES ON DATABASE deals_db TO crm_user;
GRANT ALL PRIVILEGES ON DATABASE leads_db TO crm_user;
GRANT ALL PRIVILEGES ON DATABASE accounts_db TO crm_user;
GRANT ALL PRIVILEGES ON DATABASE activities_db TO crm_user;
GRANT ALL PRIVILEGES ON DATABASE pipelines_db TO crm_user;
GRANT ALL PRIVILEGES ON DATABASE custom_objects_db TO crm_user;
GRANT ALL PRIVILEGES ON DATABASE analytics_db TO crm_user;
GRANT ALL PRIVILEGES ON DATABASE workflow_db TO crm_user;