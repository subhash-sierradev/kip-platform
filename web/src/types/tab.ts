import type { Component } from 'vue';

/**
 * Tab definition interface for navigation components
 */
export interface TabDefinition {
  /** Unique tab identifier */
  id: string;

  /** Display label for the tab */
  label: string;

  /** Component name, identifier, or Vue component for dynamic loading */
  component?: string | Component;

  /** Optional icon class for the tab */
  icon?: string;

  /** Optional DevExtreme icon class for the tab */
  iconClass?: string;

  /** Whether the tab is disabled */
  disabled?: boolean;
}
