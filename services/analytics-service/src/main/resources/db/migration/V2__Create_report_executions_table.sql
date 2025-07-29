CREATE TABLE report_executions (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    executed_by VARCHAR(255) NOT NULL,
    result_data TEXT,
    error_message TEXT,
    execution_time_ms BIGINT,
    record_count INTEGER,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
);

CREATE INDEX idx_report_executions_report_id ON report_executions(report_id);
CREATE INDEX idx_report_executions_status ON report_executions(status);
CREATE INDEX idx_report_executions_executed_by ON report_executions(executed_by);
CREATE INDEX idx_report_executions_executed_at ON report_executions(executed_at);