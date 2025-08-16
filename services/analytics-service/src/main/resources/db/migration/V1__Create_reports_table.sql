CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    report_type VARCHAR(100) NOT NULL,
    query_definition TEXT NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    organization_id VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_scheduled BOOLEAN NOT NULL DEFAULT false,
    schedule_expression VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE report_parameters (
    report_id BIGINT NOT NULL,
    parameter_name VARCHAR(255) NOT NULL,
    parameter_value TEXT,
    PRIMARY KEY (report_id, parameter_name),
    FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
);

CREATE INDEX idx_reports_organization_id ON reports(organization_id);
CREATE INDEX idx_reports_created_by ON reports(created_by);
CREATE INDEX idx_reports_is_active ON reports(is_active);
CREATE INDEX idx_reports_is_scheduled ON reports(is_scheduled);