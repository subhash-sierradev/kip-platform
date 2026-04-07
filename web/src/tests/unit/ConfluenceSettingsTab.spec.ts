import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import type { ConfluenceIntegrationResponse } from '@/api/models/ConfluenceIntegrationResponse';
import ConfluenceSettingsTab from '@/components/outbound/confluenceintegration/details/ConfluenceSettingsTab.vue';

function makeResponse(
  overrides: Partial<ConfluenceIntegrationResponse> = {}
): ConfluenceIntegrationResponse {
  return {
    id: 'ci-1',
    name: 'My Integration',
    confluenceSpaceKey: 'MY_SPACE',
    confluenceSpaceKeyFolderKey: 'FOLDER_KEY',
    reportNameTemplate: 'Report - {date}',
    includeTableOfContents: true,
    languageCodes: ['en', 'fr'],
    languages: [],
    isEnabled: true,
    itemType: 'DOCUMENT',
    itemSubtype: 'DESIGN',
    createdDate: '2026-01-01T00:00:00Z',
    createdBy: 'admin',
    lastModifiedDate: '2026-01-02T00:00:00Z',
    lastModifiedBy: 'admin',
    ...overrides,
  } as ConfluenceIntegrationResponse;
}

describe('ConfluenceSettingsTab', () => {
  it('renders space label when provided in integrationData', () => {
    const wrapper = mount(ConfluenceSettingsTab, {
      props: {
        integrationData: makeResponse({ confluenceSpaceLabel: 'Space A (SPACE_A)' }),
        loading: false,
      },
    });

    expect(wrapper.text()).toContain('Space A (SPACE_A)');
  });

  it('falls back to space key when space label is missing', () => {
    const wrapper = mount(ConfluenceSettingsTab, {
      props: {
        integrationData: makeResponse({ confluenceSpaceLabel: '' }),
        loading: false,
      },
    });

    expect(wrapper.text()).toContain('MY_SPACE');
  });

  it('renders folder label when provided in integrationData', () => {
    const wrapper = mount(ConfluenceSettingsTab, {
      props: {
        integrationData: makeResponse({ confluenceSpaceFolderLabel: 'Parent A > Folder A' }),
        loading: false,
      },
    });

    expect(wrapper.text()).toContain('Parent A > Folder A');
  });

  it('falls back to folder key when folder label is missing', () => {
    const wrapper = mount(ConfluenceSettingsTab, {
      props: {
        integrationData: makeResponse({
          confluenceSpaceFolderLabel: '',
          confluenceSpaceKeyFolderKey: 'FOLDER_A',
        }),
        loading: false,
      },
    });

    expect(wrapper.text()).toContain('FOLDER_A');
  });

  it('shows Root Folder for ROOT sentinel folder key', () => {
    const wrapper = mount(ConfluenceSettingsTab, {
      props: {
        integrationData: makeResponse({
          confluenceSpaceKey: 'SPACE_A',
          confluenceSpaceKeyFolderKey: 'ROOT',
          confluenceSpaceFolderLabel: '',
        }),
        loading: false,
      },
    });

    expect(wrapper.text()).toContain('Root Folder');
  });

  it('renders report name template', () => {
    const wrapper = mount(ConfluenceSettingsTab, {
      props: { integrationData: makeResponse(), loading: false },
    });

    expect(wrapper.text()).toContain('Report - {date}');
  });

  it('shows Enabled for includeTableOfContents when true', () => {
    const wrapper = mount(ConfluenceSettingsTab, {
      props: { integrationData: makeResponse({ includeTableOfContents: true }), loading: false },
    });

    expect(wrapper.text()).toContain('Enabled');
  });

  it('shows Disabled for includeTableOfContents when false', () => {
    const wrapper = mount(ConfluenceSettingsTab, {
      props: { integrationData: makeResponse({ includeTableOfContents: false }), loading: false },
    });

    expect(wrapper.text()).toContain('Disabled');
  });

  it('shows language names when languages array is populated', () => {
    const wrapper = mount(ConfluenceSettingsTab, {
      props: {
        integrationData: makeResponse({
          languages: [
            { name: 'English', nativeName: 'English', code: 'en' },
            { name: 'French', nativeName: 'Français', code: 'fr' },
          ],
        }),
        loading: false,
      },
    });

    expect(wrapper.text()).toContain('English');
    expect(wrapper.text()).toContain('French');
  });

  it('falls back to languageCodes when languages array is empty', () => {
    const wrapper = mount(ConfluenceSettingsTab, {
      props: {
        integrationData: makeResponse({ languages: [], languageCodes: ['en', 'de'] }),
        loading: false,
      },
    });

    expect(wrapper.text()).toContain('EN');
    expect(wrapper.text()).toContain('DE');
  });

  it('shows no languages configured message when both languages and languageCodes are empty', () => {
    const wrapper = mount(ConfluenceSettingsTab, {
      props: {
        integrationData: makeResponse({ languages: [], languageCodes: [] }),
        loading: false,
      },
    });

    expect(wrapper.text()).toContain('No languages configured');
  });

  it('shows empty state when integrationData is null and not loading', () => {
    const wrapper = mount(ConfluenceSettingsTab, {
      props: { integrationData: null, loading: false },
    });

    expect(wrapper.find('.empty-state').exists()).toBe(true);
  });

  it('does not show empty state when loading', () => {
    const wrapper = mount(ConfluenceSettingsTab, {
      props: { integrationData: null, loading: true },
    });

    expect(wrapper.find('.empty-state').exists()).toBe(false);
  });
});
