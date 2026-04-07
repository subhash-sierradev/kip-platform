-- ===========================================================================
-- FLYWAY V7 MIGRATION: Insert Confluence Notification Events
-- ===========================================================================

SET search_path TO notifications;

INSERT INTO notifications.notification_event_catalog
    (id, event_key, entity_type, display_name, description, is_enabled, notify_initiator, created_date)
VALUES
    (gen_random_uuid(), 'CONFLUENCE_INTEGRATION_CREATED',           'CONFLUENCE_INTEGRATION', 'Confluence Integration Created',            'Triggered when a new Confluence integration is created.',                                         TRUE, FALSE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'CONFLUENCE_INTEGRATION_UPDATED',           'CONFLUENCE_INTEGRATION', 'Confluence Integration Updated',            'Triggered when a Confluence integration configuration is updated.',                              TRUE, FALSE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'CONFLUENCE_INTEGRATION_DELETED',           'CONFLUENCE_INTEGRATION', 'Confluence Integration Deleted',            'Triggered when a Confluence integration is permanently deleted.',                                TRUE, FALSE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'CONFLUENCE_INTEGRATION_ENABLED',           'CONFLUENCE_INTEGRATION', 'Confluence Integration Enabled',            'Triggered when a Confluence integration is re-enabled.',                                         TRUE, FALSE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'CONFLUENCE_INTEGRATION_DISABLED',          'CONFLUENCE_INTEGRATION', 'Confluence Integration Disabled',           'Triggered when a Confluence integration is disabled.',                                           TRUE, FALSE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'CONFLUENCE_INTEGRATION_JOB_COMPLETED',    'CONFLUENCE_INTEGRATION', 'Confluence Job Completed',                  'Triggered when a Confluence integration job completes successfully.',                            TRUE, TRUE,  CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'CONFLUENCE_INTEGRATION_JOB_FAILED',       'CONFLUENCE_INTEGRATION', 'Confluence Job Failed',                     'Triggered when a Confluence integration job fails.',                                              TRUE, TRUE,  CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
