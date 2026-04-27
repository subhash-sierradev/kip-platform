-- ===========================================================================
-- V7: Add source_language column to confluence_integrations
-- ===========================================================================
-- Adds the BCP-47 source language of the FreeMarker report template.
-- Existing rows default to 'en' (English) which preserves backwards compatibility.
-- ===========================================================================

ALTER TABLE integration_platform.confluence_integrations
    ADD COLUMN IF NOT EXISTS source_language VARCHAR(10) NOT NULL DEFAULT 'en';

COMMENT ON COLUMN integration_platform.confluence_integrations.source_language
    IS 'BCP-47 source language of the FreeMarker report template (default: en). '
       'Used together with language_codes to decide whether the Translation API '
       'should be called before publishing the Confluence page.';

