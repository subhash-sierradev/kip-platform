-- ===========================================================================
-- FLYWAY V9 MIGRATION: Add CONFLUENCE_INTEGRATION to audit_logs entity_type check constraint
-- ===========================================================================

SET search_path TO integration_platform;

-- Drop existing check constraint
ALTER TABLE integration_platform.audit_logs
    DROP CONSTRAINT IF EXISTS audit_logs_entity_type_check;

-- Add updated check constraint with CONFLUENCE_INTEGRATION
ALTER TABLE integration_platform.audit_logs
    ADD CONSTRAINT audit_logs_entity_type_check
    CHECK (entity_type IN (
        'INTEGRATION',
        'INTEGRATION_CONNECTION',
        'JIRA_WEBHOOK',
        'JIRA_WEBHOOK_EVENT',
        'ARCGIS_INTEGRATION',
        'CONFLUENCE_INTEGRATION',
        'SITE_CONFIG',
        'CACHE'
    ));
