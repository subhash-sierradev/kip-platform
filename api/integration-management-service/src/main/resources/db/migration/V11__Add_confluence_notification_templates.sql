-- ===========================================================================
-- FLYWAY V11 MIGRATION: Add Confluence Notification Templates + Adhoc-Run Event
-- ===========================================================================
-- Adds the missing GLOBAL notification_template rows for all 8 Confluence
-- notification events (7 seeded in V7 + 1 new adhoc-run event added here).
-- All inserts are idempotent via ON CONFLICT / NOT EXISTS guards.
-- ===========================================================================

SET search_path TO notifications;

-- ---------------------------------------------------------------------------
-- 1. Add the adhoc-run catalog event (not present in V7)
-- ---------------------------------------------------------------------------
INSERT INTO notifications.notification_event_catalog
    (id, event_key, entity_type, display_name, description, is_enabled, notify_initiator, created_date)
VALUES
    (gen_random_uuid(), 'CONFLUENCE_INTEGRATION_JOB_ADHOC_RUN', 'CONFLUENCE_INTEGRATION',
     'Confluence Job Ad-hoc Run Triggered',
     'Triggered when a Confluence integration job is run manually.',
     TRUE, FALSE, CURRENT_TIMESTAMP)
ON CONFLICT (event_key) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Seed GLOBAL templates for all 8 Confluence notification events
-- ---------------------------------------------------------------------------
INSERT INTO notifications.notification_template
(id, tenant_id, event_id, title_template, message_template, allowed_placeholders,
 is_deleted, version, created_date, created_by, last_modified_date, last_modified_by)

SELECT
    gen_random_uuid(),
    'GLOBAL',
    e.id,
    t.title_template,
    t.message_template,
    '',
    FALSE,
    0,
    CURRENT_TIMESTAMP,
    'system',
    CURRENT_TIMESTAMP,
    'system'

FROM (VALUES
    ('CONFLUENCE_INTEGRATION_CREATED',
     'Confluence Integration Created: {{integrationName}}',
     'A new Confluence integration "{{integrationName}}" was created by {{createdBy}} on {{timestamp}}.'),

    ('CONFLUENCE_INTEGRATION_UPDATED',
     'Confluence Integration Updated: {{integrationName}}',
     'The Confluence integration "{{integrationName}}" was updated by {{updatedBy}} on {{timestamp}}.'),

    ('CONFLUENCE_INTEGRATION_DELETED',
     'Confluence Integration Deleted: {{integrationName}}',
     'The Confluence integration "{{integrationName}}" was permanently deleted by {{deletedBy}} on {{timestamp}}.'),

    ('CONFLUENCE_INTEGRATION_ENABLED',
     'Confluence Integration Enabled: {{integrationName}}',
     'The Confluence integration "{{integrationName}}" has been re-enabled by {{enabledBy}} on {{timestamp}}.'),

    ('CONFLUENCE_INTEGRATION_DISABLED',
     'Confluence Integration Disabled: {{integrationName}}',
     'The Confluence integration "{{integrationName}}" has been disabled by {{disabledBy}} on {{timestamp}}.'),

    ('CONFLUENCE_INTEGRATION_JOB_COMPLETED',
     'Confluence Job Completed: {{integrationName}}',
     'The Confluence integration "{{integrationName}}" completed successfully at {{timestamp}}. {{recordCount}} records were processed.'),

    ('CONFLUENCE_INTEGRATION_JOB_FAILED',
     'Confluence Job Failed: {{integrationName}}',
     'The Confluence integration "{{integrationName}}" failed at {{timestamp}}. Error: {{errorMessage}}.'),

    ('CONFLUENCE_INTEGRATION_JOB_ADHOC_RUN',
     'Confluence Job Ad-hoc Run Triggered: {{integrationName}}',
     'An ad-hoc run of the Confluence integration "{{integrationName}}" was triggered by {{triggeredBy}} at {{timestamp}}.')

) AS t(event_key, title_template, message_template)

    JOIN notifications.notification_event_catalog e
         ON e.event_key = t.event_key

WHERE NOT EXISTS (
    SELECT 1
    FROM notifications.notification_template nt
    WHERE nt.tenant_id = 'GLOBAL'
      AND nt.event_id = e.id
);
