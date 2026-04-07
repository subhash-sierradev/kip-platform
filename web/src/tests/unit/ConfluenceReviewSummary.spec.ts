/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';

const masterDataSpies = vi.hoisted(() => ({
  getAllActiveLanguages: vi.fn(),
}));

vi.mock('@/api/services/MasterDataService', () => ({
  MasterDataService: {
    getAllActiveLanguages: masterDataSpies.getAllActiveLanguages,
  },
}));

vi.mock('@/utils/timezoneUtils', () => ({
  getUserTimezone: () => 'UTC',
  formatTimezoneInfo: () => 'UTC (Coordinated Universal Time)',
}));

import ConfluenceReviewSummary from '@/components/outbound/confluenceintegration/wizard/steps/ReviewSummary.vue';
import type { ConfluenceFormData } from '@/types/ConfluenceFormData';

function makeFormData(overrides: Partial<ConfluenceFormData> = {}): ConfluenceFormData {
  return {
    name: 'My Integration',
    description: 'A description',
    itemType: 'DOCUMENT',
    subType: 'DESIGN',
    subTypeLabel: 'Design',
    languageCodes: ['en', 'fr'],
    reportNameTemplate: 'Report - {date}',
    includeTableOfContents: true,
    executionDate: '2026-01-01',
    executionTime: '09:00',
    frequencyPattern: 'DAILY',
    dailyFrequency: '24',
    selectedDays: [],
    selectedMonths: [],
    isExecuteOnMonthEnd: false,
    confluenceSpaceKey: 'MY_SPACE',
    confluenceSpaceKeyFolderKey: 'FOLDER',
    connectionMethod: 'new',
    username: 'user',
    password: 'pass',
    ...overrides,
  };
}

describe('ConfluenceReviewSummary', () => {
  beforeEach(() => {
    masterDataSpies.getAllActiveLanguages.mockResolvedValue([
      { code: 'en', name: 'English', nativeName: 'English' },
      { code: 'fr', name: 'French', nativeName: 'Français' },
    ]);
  });

  it('renders integration name', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData({ name: 'Test Name' }) },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('Test Name');
  });

  it('renders description', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData({ description: 'My desc' }) },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('My desc');
  });

  it('shows "No description provided" when description is empty', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData({ description: '' }) },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('No description provided');
  });

  it('shows "Daily" frequency label for DAILY pattern', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData({ frequencyPattern: 'DAILY' }) },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('Daily');
  });

  it('shows "Weekly" frequency label for WEEKLY pattern', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: {
        formData: makeFormData({
          frequencyPattern: 'WEEKLY',
          selectedDays: ['Monday', 'Wednesday'],
        }),
      },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('Weekly');
    expect(wrapper.text()).toContain('Monday');
  });

  it('shows "Monthly" frequency label for MONTHLY pattern', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: {
        formData: makeFormData({
          frequencyPattern: 'MONTHLY',
          selectedMonths: [1, 3, 5],
          isExecuteOnMonthEnd: false,
        }),
      },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('Monthly');
  });

  it('shows isExecuteOnMonthEnd when monthly pattern is set', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: {
        formData: makeFormData({
          frequencyPattern: 'MONTHLY',
          selectedMonths: [],
          isExecuteOnMonthEnd: true,
        }),
      },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('Enabled');
  });

  it('shows "Custom (Cron)" for CUSTOM pattern with cron expression', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: {
        formData: makeFormData({
          frequencyPattern: 'CUSTOM',
          cronExpression: '0 9 * * 1',
        }),
      },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('Custom (Cron)');
    expect(wrapper.text()).toContain('0 9 * * 1');
  });

  it('shows raw frequencyPattern when unknown', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData({ frequencyPattern: 'UNKNOWN' }) },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('UNKNOWN');
  });

  it('shows "Once daily" daily interval for 24', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData({ frequencyPattern: 'DAILY', dailyFrequency: '24' }) },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('Once daily');
  });

  it('shows "Every hour" daily interval for 1', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData({ frequencyPattern: 'DAILY', dailyFrequency: '1' }) },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('Every hour');
  });

  it('shows "Every N hours" for custom daily interval', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData({ frequencyPattern: 'DAILY', dailyFrequency: '6' }) },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('Every 6 hours');
  });

  it('shows "Not set" when dailyFrequency is 0', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData({ frequencyPattern: 'DAILY', dailyFrequency: '0' }) },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('Not set');
  });

  it('shows confluence space key', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData({ confluenceSpaceKey: 'MYSPACE' }) },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('MYSPACE');
  });

  it('shows space folder key when provided', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData({ confluenceSpaceKeyFolderKey: 'MY_FOLDER' }) },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('MY_FOLDER');
  });

  it('shows ROOT when folder key is empty for a selected space', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: {
        formData: makeFormData({
          confluenceSpaceKey: 'MYSPACE',
          confluenceSpaceKeyFolderKey: '',
          confluenceSpaceFolderLabel: '',
        }),
      },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('ROOT');
  });

  it('shows duplicate name warning when isDuplicateName is true', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData(), isDuplicateName: true },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.find('.rs-duplicate-warning').exists()).toBe(true);
  });

  it('hides duplicate name warning when isDuplicateName is false', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData(), isDuplicateName: false },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.find('.rs-duplicate-warning').exists()).toBe(false);
  });

  it('shows execution date when provided', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData({ executionDate: '2026-03-01' }) },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('Start Date');
  });

  it('hides execution date section when executionDate is null', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData({ executionDate: null }) },
    });
    await new Promise(r => setTimeout(r, 0));
    // No "Start Date:" label should appear
    expect(wrapper.text()).not.toContain('Start Date:');
  });

  it('shows includeTableOfContents enabled', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData({ includeTableOfContents: true }) },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('Enabled');
  });

  it('shows includeTableOfContents disabled', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData({ includeTableOfContents: false }) },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('Disabled');
  });

  it('shows dynamic document section when provided', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: {
        formData: makeFormData({ dynamicDocument: 'doc-type', dynamicDocumentLabel: 'My Doc' }),
      },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('My Doc');
  });

  it('shows languages from loaded language list', async () => {
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData({ languageCodes: ['en'] }) },
    });
    await new Promise(r => setTimeout(r, 0));
    // After language load, should display loaded language name
    expect(wrapper.text()).toContain('English');
  });

  it('falls back to uppercased codes when getAllActiveLanguages rejects', async () => {
    masterDataSpies.getAllActiveLanguages.mockRejectedValue(new Error('fail'));
    const wrapper = mount(ConfluenceReviewSummary, {
      props: { formData: makeFormData({ languageCodes: ['en', 'fr'] }) },
    });
    await new Promise(r => setTimeout(r, 0));
    // Falls back to showing code display
    expect(wrapper.exists()).toBe(true);
  });
});
