/**
 * Enum representing tenant types for site configuration management.
 * This enum provides type-safe constants for tenant identification,
 * eliminating the need for null checks and making the code more maintainable.
 *
 * Matches the backend TenantType enum for consistency.
 */
export enum TenantType {
  /**
   * Represents global configurations that apply to all tenants as defaults.
   * Global configurations serve as fallback values when no tenant-specific
   * configuration exists.
   */
  GLOBAL = 'GLOBAL',
}

/**
 * Utility functions for TenantType enum
 */
export class TenantTypeUtil {
  /**
   * Checks if the given tenant ID represents a global configuration.
   *
   * @param tenantId The tenant ID to check
   * @returns true if the tenant ID represents a global configuration, false otherwise
   */
  static isGlobal(tenantId?: string | null): boolean {
    return tenantId === TenantType.GLOBAL;
  }

  /**
   * Returns the global tenant identifier.
   *
   * @returns The global tenant identifier string
   */
  static getGlobalValue(): string {
    return TenantType.GLOBAL;
  }
}
