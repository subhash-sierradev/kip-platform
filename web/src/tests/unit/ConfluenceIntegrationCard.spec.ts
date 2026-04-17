import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import type { ConfluenceIntegrationSummaryResponse } from '@/api/models/ConfluenceIntegrationSummaryResponse';
import IntegrationStatusIcon from '@/components/common/IntegrationStatusIcon.vue';
import ConfluenceIntegrationCard from '@/components/outbound/confluenceintegration/ConfluenceIntegrationCard.vue';

function makeIntegration(
  overrides: Partial<ConfluenceIntegrationSummaryResponse> = {}
): ConfluenceIntegrationSummaryResponse {
  return {
    id: 'ci-1',
    name: 'My Confluence Integration',
    itemType: 'DOCUMENT',
    itemSubtype: 'DESIGN',
    itemSubtypeLabel: 'Design',
    dynamicDocumentType: null,
    dynamicDocumentTypeLabel: null,
    languageCodes: ['en', 'fr'],
    confluenceSpaceKey: 'TEST',
    reportNameTemplate: 'Report - {date}',
    frequencyPattern: 'DAILY',
    executionTime: '09:00',
    createdDate: '2026-01-01T00:00:00Z',
    createdBy: 'admin',
    lastModifiedDate: '2026-01-02T00:00:00Z',
    lastModifiedBy: 'admin',
    isEnabled: true,
    ...overrides,
  };
}

describe('ConfluenceIntegrationCard', () => {
  it('renders the integration name', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration(), menuItems: [] },
    });
    expect(wrapper.text()).toContain('My Confluence Integration');
  });

  it('renders "Unnamed Integration" when name is empty', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration({ name: '' }), menuItems: [] },
    });
    expect(wrapper.text()).toContain('Unnamed Integration');
  });

  it('shows enabled chip when integration is enabled', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration({ isEnabled: true }), menuItems: [] },
    });
    expect(wrapper.find('.status-active').exists()).toBe(true);
    expect(wrapper.find('.status-active').text()).toBe('Enabled');
  });

  it('shows disabled chip when integration is disabled', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration({ isEnabled: false }), menuItems: [] },
    });
    expect(wrapper.find('.status-disabled').exists()).toBe(true);
    expect(wrapper.find('.status-disabled').text()).toBe('Disabled');
  });

  it('shows environment chip with itemSubtypeLabel', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration({ itemSubtypeLabel: 'Design Docs' }), menuItems: [] },
    });
    expect(wrapper.find('.environment-chip').text()).toContain('Design Docs');
  });

  it('falls back to itemSubtype when label is absent', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: {
        integration: makeIntegration({ itemSubtypeLabel: null, itemSubtype: 'DESIGN' }),
        menuItems: [],
      },
    });
    expect(wrapper.find('.environment-chip').text()).toContain('DESIGN');
  });

  it('shows dynamic document chip when dynamicDocumentType is set', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: {
        integration: makeIntegration({
          dynamicDocumentType: 'doc-123',
          dynamicDocumentTypeLabel: 'My Doc Type',
        }),
        menuItems: [],
      },
    });
    expect(wrapper.find('.dynamic-doc-chip').exists()).toBe(true);
    expect(wrapper.find('.dynamic-doc-chip').text()).toContain('My Doc Type');
  });

  it('shows raw dynamicDocumentType when label is null', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: {
        integration: makeIntegration({
          dynamicDocumentType: 'raw-type',
          dynamicDocumentTypeLabel: null,
        }),
        menuItems: [],
      },
    });
    expect(wrapper.find('.dynamic-doc-chip').text()).toContain('raw-type');
  });

  it('hides dynamic doc chip when dynamicDocumentType is empty', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration({ dynamicDocumentType: '' }), menuItems: [] },
    });
    expect(wrapper.find('.dynamic-doc-chip').exists()).toBe(false);
  });

  it('hides dynamic doc chip when dynamicDocumentType is whitespace only', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration({ dynamicDocumentType: '   ' }), menuItems: [] },
    });
    expect(wrapper.find('.dynamic-doc-chip').exists()).toBe(false);
  });

  it('hides dynamic doc chip when dynamicDocumentType is null', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration({ dynamicDocumentType: null }), menuItems: [] },
    });
    expect(wrapper.find('.dynamic-doc-chip').exists()).toBe(false);
  });

  it('shows language chip for up to 3 languages', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration({ languageCodes: ['en', 'fr', 'de'] }), menuItems: [] },
    });
    const chipText = wrapper.findAll('.environment-chip')[1]?.text() ?? '';
    expect(chipText).toContain('EN');
    expect(chipText).toContain('FR');
    expect(chipText).toContain('DE');
  });

  it('shows "+N more" suffix for more than 3 languages', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: {
        integration: makeIntegration({ languageCodes: ['en', 'fr', 'de', 'es', 'it'] }),
        menuItems: [],
      },
    });
    const chipText = wrapper.findAll('.environment-chip')[1]?.text() ?? '';
    expect(chipText).toContain('+2 more');
  });

  it('hides language chip when no language codes', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration({ languageCodes: [] }), menuItems: [] },
    });
    // Only the itemSubtype chip should appear
    expect(wrapper.findAll('.environment-chip')).toHaveLength(1);
  });

  it('shows "Not scheduled" when executionTime is missing', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: {
        integration: makeIntegration({ executionTime: '' }),
        menuItems: [],
      },
    });
    expect(wrapper.text()).toContain('Not scheduled');
  });

  it('shows "Daily at ..." when executionTime is provided', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration({ executionTime: '10:00' }), menuItems: [] },
    });
    expect(wrapper.text()).toContain('Daily at');
  });

  it('shows "Never Run" when lastAttemptTimeUtc is missing', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration({ lastAttemptTimeUtc: undefined }), menuItems: [] },
    });
    expect(wrapper.text()).toContain('Never Run');
  });

  it('shows formatted last run date when lastAttemptTimeUtc exists', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: {
        integration: makeIntegration({ lastAttemptTimeUtc: '2026-01-15T10:00:00Z' }),
        menuItems: [],
      },
    });
    expect(wrapper.text()).toContain('2026');
  });

  it('shows "Not scheduled" when nextRunAtUtc is missing', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration({ nextRunAtUtc: undefined }), menuItems: [] },
    });
    const cells = wrapper.findAll('.info-cell');
    const nextRunCell = cells.find(c => c.text().includes('Next Run:'));
    expect(nextRunCell?.text()).toContain('Not scheduled');
  });

  it('shows formatted next run date when nextRunAtUtc exists', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: {
        integration: makeIntegration({ nextRunAtUtc: '2026-02-01T08:00:00Z' }),
        menuItems: [],
      },
    });
    expect(wrapper.text()).toContain('2026');
  });

  it.each([
    ['SUCCESS', 'dx-icon-check'],
    ['FAILED', 'dx-icon-close'],
    ['RUNNING', 'dx-icon-refresh'],
    ['ABORTED', 'dx-icon-remove'],
    ['SCHEDULED', 'dx-icon-clock'],
  ])('shows %s status icon', (status, expectedClass) => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: {
        integration: makeIntegration({ lastStatus: status as any }),
        menuItems: [],
      },
    });
    expect(wrapper.find(`.${expectedClass}`).exists()).toBe(true);
  });

  it('emits "open" event with integration id when card is clicked', async () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration({ id: 'ci-42' }), menuItems: [] },
    });
    await wrapper.find('.integration-card').trigger('click');
    expect(wrapper.emitted('open')).toBeTruthy();
    expect(wrapper.emitted('open')![0]).toEqual(['ci-42']);
  });

  it('emits "action" event when ActionMenu action is triggered', async () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration(), menuItems: [{ id: 'edit', label: 'Edit' }] },
    });
    const actionMenu = wrapper.findComponent({ name: 'ActionMenu' });
    actionMenu.vm.$emit('action', 'edit');
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted('action')).toBeTruthy();
    expect(wrapper.emitted('action')![0]).toEqual(['edit']);
  });

  it('shows status tooltip on mouseenter', async () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: {
        integration: makeIntegration({ lastStatus: 'SUCCESS' }),
        menuItems: [],
      },
    });
    const icon = wrapper.find('.dx-icon-check');
    await icon.trigger('mouseenter');
    const iconComponent = wrapper.findComponent(IntegrationStatusIcon);
    expect((iconComponent.vm as any).tooltipVisible).toBe(true);
  });

  it('hides status tooltip on mouseleave', async () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration({ lastStatus: 'SUCCESS' }), menuItems: [] },
    });
    const icon = wrapper.find('.dx-icon-check');
    await icon.trigger('mouseenter');
    await icon.trigger('mouseleave');
    const iconComponent = wrapper.findComponent(IntegrationStatusIcon);
    expect((iconComponent.vm as any).tooltipVisible).toBe(false);
  });

  it('no status icon when lastStatus is undefined', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration({ lastStatus: undefined }), menuItems: [] },
    });
    expect(wrapper.find('[class*="dx-icon-check"]').exists()).toBe(false);
    expect(wrapper.find('[class*="dx-icon-close"]').exists()).toBe(false);
  });

  it('renders Updated label in card view', () => {
    const wrapper = mount(ConfluenceIntegrationCard, {
      props: { integration: makeIntegration(), menuItems: [] },
    });
    expect(wrapper.text()).toContain('Updated:');
  });
});
