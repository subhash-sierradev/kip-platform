ALTER TABLE integration_platform.audit_logs
ADD COLUMN IF NOT EXISTS client_ip_address VARCHAR(45);
