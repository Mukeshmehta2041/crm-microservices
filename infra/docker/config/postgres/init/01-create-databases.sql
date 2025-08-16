-- Create databases for each service
CREATE DATABASE auth_db;
CREATE DATABASE tenant_db;
CREATE DATABASE users_db;
CREATE DATABASE deals_db;
CREATE DATABASE leads_db;
CREATE DATABASE accounts_db;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE auth_db TO crm_user;
GRANT ALL PRIVILEGES ON DATABASE tenant_db TO crm_user;
GRANT ALL PRIVILEGES ON DATABASE users_db TO crm_user;
GRANT ALL PRIVILEGES ON DATABASE deals_db TO crm_user;
GRANT ALL PRIVILEGES ON DATABASE leads_db TO crm_user;
GRANT ALL PRIVILEGES ON DATABASE accounts_db TO crm_user;