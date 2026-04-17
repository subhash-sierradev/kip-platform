SET search_path TO integration_platform;

-- ===========================================================================
-- Insert Master Data: Credential Types and Site Configurations
-- ===========================================================================

INSERT INTO credential_types (credential_auth_type, display_name, is_enabled, required_fields) VALUES
('BASIC_AUTH', 'Basic Auth', true, '["username", "password"]'::jsonb),
('OAUTH2', 'OAuth 2.0', true, '["clientId", "clientSecret", "tokenUrl", "scope"]'::jsonb)
ON CONFLICT (credential_auth_type) DO NOTHING;

-- ===========================================================================
-- Insert System Profiles (GLOBAL tenant and system user)
-- ===========================================================================

-- Insert GLOBAL tenant profile for system-level operations
INSERT INTO tenant_profiles (tenant_id, tenant_name, created_date) 
VALUES 
    ('GLOBAL','Global System Tenant',CURRENT_TIMESTAMP)
ON CONFLICT (tenant_id) DO NOTHING;

-- Insert system user profile under GLOBAL tenant for automated operations
INSERT INTO user_profiles (keycloak_user_id, email, display_name, tenant_id, created_date)
VALUES ('system','system@integration-platform.local','System User','GLOBAL',CURRENT_TIMESTAMP)
ON CONFLICT (keycloak_user_id, tenant_id) DO NOTHING;

-- ===========================================================================
-- Insert Global Site Configurations
-- ===========================================================================

-- Insert global site configuration with tenant_id = 'GLOBAL'
-- Uses ON CONFLICT on config_key only since GLOBAL configs have unique constraint on config_key alone
INSERT INTO site_configs (config_key, config_value, value_type, description, tenant_id, created_by, last_modified_by, version, is_deleted, created_date, last_modified_date)
VALUES 
    ('AUTO_LOGOUT_TIME','15','NUMBER','Auto logout time in minutes','GLOBAL','system','system',0,false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    ('ARCGIS_INITIAL_SYNC_START_TIMESTAMP','2025-01-01T00:00:00Z','TIMESTAMP','Initial sync start timestamp for ArcGIS integrations on first execution','GLOBAL','system','system',0,false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    ('CONFLUENCE_INITIAL_SYNC_START_TIMESTAMP','2025-01-01T00:00:00Z','TIMESTAMP','Initial sync start timestamp for Confluence integrations on first execution','GLOBAL','system','system',0,false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);

-- ===========================================================================
-- Insert Master Data: Languages
-- ===========================================================================
INSERT INTO integration_platform.languages (code, name, native_name, sort_order) VALUES
    ('en', 'English',  'English',   1),
    ('fr', 'French',   'Français',  2),
    ('de', 'German',   'Deutsch',   3),
    ('ja', 'Japanese', '日本語',    4),
    ('es', 'Spanish',  'Español',   5)
ON CONFLICT (code) DO NOTHING;

