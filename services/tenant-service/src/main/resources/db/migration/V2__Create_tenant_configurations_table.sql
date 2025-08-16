-- Create tenant_configurations table
CREATE TABLE tenant_configurations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    config_key VARCHAR(255) NOT NULL,
    config_value TEXT,
    config_type VARCHAR(50) NOT NULL DEFAULT 'STRING',
    category VARCHAR(100),
    description VARCHAR(500),
    is_encrypted BOOLEAN DEFAULT FALSE,
    is_system BOOLEAN DEFAULT FALSE,
    is_editable BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_config_type CHECK (config_type IN ('STRING', 'BOOLEAN', 'INTEGER', 'LONG', 'DOUBLE', 'JSON')),
    CONSTRAINT uk_tenant_config_key UNIQUE (tenant_id, config_key)
);

-- Create indexes for tenant_configurations table
CREATE INDEX idx_tenant_config_tenant_id ON tenant_configurations(tenant_id);
CREATE INDEX idx_tenant_config_key ON tenant_configurations(config_key);
CREATE INDEX idx_tenant_config_category ON tenant_configurations(category);
CREATE INDEX idx_tenant_config_system ON tenant_configurations(is_system);
CREATE INDEX idx_tenant_config_editable ON tenant_configurations(is_editable);

-- Create trigger to update updated_at timestamp
CREATE TRIGGER update_tenant_configurations_updated_at BEFORE UPDATE ON tenant_configurations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert default configurations for system-wide settings
INSERT INTO tenant_configurations (tenant_id, config_key, config_value, config_type, category, description, is_system, is_editable) VALUES
-- Default system configurations that will be copied to new tenants
('00000000-0000-0000-0000-000000000000', 'timezone', 'UTC', 'STRING', 'general', 'Default timezone', TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'date_format', 'YYYY-MM-DD', 'STRING', 'general', 'Default date format', TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'time_format', '24h', 'STRING', 'general', 'Default time format (12h/24h)', TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'currency', 'USD', 'STRING', 'general', 'Default currency', TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'language', 'en-US', 'STRING', 'general', 'Default language', TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'theme', 'light', 'STRING', 'ui', 'Default theme', TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'primary_color', '#007bff', 'STRING', 'ui', 'Default primary color', TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'secondary_color', '#6c757d', 'STRING', 'ui', 'Default secondary color', TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'email_notifications', 'true', 'BOOLEAN', 'notifications', 'Enable email notifications', TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'sms_notifications', 'false', 'BOOLEAN', 'notifications', 'Enable SMS notifications', TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'max_file_size_mb', '10', 'INTEGER', 'files', 'Maximum file size in MB', TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'allowed_file_types', 'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx', 'STRING', 'files', 'Allowed file types', TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'session_timeout_minutes', '60', 'INTEGER', 'security', 'Session timeout in minutes', TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'two_factor_required', 'false', 'BOOLEAN', 'security', 'Require two-factor authentication', TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'api_rate_limit', '1000', 'INTEGER', 'api', 'API rate limit per hour', TRUE, TRUE);