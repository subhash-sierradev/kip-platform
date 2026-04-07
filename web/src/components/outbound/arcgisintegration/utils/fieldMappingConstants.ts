/**
 * Constants and utilities for ArcGIS Integration field mapping
 * Centralizes default mandatory mapping configuration
 */

/** Default mandatory source field for ArcGIS location identification */
export const DEFAULT_SOURCE_FIELD = 'id';

/** Default mandatory target field for ArcGIS location identification */
export const DEFAULT_TARGET_FIELD = 'external_location_id';

/**
 * Checks if a field mapping is the mandatory default mapping
 * (id -> external_location_id)
 *
 * @param mapping - Field mapping object to check
 * @returns true if this is the default mandatory mapping
 */
export function isDefaultMapping(mapping: { sourceField?: string; targetField?: string }): boolean {
  return (
    mapping.sourceField === DEFAULT_SOURCE_FIELD && mapping.targetField === DEFAULT_TARGET_FIELD
  );
}
