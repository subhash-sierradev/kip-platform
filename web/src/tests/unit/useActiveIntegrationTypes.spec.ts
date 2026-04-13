/* eslint-disable simple-import-sort/imports */
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';

import { NotificationEntityType } from '@/api/models/NotificationEntityType';

const authState = { userRoles: [] as string[] };

vi.mock('@/store/auth', () => ({
  useAuthStore: () => authState,
}));

import { useActiveIntegrationTypes } from '@/composables/useActiveIntegrationTypes';

function mountComposable<T>(factory: () => T): T {
  let exposed!: T;
  const Comp = defineComponent({
    setup() {
      exposed = factory();
      return () => null;
    },
  });
  mount(Comp);
  return exposed;
}

describe('useActiveIntegrationTypes', () => {
  beforeEach(() => {
    authState.userRoles = [];
  });

  it('returns only SITE_CONFIG when user has no feature roles', () => {
    const { activeEntityTypes } = mountComposable(() => useActiveIntegrationTypes());
    expect(activeEntityTypes.value.size).toBe(1);
    expect(activeEntityTypes.value.has(NotificationEntityType.SITE_CONFIG)).toBe(true);
  });

  it('adds JIRA_WEBHOOK and INTEGRATION_CONNECTION when user has jira role', () => {
    authState.userRoles = ['feature_jira_webhook'];
    const { activeEntityTypes } = mountComposable(() => useActiveIntegrationTypes());
    expect(activeEntityTypes.value.has(NotificationEntityType.SITE_CONFIG)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.JIRA_WEBHOOK)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.INTEGRATION_CONNECTION)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.ARCGIS_INTEGRATION)).toBe(false);
  });

  it('adds ARCGIS_INTEGRATION and INTEGRATION_CONNECTION when user has arcgis role', () => {
    authState.userRoles = ['feature_arcgis_integration'];
    const { activeEntityTypes } = mountComposable(() => useActiveIntegrationTypes());
    expect(activeEntityTypes.value.has(NotificationEntityType.SITE_CONFIG)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.ARCGIS_INTEGRATION)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.INTEGRATION_CONNECTION)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.JIRA_WEBHOOK)).toBe(false);
  });

  it('adds all four types when user has both jira and arcgis roles', () => {
    authState.userRoles = ['feature_jira_webhook', 'feature_arcgis_integration'];
    const { activeEntityTypes } = mountComposable(() => useActiveIntegrationTypes());
    expect(activeEntityTypes.value.size).toBe(4);
    expect(activeEntityTypes.value.has(NotificationEntityType.SITE_CONFIG)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.JIRA_WEBHOOK)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.ARCGIS_INTEGRATION)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.INTEGRATION_CONNECTION)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.CONFLUENCE_INTEGRATION)).toBe(false);
  });

  it('adds CONFLUENCE_INTEGRATION and INTEGRATION_CONNECTION when user has confluence role only', () => {
    authState.userRoles = ['feature_confluence_integration'];
    const { activeEntityTypes } = mountComposable(() => useActiveIntegrationTypes());
    expect(activeEntityTypes.value.size).toBe(3);
    expect(activeEntityTypes.value.has(NotificationEntityType.SITE_CONFIG)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.CONFLUENCE_INTEGRATION)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.INTEGRATION_CONNECTION)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.JIRA_WEBHOOK)).toBe(false);
    expect(activeEntityTypes.value.has(NotificationEntityType.ARCGIS_INTEGRATION)).toBe(false);
  });

  it('adds CONFLUENCE_INTEGRATION and JIRA_WEBHOOK types when user has confluence and jira roles', () => {
    authState.userRoles = ['feature_confluence_integration', 'feature_jira_webhook'];
    const { activeEntityTypes } = mountComposable(() => useActiveIntegrationTypes());
    expect(activeEntityTypes.value.size).toBe(4);
    expect(activeEntityTypes.value.has(NotificationEntityType.SITE_CONFIG)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.CONFLUENCE_INTEGRATION)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.JIRA_WEBHOOK)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.INTEGRATION_CONNECTION)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.ARCGIS_INTEGRATION)).toBe(false);
  });

  it('adds all five types when user has all three feature roles', () => {
    authState.userRoles = [
      'feature_jira_webhook',
      'feature_arcgis_integration',
      'feature_confluence_integration',
    ];
    const { activeEntityTypes } = mountComposable(() => useActiveIntegrationTypes());
    expect(activeEntityTypes.value.size).toBe(5);
    expect(activeEntityTypes.value.has(NotificationEntityType.SITE_CONFIG)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.JIRA_WEBHOOK)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.ARCGIS_INTEGRATION)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.CONFLUENCE_INTEGRATION)).toBe(true);
    expect(activeEntityTypes.value.has(NotificationEntityType.INTEGRATION_CONNECTION)).toBe(true);
  });
});
