-- ===========================================================================
-- FLYWAY V6 MIGRATION: Update Integration Connection Notification Templates
-- ===========================================================================
-- Description: Updates the GLOBAL notification templates for the three
-- INTEGRATION_CONNECTION_* events to include {{serviceType}} (e.g. Jira,
-- ArcGIS, Confluence) in both title and message, as per KIP-543.
-- Only GLOBAL templates are affected; tenant-custom overrides are untouched.
-- ===========================================================================

UPDATE notifications.notification_template nt
SET
    title_template        = t.new_title,
    message_template      = t.new_message,
    last_modified_date    = CURRENT_TIMESTAMP,
    last_modified_by      = 'system'
FROM (VALUES
    ('INTEGRATION_CONNECTION_CREATED',
     '{{serviceType}} credentials created: {{connectionName}}',
     '"{{connectionName}}" ({{serviceType}}) credentials were created by {{createdBy}} on {{timestamp}}.'),

    ('INTEGRATION_CONNECTION_SECRET_UPDATED',
     '{{serviceType}} credentials updated: {{connectionName}}',
     'Credentials for "{{connectionName}}" ({{serviceType}}) were updated by {{updatedBy}} on {{timestamp}}.'),

    ('INTEGRATION_CONNECTION_DELETED',
     '{{serviceType}} credentials deleted: {{connectionName}}',
     '"{{connectionName}}" ({{serviceType}}) credentials were permanently deleted by {{deletedBy}} on {{timestamp}}.')

) AS t(event_key, new_title, new_message)

JOIN notifications.notification_event_catalog e
     ON e.event_key = t.event_key

WHERE nt.tenant_id = 'GLOBAL'
  AND nt.event_id  = e.id;
