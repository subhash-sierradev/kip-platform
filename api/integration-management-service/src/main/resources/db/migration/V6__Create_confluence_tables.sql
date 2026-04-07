-- ===========================================================================
-- FLYWAY V6 MIGRATION: Create Confluence Integration Tables
-- ===========================================================================

SET search_path TO integration_platform;

-- ---------------------------------------------------------------------------
-- languages  (master table — future languages = INSERT only, no code change)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS integration_platform.languages (
    code        VARCHAR(10)  PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    native_name VARCHAR(100) NOT NULL,
    is_enabled  BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order  INT          NOT NULL DEFAULT 0
);

INSERT INTO integration_platform.languages (code, name, native_name, sort_order) VALUES
    ('en', 'English',  'English',   1),
    ('fr', 'French',   'Français',  2),
    ('de', 'German',   'Deutsch',   3),
    ('ja', 'Japanese', '日本語',    4),
    ('es', 'Spanish',  'Español',   5)
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- confluence_integrations
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS integration_platform.confluence_integrations (
    id                            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                          VARCHAR(100) NOT NULL,
    normalized_name               VARCHAR(100) NOT NULL,
    description                   VARCHAR(500),
    -- Document selector
    document_item_type            VARCHAR(100) NOT NULL DEFAULT 'DOCUMENT',
    document_item_subtype         VARCHAR(100) NOT NULL,
    dynamic_document_type         VARCHAR(100),
    -- Page config
    report_name_template          VARCHAR(255) NOT NULL,
    confluence_space_key          VARCHAR(100) NOT NULL,
    confluence_space_folder_key   VARCHAR(100) NOT NULL,
    include_table_of_contents     BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Relations
    connection_id                 UUID         NOT NULL REFERENCES integration_platform.integration_connections(id),
    schedule_id                   UUID         UNIQUE   REFERENCES integration_platform.integration_schedules(id),
    -- State / audit
    is_enabled                    BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted                    BOOLEAN      NOT NULL DEFAULT FALSE,
    tenant_id                     VARCHAR(100) NOT NULL REFERENCES integration_platform.tenant_profiles(tenant_id),
    created_by                    VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_by              VARCHAR(255) NOT NULL DEFAULT 'system',
    created_date                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modified_date            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version                       BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_confluence_tenant_name_active
    ON integration_platform.confluence_integrations(tenant_id, normalized_name)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_confluence_integrations_tenant_id
    ON integration_platform.confluence_integrations(tenant_id, is_deleted);

CREATE INDEX IF NOT EXISTS idx_confluence_integrations_schedule_id
    ON integration_platform.confluence_integrations(schedule_id)
    WHERE schedule_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- confluence_integration_languages  (join table)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS integration_platform.confluence_integration_languages (
    integration_id UUID        NOT NULL REFERENCES integration_platform.confluence_integrations(id) ON DELETE CASCADE,
    language_code  VARCHAR(10) NOT NULL REFERENCES integration_platform.languages(code),
    PRIMARY KEY (integration_id, language_code)
);

CREATE INDEX IF NOT EXISTS idx_conf_int_languages_integration
    ON integration_platform.confluence_integration_languages(integration_id);

-- ---------------------------------------------------------------------------
-- integration_job_executions — add execution_metadata column (safe for existing rows)
-- ---------------------------------------------------------------------------
ALTER TABLE integration_platform.integration_job_executions
    ADD COLUMN IF NOT EXISTS execution_metadata JSONB;
