-- Create security_audit_log table
CREATE TABLE security_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    tenant_id UUID,
    event_type VARCHAR(50) NOT NULL,
    event_description VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    session_id VARCHAR(255),
    additional_data TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_audit_status CHECK (status IN ('SUCCESS', 'FAILURE', 'WARNING'))
);

-- Create indexes for security_audit_log table
CREATE INDEX idx_audit_log_user_id ON security_audit_log(user_id);
CREATE INDEX idx_audit_log_tenant_id ON security_audit_log(tenant_id);
CREATE INDEX idx_audit_log_event_type ON security_audit_log(event_type);
CREATE INDEX idx_audit_log_timestamp ON security_audit_log(timestamp);
CREATE INDEX idx_audit_log_ip_address ON security_audit_log(ip_address);
CREATE INDEX idx_audit_log_status ON security_audit_log(status);
CREATE INDEX idx_audit_log_session_id ON security_audit_log(session_id);

-- Create composite indexes for common queries
CREATE INDEX idx_audit_log_user_event_time ON security_audit_log(user_id, event_type, timestamp);
CREATE INDEX idx_audit_log_ip_event_time ON security_audit_log(ip_address, event_type, timestamp);
CREATE INDEX idx_audit_log_tenant_time ON security_audit_log(tenant_id, timestamp);