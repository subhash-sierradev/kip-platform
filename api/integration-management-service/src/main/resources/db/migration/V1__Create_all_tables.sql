-- ===========================================================================
-- FLYWAY V1 MIGRATION: Create Business Tables
-- Integration Platform Database Schema
-- ===========================================================================
-- Description: Creates the core business database schema for the Integration Platform
-- including all business entities, indexes, and constraints.
-- ===========================================================================

-- Create schema and set search path
CREATE SCHEMA IF NOT EXISTS integration_platform;
SET search_path TO integration_platform;

-- ===========================================================================
-- TABLE: tenant_profiles
-- ===========================================================================
CREATE TABLE integration_platform.tenant_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL UNIQUE,
    tenant_name VARCHAR(255),
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===========================================================================
-- TABLE: user_profiles
-- ===========================================================================
CREATE TABLE integration_platform.user_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_user_id VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),

    -- Role-based access control fields
    is_tenant_admin BOOLEAN NOT NULL DEFAULT false,

    -- Tenant identifier for multi-tenant isolation and composite FK constraints
    tenant_id VARCHAR(100) NOT NULL,

    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints: composite unique on (keycloak_user_id, tenant_id) for FK target
    CONSTRAINT uq_users_keycloak_user_tenant
        UNIQUE (keycloak_user_id, tenant_id),

    -- Foreign key to tenant_profiles via tenant_id
    CONSTRAINT fk_user_profiles_tenant
        FOREIGN KEY (tenant_id) REFERENCES integration_platform.tenant_profiles(tenant_id) ON DELETE RESTRICT
);

    -- Indexes to support foreign key validation and tenant-scoped lookups on user_profiles
    CREATE INDEX idx_user_profiles_tenant
        ON integration_platform.user_profiles (tenant_id);

    -- Indexes to support foreign key validation and tenant-scoped lookups on user_profiles
    CREATE INDEX idx_user_profiles_keycloak_user_tenant
        ON integration_platform.user_profiles (keycloak_user_id, tenant_id);

-- ===========================================================================
-- TABLE: vault_secrets
-- ===========================================================================
-- Temporary table to support vault secret storage if Azure Key Vault is service down used in dev environment
CREATE TABLE vault_secrets (
    secret_key     VARCHAR(255) PRIMARY KEY,
    secret_value   JSONB        NOT NULL,
    created_date   TIMESTAMP    NOT NULL,
    updated_date   TIMESTAMP    NOT NULL,
    is_deleted     BOOLEAN      NOT NULL DEFAULT false
);

-- ===========================================================================
-- TABLE: audit_logs
-- ===========================================================================
CREATE TABLE audit_logs (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL CHECK (entity_type IN ('INTEGRATION', 'INTEGRATION_CONNECTION', 'JIRA_WEBHOOK', 'JIRA_WEBHOOK_EVENT', 'ARCGIS_INTEGRATION', 'CONFLUENCE_INTEGRATION', 'SITE_CONFIG', 'CACHE')),
    entity_id VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100),
    action VARCHAR(50) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'UPSERT', 'DELETE', 'ENABLED', 'DISABLED', 'PENDING', 'RETRY', 'EXECUTE', 'RUN_NOW', 'LOGIN', 'LOGOUT', 'CLEAR_CACHE')),
    result VARCHAR(20) CHECK (result IN ('SUCCESS', 'FAILED', 'IN_PROGRESS')),
    performed_by VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    client_ip_address VARCHAR(45),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    -- Foreign key to tenant_profiles
    CONSTRAINT fk_audit_logs_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant_profiles(tenant_id) ON DELETE RESTRICT,
    -- Composite FK constraint for user reference
    CONSTRAINT fk_audit_logs_performed_user
        FOREIGN KEY (performed_by, tenant_id) REFERENCES user_profiles(keycloak_user_id, tenant_id) ON DELETE RESTRICT
);

-- Audit logs indexes (append-only table with time-series queries)
CREATE INDEX idx_audit_logs_tenant_timestamp ON audit_logs(tenant_id, timestamp);

-- ===========================================================================
-- TABLE: credential_types
-- ===========================================================================
-- Credential types for defining available authentication methods
CREATE TABLE credential_types (
    credential_auth_type VARCHAR(50) NOT NULL PRIMARY KEY,
    display_name VARCHAR(50) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    required_fields JSONB NOT NULL DEFAULT '[]'::jsonb
);

-- Credential types indexes
CREATE INDEX idx_credential_types_enabled ON credential_types(is_enabled) WHERE is_enabled = true;

-- ===========================================================================
-- TABLE: site_configs
-- ===========================================================================
-- Site-wide configuration settings with multi-tenant support
CREATE TABLE site_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(1000) NOT NULL,
    value_type VARCHAR(20) NOT NULL CHECK (value_type IN ('STRING', 'NUMBER', 'BOOLEAN', 'TIMESTAMP')),
    description VARCHAR(200),
    tenant_id VARCHAR(100) NOT NULL DEFAULT 'GLOBAL',
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    last_modified_by VARCHAR(255) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0,

    -- Foreign key to tenant_profiles for multi-tenant configurations
    CONSTRAINT fk_site_configs_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant_profiles(tenant_id) ON DELETE RESTRICT,

    -- Composite FK constraints for user references
    CONSTRAINT fk_site_configs_created_user
        FOREIGN KEY (created_by, tenant_id) REFERENCES user_profiles(keycloak_user_id, tenant_id) ON DELETE RESTRICT,
    CONSTRAINT fk_site_configs_last_modified_by
        FOREIGN KEY (last_modified_by, tenant_id) REFERENCES user_profiles(keycloak_user_id, tenant_id) ON DELETE RESTRICT
);

-- Site configs indexes
CREATE UNIQUE INDEX uq_site_configs_config_key_tenant_id ON site_configs(config_key, tenant_id);

-- ===========================================================================
-- TABLE: integration_connections
-- ===========================================================================
-- Integration connections for external systems
CREATE TABLE integration_connections (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    connection_hash_key VARCHAR(100) NOT NULL,
    secret_name VARCHAR(100) NOT NULL UNIQUE,
    fetch_mode VARCHAR(20) NOT NULL DEFAULT 'GET' CHECK (fetch_mode IN ('GET', 'POST')),
    service_type VARCHAR(50) NOT NULL,
    last_connection_status VARCHAR(50) CHECK (last_connection_status IN ('SUCCESS','FAILED')),
    last_connection_message TEXT,
    last_connection_test TIMESTAMP WITH TIME ZONE,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    last_modified_by VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,

    -- Foreign key to tenant_profiles
    CONSTRAINT fk_integration_connections_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant_profiles(tenant_id) ON DELETE RESTRICT,

    -- Composite FK constraints for user references
    CONSTRAINT fk_integration_connections_created_user
        FOREIGN KEY (created_by, tenant_id) REFERENCES user_profiles(keycloak_user_id, tenant_id) ON DELETE RESTRICT,
    CONSTRAINT fk_integration_connections_updated_user
        FOREIGN KEY (last_modified_by, tenant_id) REFERENCES user_profiles(keycloak_user_id, tenant_id) ON DELETE RESTRICT
);

-- Global uniqueness of active hash key
CREATE UNIQUE INDEX uq_integration_connections_hash_key_active ON integration_connections (connection_hash_key) WHERE is_deleted = false;

-- Integration connections indexes (heavily queried table)
CREATE INDEX idx_integration_connections_tenant_deleted ON integration_connections(tenant_id, is_deleted);
CREATE INDEX idx_integration_connections_service_type_tenant ON integration_connections(tenant_id, service_type);

-- ===========================================================================
-- TABLE: integration_schedules
-- ===========================================================================
CREATE TABLE integration_schedules (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    execution_date DATE,
    execution_time TIME NOT NULL,
    frequency_pattern VARCHAR(20) NOT NULL CHECK (frequency_pattern IN ('DAILY', 'WEEKLY', 'MONTHLY', 'CUSTOM')),
    daily_execution_interval INTEGER,
    day_schedule JSONB,
    month_schedule JSONB,
    is_execute_on_month_end BOOLEAN NOT NULL DEFAULT false,
    time_calculation_mode VARCHAR(30) NOT NULL DEFAULT 'FLEXIBLE_INTERVAL'
        CHECK (time_calculation_mode IN ('FIXED_DAY_BOUNDARY', 'FLEXIBLE_INTERVAL')),
    processed_until TIMESTAMP WITH TIME ZONE,
    business_time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    cron_expression VARCHAR(100)
);

-- ===========================================================================
-- TABLE: jira_webhooks
-- ===========================================================================
-- Jira webhook configurations
CREATE TABLE jira_webhooks (
    id VARCHAR(100) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    description TEXT,
    webhook_url VARCHAR(255) NOT NULL,
    connection_id UUID NOT NULL,
    sample_payload TEXT NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    last_modified_by VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,

    -- Foreign key to integration_connections
    CONSTRAINT fk_jira_webhooks_connection
        FOREIGN KEY (connection_id) REFERENCES integration_connections(id) ON DELETE RESTRICT,
    -- Foreign key to tenant_profiles
    CONSTRAINT fk_jira_webhooks_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant_profiles(tenant_id) ON DELETE RESTRICT,

    -- Composite FK constraints for user references
    CONSTRAINT fk_jira_webhooks_created_user
        FOREIGN KEY (created_by, tenant_id) REFERENCES user_profiles(keycloak_user_id, tenant_id) ON DELETE RESTRICT,
    CONSTRAINT fk_jira_webhooks_updated_user
        FOREIGN KEY (last_modified_by, tenant_id) REFERENCES user_profiles(keycloak_user_id, tenant_id) ON DELETE RESTRICT
);

-- Jira webhooks indexes (frequently accessed)
CREATE UNIQUE INDEX uq_jira_webhooks_tenant_name_active ON jira_webhooks (tenant_id, normalized_name) WHERE is_deleted = false;
CREATE INDEX idx_jira_webhooks_tenant_id ON jira_webhooks(tenant_id, is_deleted);

-- ===========================================================================
-- TABLE: jira_field_mappings
-- ===========================================================================
CREATE TABLE jira_field_mappings (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    jira_webhook_id VARCHAR(100) NOT NULL,
    jira_field_id VARCHAR(100) NOT NULL,
    jira_field_name VARCHAR(255),
    display_label VARCHAR(255),
    data_type VARCHAR(50) NOT NULL CHECK (data_type IN ('STRING', 'NUMBER', 'DATE', 'ARRAY', 'OBJECT', 'BOOLEAN', 'URL', 'EMAIL', 'USER', 'MULTIUSER')),
    template TEXT,
    required BOOLEAN NOT NULL DEFAULT false,
    default_value VARCHAR(500),
    metadata JSONB,

    -- Foreign key to jira_webhooks
    CONSTRAINT fk_jira_field_mappings_webhook
        FOREIGN KEY (jira_webhook_id) REFERENCES jira_webhooks(id) ON DELETE RESTRICT
);
-- Jira field mappings indexes
CREATE INDEX idx_jira_field_mappings_webhook ON jira_field_mappings(jira_webhook_id);

-- ===========================================================================
-- TABLE: jira_webhook_events
-- ===========================================================================
CREATE TABLE jira_webhook_events (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    triggered_by VARCHAR(255) NOT NULL,
    triggered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    webhook_id VARCHAR(100) NOT NULL,
    incoming_payload TEXT,
    transformed_payload TEXT,
    response_status_code INTEGER,
    response_body TEXT,
    trigger_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (trigger_status IN ('PENDING', 'SUCCESS', 'FAILED', 'RETRYING')),
    error_message TEXT,
    retry_attempt INTEGER NOT NULL DEFAULT 0,
    original_event_id VARCHAR(100),
    jira_issue_url VARCHAR(500),
    tenant_id VARCHAR(100) NOT NULL
);

-- Webhook events indexes (high-volume table)
CREATE INDEX idx_webhook_events_webhook_id ON jira_webhook_events(webhook_id);
CREATE INDEX idx_webhook_events_original_event ON jira_webhook_events(original_event_id) WHERE original_event_id IS NOT NULL;
CREATE INDEX idx_webhook_events_triggered_at ON jira_webhook_events(triggered_at);
CREATE INDEX idx_webhook_events_tenant_id ON jira_webhook_events(tenant_id);
CREATE INDEX idx_webhook_events_webhook_tenant ON jira_webhook_events(webhook_id, tenant_id);
-- Composite index to support LEFT JOIN LATERAL ... ORDER BY triggered_at DESC LIMIT 1
CREATE INDEX idx_webhook_events_webhook_triggered ON jira_webhook_events (webhook_id, triggered_at DESC);

-- ===========================================================================
-- TABLE: arcgis_integrations
-- ===========================================================================
CREATE TABLE arcgis_integrations (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    normalized_name VARCHAR(100) NOT NULL,
    description TEXT,
    item_type VARCHAR(100) NOT NULL,
    item_subtype VARCHAR(100) NOT NULL,
    dynamic_document_type VARCHAR(100),
    connection_id UUID NOT NULL,
    schedule_id UUID NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    last_modified_by VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_enabled BOOLEAN DEFAULT false,
    is_deleted BOOLEAN NOT NULL DEFAULT false,

    -- Foreign key to integration_connections
    CONSTRAINT fk_arcgis_integrations_connection
        FOREIGN KEY (connection_id) REFERENCES integration_connections(id) ON DELETE RESTRICT,

    -- Foreign key to integration_schedules
    CONSTRAINT fk_arcgis_integrations_schedule
        FOREIGN KEY (schedule_id) REFERENCES integration_schedules(id) ON DELETE RESTRICT,

    -- Foreign key to tenant_profiles
    CONSTRAINT fk_arcgis_integrations_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant_profiles(tenant_id) ON DELETE RESTRICT,

    -- Composite FK constraints for user references
    CONSTRAINT fk_arcgis_integrations_created_user
        FOREIGN KEY (created_by, tenant_id) REFERENCES user_profiles(keycloak_user_id, tenant_id) ON DELETE RESTRICT,
    CONSTRAINT fk_arcgis_integrations_updated_user
        FOREIGN KEY (last_modified_by, tenant_id) REFERENCES user_profiles(keycloak_user_id, tenant_id) ON DELETE RESTRICT
);

-- ArcGIS integrations indexes
CREATE UNIQUE INDEX uq_arcgis_tenant_name_active ON arcgis_integrations (tenant_id, normalized_name) WHERE is_deleted = false;
CREATE INDEX idx_arcgis_integrations_tenant_id ON arcgis_integrations(tenant_id, is_deleted);
CREATE INDEX idx_arcgis_integrations_schedule_id ON arcgis_integrations(schedule_id) WHERE schedule_id IS NOT NULL;

-- ===========================================================================
-- TABLE: integration_field_mappings
-- ===========================================================================
CREATE TABLE integration_field_mappings (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    integration_id UUID NOT NULL,
    source_field_path VARCHAR(100) NOT NULL,
    target_field_path VARCHAR(100) NOT NULL,
    transformation_type VARCHAR(50) NOT NULL,
    transformation_config JSONB,
    is_mandatory BOOLEAN NOT NULL DEFAULT false,
    default_value TEXT,
    display_order INTEGER,

    -- Foreign key to arcgis_integrations
    CONSTRAINT fk_integration_field_mappings_integration
        FOREIGN KEY (integration_id) REFERENCES arcgis_integrations(id) ON DELETE RESTRICT
);

-- ===========================================================================
-- TABLE: integration_job_executions
-- ===========================================================================
CREATE TABLE integration_job_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id UUID NOT NULL,
    triggered_by VARCHAR(20) NOT NULL CHECK (triggered_by IN ('SCHEDULER', 'RETRY', 'API')),
    triggered_by_user VARCHAR(255),
    window_start TIMESTAMP WITH TIME ZONE,
    window_end   TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SCHEDULED', 'RUNNING', 'SUCCESS', 'FAILED', 'ABORTED')),
    started_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    added_records INTEGER NOT NULL DEFAULT 0,
    updated_records INTEGER NOT NULL DEFAULT 0,
    failed_records INTEGER NOT NULL DEFAULT 0,
    total_records INTEGER NOT NULL DEFAULT 0,
    added_records_metadata JSONB,
    updated_records_metadata JSONB,
    failed_records_metadata JSONB,
    total_records_metadata JSONB,
    error_message TEXT,
    -- Retry lineage tracking (KIP-437)
    original_job_id UUID,
    retry_attempt INT NOT NULL DEFAULT 0,

    -- Foreign key to integration_schedules
    CONSTRAINT fk_integration_job_executions_schedule
        FOREIGN KEY (schedule_id) REFERENCES integration_schedules(id) ON DELETE RESTRICT
);

-- Integration job execution indexes (heavy read/write table)
CREATE INDEX idx_integration_job_executions_schedule_id ON integration_job_executions(schedule_id);
CREATE INDEX idx_integration_job_executions_started_at ON integration_job_executions(started_at);
-- Composite index to support LEFT JOIN LATERAL ... ORDER BY started_at DESC LIMIT 1
CREATE INDEX idx_job_executions_schedule_started ON integration_job_executions (schedule_id, started_at DESC);
-- Retry lineage indexes (KIP-437)
CREATE INDEX idx_original_job_id ON integration_job_executions(original_job_id);
-- Partial unique index to enforce one retry attempt per lineage
-- Only applies to rows where original_job_id is NOT NULL
CREATE UNIQUE INDEX unique_retry_per_job ON integration_job_executions(original_job_id, retry_attempt) WHERE original_job_id IS NOT NULL;
