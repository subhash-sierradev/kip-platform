-- ===========================================================================
-- FLYWAY V5 MIGRATION: Insert Sample Data for Notifications Schema
-- Notifications Sample / Seed Data
-- ===========================================================================
-- Description: Seeds the notifications schema with a full event catalog
-- (platform-wide), and organization-2 / org2-admin-specific rules,
-- templates, recipient policies, channels, and in-app notifications.
-- All inserts use ON CONFLICT / NOT EXISTS checks to remain idempotent.
-- ===========================================================================

SET search_path TO notifications;

-- ===========================================================================
-- SEED: notification_event_catalog  (platform-wide, no tenant)
-- ===========================================================================

INSERT INTO notifications.notification_event_catalog
    (id, event_key, entity_type, display_name, description, is_enabled, notify_initiator, created_date)
VALUES
    -- ArcGIS Integration lifecycle events
    (gen_random_uuid(), 'ARCGIS_INTEGRATION_CREATED',            'ARCGIS_INTEGRATION',     'ArcGIS Integration Created',              'Triggered when a new ArcGIS integration is created.',                                                     TRUE, FALSE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'ARCGIS_INTEGRATION_UPDATED',            'ARCGIS_INTEGRATION',     'ArcGIS Integration Updated',              'Triggered when an ArcGIS integration configuration is updated.',                                                 TRUE, FALSE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'ARCGIS_INTEGRATION_DISABLED',           'ARCGIS_INTEGRATION',     'ArcGIS Integration Disabled',             'Triggered when an ArcGIS integration is disabled.',                                                              TRUE, FALSE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'ARCGIS_INTEGRATION_ENABLED',            'ARCGIS_INTEGRATION',     'ArcGIS Integration Enabled',              'Triggered when an ArcGIS integration is re-enabled.',                                                          TRUE, FALSE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'ARCGIS_INTEGRATION_DELETED',            'ARCGIS_INTEGRATION',     'ArcGIS Integration Deleted',              'Triggered when an ArcGIS integration is permanently deleted.',                                                 TRUE, FALSE, CURRENT_TIMESTAMP),
    -- ArcGIS Integration job events
    (gen_random_uuid(), 'ARCGIS_INTEGRATION_JOB_ADHOC_RUN',     'ARCGIS_INTEGRATION',     'ArcGIS Job Ad-hoc Run Triggered',         'Triggered when an ArcGIS integration job is run manually.',                                                     TRUE, FALSE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'ARCGIS_INTEGRATION_JOB_COMPLETED',     'ARCGIS_INTEGRATION',     'ArcGIS Job Completed',                    'Triggered when an ArcGIS integration job completes successfully.',                                              TRUE, TRUE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'ARCGIS_INTEGRATION_JOB_FAILED',        'ARCGIS_INTEGRATION',     'ArcGIS Job Failed',                       'Triggered when an ArcGIS integration job fails.',                                                           TRUE, TRUE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'ARCGIS_INTEGRATION_JOB_ABORTED',       'ARCGIS_INTEGRATION',     'ArcGIS Job Aborted',                      'Triggered when an ArcGIS integration job is aborted.',                                                           TRUE, TRUE, CURRENT_TIMESTAMP),
    -- Jira Webhook integration lifecycle events
    (gen_random_uuid(), 'JIRAWEBHOOK_INTEGRATION_CREATED',      'JIRA_WEBHOOK',           'Jira Webhook Created',                    'Triggered when a new Jira webhook integration is created.',                                                                TRUE, FALSE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'JIRAWEBHOOK_INTEGRATION_UPDATED',      'JIRA_WEBHOOK',           'Jira Webhook Updated',                    'Triggered when a Jira webhook integration configuration is updated.',                                                  TRUE, FALSE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'JIRAWEBHOOK_INTEGRATION_DISABLED',     'JIRA_WEBHOOK',           'Jira Webhook Disabled',                   'Triggered when a Jira webhook integration is disabled.',                                                           TRUE, FALSE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'JIRAWEBHOOK_INTEGRATION_ENABLED',      'JIRA_WEBHOOK',           'Jira Webhook Enabled',                    'Triggered when a Jira webhook integration is re-enabled.',                                                            TRUE, FALSE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'JIRAWEBHOOK_INTEGRATION_DELETED',      'JIRA_WEBHOOK',           'Jira Webhook Deleted',                    'Triggered when a Jira webhook integration is permanently deleted.',                                                       TRUE, FALSE, CURRENT_TIMESTAMP),
    -- Jira Webhook event processing events    
    (gen_random_uuid(), 'JIRAWEBHOOK_EVENT_FAILED',             'JIRA_WEBHOOK',           'Jira Webhook Event Failed',               'Triggered when processing of a Jira webhook event fails.',                                                            TRUE, TRUE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'JIRAWEBHOOK_EVENT_COMPLETED',          'JIRA_WEBHOOK',           'Jira Webhook Event Completed',            'Triggered when a Jira webhook event is successfully processed.',                                                                   TRUE, TRUE, CURRENT_TIMESTAMP),
    -- Site config events
    (gen_random_uuid(), 'SITE_CONFIG_UPDATED',                  'SITE_CONFIG',            'Site Configuration Updated',              'Triggered when platform-level site configuration is updated.',                                                                   TRUE, FALSE, CURRENT_TIMESTAMP),
    -- Integration connection events
    (gen_random_uuid(), 'INTEGRATION_CONNECTION_CREATED',       'INTEGRATION_CONNECTION', 'Integration Connection Created',          'Triggered when a new integration connection is created.',                                                         TRUE, FALSE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'INTEGRATION_CONNECTION_SECRET_UPDATED','INTEGRATION_CONNECTION', 'Integration Connection Secret Updated',   'Triggered when the credentials of an integration connection are updated.',                            TRUE, FALSE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'INTEGRATION_CONNECTION_DELETED',       'INTEGRATION_CONNECTION', 'Integration Connection Deleted',          'Triggered when an integration connection is permanently deleted.',                                                TRUE, FALSE, CURRENT_TIMESTAMP)
ON CONFLICT (event_key) DO NOTHING;

-- ===========================================================================
-- SEED: notification_template  (tenant: organization-2)
-- ===========================================================================

INSERT INTO notifications.notification_template
(id, tenant_id, event_id, title_template, message_template, allowed_placeholders,
 is_deleted, version, created_date, created_by, last_modified_date, last_modified_by)

SELECT
    gen_random_uuid(),
    'GLOBAL',
    e.id,
    t.title_template,
    t.message_template,
    '',  -- ✅ set placeholder as blank string
    FALSE,
    0,
    CURRENT_TIMESTAMP,
    'system',
    CURRENT_TIMESTAMP,
    'system'

FROM (VALUES
          ('ARCGIS_INTEGRATION_CREATED',
           'ArcGIS Integration Created: {{integrationName}}',
           'A new ArcGIS integration "{{integrationName}}" was created by {{createdBy}} on {{timestamp}}.'),

          ('ARCGIS_INTEGRATION_UPDATED',
           'ArcGIS Integration Updated: {{integrationName}}',
           'The ArcGIS integration "{{integrationName}}" was updated by {{updatedBy}} on {{timestamp}}.'),

          ('ARCGIS_INTEGRATION_DISABLED',
           'ArcGIS Integration Disabled: {{integrationName}}',
           'The ArcGIS integration "{{integrationName}}" has been disabled by {{disabledBy}} on {{timestamp}}.'),

          ('ARCGIS_INTEGRATION_ENABLED',
           'ArcGIS Integration Enabled: {{integrationName}}',
           'The ArcGIS integration "{{integrationName}}" has been re-enabled by {{enabledBy}} on {{timestamp}}.'),

          ('ARCGIS_INTEGRATION_DELETED',
           'ArcGIS Integration Deleted: {{integrationName}}',
           'The ArcGIS integration "{{integrationName}}" was permanently deleted by {{deletedBy}} on {{timestamp}}.'),

          ('ARCGIS_INTEGRATION_JOB_ADHOC_RUN',
           'ArcGIS Job Ad-hoc Run Triggered: {{integrationName}}',
           'An ad-hoc run of the ArcGIS integration "{{integrationName}}" was triggered by {{triggeredBy}} at {{timestamp}}.'),

          ('ARCGIS_INTEGRATION_JOB_COMPLETED',
           'ArcGIS Job Completed: {{integrationName}}',
           'The ArcGIS integration "{{integrationName}}" completed successfully at {{timestamp}}. {{recordCount}} records were processed.'),

          ('ARCGIS_INTEGRATION_JOB_FAILED',
           'ArcGIS Job Failed: {{integrationName}}',
           'The ArcGIS integration "{{integrationName}}" failed at {{timestamp}}. Error: {{errorMessage}}.'),

          ('ARCGIS_INTEGRATION_JOB_ABORTED',
           'ArcGIS Job Aborted: {{integrationName}}',
           'The ArcGIS integration "{{integrationName}}" was aborted at {{timestamp}} by {{abortedBy}}.'),

          ('JIRAWEBHOOK_INTEGRATION_CREATED',
           'Jira Webhook Created: {{webhookName}}',
           'A new Jira webhook "{{webhookName}}" was created by {{createdBy}} on {{timestamp}}.'),

          ('JIRAWEBHOOK_INTEGRATION_UPDATED',
           'Jira Webhook Updated: {{webhookName}}',
           'The Jira webhook "{{webhookName}}" was updated by {{updatedBy}} on {{timestamp}}.'),

          ('JIRAWEBHOOK_INTEGRATION_DISABLED',
           'Jira Webhook Disabled: {{webhookName}}',
           'The Jira webhook "{{webhookName}}" has been disabled by {{disabledBy}} on {{timestamp}}.'),

          ('JIRAWEBHOOK_INTEGRATION_ENABLED',
           'Jira Webhook Enabled: {{webhookName}}',
           'The Jira webhook "{{webhookName}}" has been re-enabled by {{enabledBy}} on {{timestamp}}.'),

          ('JIRAWEBHOOK_INTEGRATION_DELETED',
           'Jira Webhook Deleted: {{webhookName}}',
           'The Jira webhook "{{webhookName}}" was permanently deleted by {{deletedBy}} on {{timestamp}}.'),
          
          ('JIRAWEBHOOK_EVENT_FAILED',
           'Jira Webhook Event Failed: {{webhookName}}',
           'Processing of a Jira event for webhook "{{webhookName}}" failed at {{timestamp}}. Error: {{errorMessage}}.'),

          ('JIRAWEBHOOK_EVENT_COMPLETED',
           'Jira Webhook Event Completed: {{webhookName}}',
           'A Jira event for webhook "{{webhookName}}" was processed successfully at {{timestamp}}.'),

          ('SITE_CONFIG_UPDATED',
           'Site Configuration Updated',
           'Platform site configuration (section: {{configSection}}) was updated by {{updatedBy}} on {{timestamp}}.'),

          ('INTEGRATION_CONNECTION_CREATED',
           'Integration Connection Created: {{connectionName}}',
           'A new integration connection "{{connectionName}}" was created by {{createdBy}} on {{timestamp}}.'),

          ('INTEGRATION_CONNECTION_SECRET_UPDATED',
           'Integration Connection Credentials Updated: {{connectionName}}',
           'The credentials for integration connection "{{connectionName}}" were updated by {{updatedBy}} on {{timestamp}}.'),

          ('INTEGRATION_CONNECTION_DELETED',
           'Integration Connection Deleted: {{connectionName}}',
           'The integration connection "{{connectionName}}" was permanently deleted by {{deletedBy}} on {{timestamp}}.')

     ) AS t(event_key, title_template, message_template)

         JOIN notifications.notification_event_catalog e
              ON e.event_key = t.event_key

WHERE NOT EXISTS (
    SELECT 1
    FROM notifications.notification_template nt
    WHERE nt.tenant_id = 'GLOBAL'
      AND nt.event_id = e.id
);

