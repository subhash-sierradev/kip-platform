-- ===========================================================================
-- FLYWAY V4 MIGRATION: Create Notifications Schema and Tables
-- Notifications Database Schema
-- ===========================================================================
-- Description: Creates a dedicated 'notifications' schema with all tables
-- required for the in-app notification system. Kept separate from the
-- integration_platform schema to isolate notification data and simplify
-- future extraction to a standalone service.
-- Note: The Flyway user must have CREATE SCHEMA privilege, or the DBA
-- must pre-create the schema before this migration executes.
-- ===========================================================================

CREATE SCHEMA IF NOT EXISTS notifications;
SET search_path TO notifications;

-- ===========================================================================
-- TABLE: notification_event_catalog
-- ===========================================================================
-- Catalog of all notification-triggering events supported by the platform
CREATE TABLE notifications.notification_event_catalog (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    event_key    VARCHAR(200) NOT NULL,
    entity_type   VARCHAR(100) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description       TEXT,
    is_enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    notify_initiator  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_date      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_notification_event_catalog PRIMARY KEY (id),
    CONSTRAINT uq_notification_event_catalog_key UNIQUE (event_key)
);

-- ===========================================================================
-- TABLE: notification_rule
-- ===========================================================================
-- Per-tenant rules that define when and how notifications are triggered
CREATE TABLE notifications.notification_rule (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(100) NOT NULL,
    event_id            UUID         NOT NULL,
    severity            VARCHAR(20)  NOT NULL,
    is_enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    version             BIGINT       NOT NULL DEFAULT 0,
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_date  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by    VARCHAR(255) NOT NULL DEFAULT 'system',
    CONSTRAINT pk_notification_rule PRIMARY KEY (id),
    CONSTRAINT chk_notification_rule_severity
        CHECK (severity IN ('INFO', 'WARNING', 'ERROR', 'SUCCESS')),
    CONSTRAINT fk_notification_rule_event
        FOREIGN KEY (event_id) REFERENCES notifications.notification_event_catalog (id),
    CONSTRAINT uq_notification_rule_tenant_event
        UNIQUE (tenant_id, event_id)
);

-- ===========================================================================
-- TABLE: notification_template
-- ===========================================================================
-- Per-tenant message templates used to render notification titles and bodies
CREATE TABLE notifications.notification_template (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(100) NOT NULL,
    event_id            UUID         NOT NULL,
    title_template      VARCHAR(255) NOT NULL,
    message_template    TEXT         NOT NULL,
    allowed_placeholders TEXT,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    version             BIGINT       NOT NULL DEFAULT 0,
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_date  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by    VARCHAR(255) NOT NULL DEFAULT 'system',
    CONSTRAINT pk_notification_template PRIMARY KEY (id),
    CONSTRAINT fk_notification_template_event
        FOREIGN KEY (event_id) REFERENCES notifications.notification_event_catalog (id)
);

-- ===========================================================================
-- TABLE: notification_recipient_policy
-- ===========================================================================
-- Defines the recipient audience for a notification rule: all users, admins, or selected users
-- One policy per rule enforced by the UNIQUE constraint on rule_id
CREATE TABLE notifications.notification_recipient_policy (
    id                 UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id          VARCHAR(100) NOT NULL,
    rule_id            UUID         NOT NULL,
    recipient_type     VARCHAR(50)  NOT NULL,
    is_deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    version            BIGINT       NOT NULL DEFAULT 0,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(255) NOT NULL DEFAULT 'system',
    CONSTRAINT pk_notification_recipient_policy PRIMARY KEY (id),
    CONSTRAINT uq_notification_recipient_policy_rule UNIQUE (rule_id),
    CONSTRAINT chk_notification_recipient_policy_type
        CHECK (recipient_type IN ('ALL_USERS', 'ADMINS_ONLY', 'SELECTED_USERS')),
    CONSTRAINT fk_notification_recipient_policy_rule
        FOREIGN KEY (rule_id) REFERENCES notifications.notification_rule (id)
);

-- ===========================================================================
-- TABLE: notification_recipient_user
-- ===========================================================================
-- Individual users assigned to a SELECTED_USERS recipient policy
CREATE TABLE notifications.notification_recipient_user (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(100) NOT NULL,
    recipient_policy_id UUID         NOT NULL,
    user_id             VARCHAR(255) NOT NULL,
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_notification_recipient_user PRIMARY KEY (id),
    CONSTRAINT fk_notification_recipient_user_policy
        FOREIGN KEY (recipient_policy_id) REFERENCES notifications.notification_recipient_policy (id)
);

-- ===========================================================================
-- TABLE: app_notifications
-- ===========================================================================
-- Runtime per-user notification records delivered to the in-app notification centre
CREATE TABLE notifications.app_notifications (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id    VARCHAR(100) NOT NULL,
    user_id      VARCHAR(255) NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    severity     VARCHAR(20)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    message      TEXT         NOT NULL,
    metadata     JSONB,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_app_notifications PRIMARY KEY (id),
    CONSTRAINT chk_app_notifications_severity
        CHECK (severity IN ('INFO', 'WARNING', 'ERROR', 'SUCCESS'))
);

-- ===========================================================================
-- INDEXES: notification_rule
-- ===========================================================================
-- Support tenant-scoped rule lookups
CREATE INDEX idx_notification_rule_tenant
    ON notifications.notification_rule (tenant_id);

-- ===========================================================================
-- INDEXES: notification_template
-- ===========================================================================
-- Support tenant-scoped template lookups
CREATE INDEX idx_notification_template_tenant
    ON notifications.notification_template (tenant_id);

-- ===========================================================================
-- INDEXES: app_notifications
-- ===========================================================================
-- Support tenant + user unread notification queries
CREATE INDEX idx_app_notifications_tenant_user
    ON notifications.app_notifications (tenant_id, user_id);

-- Support unread notification filtering
CREATE INDEX idx_app_notifications_tenant_unread
    ON notifications.app_notifications (tenant_id, is_read)
    WHERE is_read = FALSE;

-- Support time-ordered notification listing
CREATE INDEX idx_app_notifications_tenant_created
    ON notifications.app_notifications (tenant_id, created_date DESC);

-- Covering index for countAllGrouped aggregate: enables index-only scan
-- (tenant_id, user_id) as B-tree key; is_read + severity in INCLUDE so
-- the planner never fetches heap pages to aggregate those columns
CREATE INDEX idx_app_notifications_count_grouped
    ON notifications.app_notifications (tenant_id, user_id)
    INCLUDE (is_read, severity);

-- Composite index for paginated list with ORDER BY created_date DESC:
-- three-column key lets the planner traverse rows in declared order,
-- eliminating the sort step for findByTenantIdAndUserIdWithFilters
CREATE INDEX idx_app_notifications_tenant_user_created
    ON notifications.app_notifications (tenant_id, user_id, created_date DESC);

