-- KIP-437: Add retry lineage tracking to integration_job_executions
-- Introduces original_job_id and retry_attempt to group executions and retries under one logical job

-- Add columns to track retry lineage
ALTER TABLE integration_platform.integration_job_executions
    ADD COLUMN IF NOT EXISTS original_job_id UUID,
    ADD COLUMN IF NOT EXISTS retry_attempt INT NOT NULL DEFAULT 0;

ALTER TABLE integration_platform.integration_schedules
    ADD COLUMN IF NOT EXISTS time_calculation_mode VARCHAR(30) NOT NULL DEFAULT 'FLEXIBLE_INTERVAL',
    ADD COLUMN IF NOT EXISTS processed_until TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS business_time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC';

-- Create index for efficient lineage lookups
CREATE INDEX IF NOT EXISTS idx_original_job_id 
    ON integration_platform.integration_job_executions(original_job_id);

-- Add partial unique index to enforce one retry attempt per lineage
-- Only applies to rows where original_job_id is NOT NULL
-- ArcGIS rows with null original_job_id are excluded from this constraint
CREATE UNIQUE INDEX IF NOT EXISTS unique_retry_per_job 
    ON integration_platform.integration_job_executions(original_job_id, retry_attempt) 
    WHERE original_job_id IS NOT NULL;