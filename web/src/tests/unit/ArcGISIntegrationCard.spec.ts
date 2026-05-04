import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import type { ArcGISIntegrationSummaryResponse } from '@/api/models/ArcGISIntegrationSummaryResponse';
import ArcGISIntegrationCard from '@/components/outbound/arcgisintegration/ArcGISIntegrationCard.vue';
import type { ArcGISIntegrationExecutionSummary } from '@/types/ArcGISIntegrationExecution';

function makeBaseIntegration(): ArcGISIntegrationSummaryResponse {
  return {
    id: 'int-1',
    name: 'Test Integration',
    createdDate: '2026-01-01T12:00:00Z',
    lastModifiedDate: '2026-01-10T12:00:00Z',
    itemSubtype: 'ArcGIS',
    itemSubtypeLabel: 'ArcGIS',
    itemType: 'DOCUMENT',
    dynamicDocumentType: '',
    dynamicDocumentTypeLabel: null,
    isEnabled: true,
    // schedule fields for fallback
    frequencyPattern: 'DAILY',
    executionDate: '2026-01-20',
    executionTime: '09:00:00',
    daySchedule: undefined,
    monthSchedule: undefined,
    dailyExecutionInterval: 1,
    cronExpression: undefined,
  } as unknown as ArcGISIntegrationSummaryResponse;
}

describe('ArcGISIntegrationCard', () => {
  it('renders last and next run from nested execution fields', () => {
    const integration = makeBaseIntegration() as unknown as ArcGISIntegrationSummaryResponse &
      ArcGISIntegrationExecutionSummary;
    integration.execution = {
      lastSuccessTimeUtc: '2026-01-15T10:00:00Z',
      nextRunAtUtc: '2026-01-16T09:00:00Z',
      lastStatus: 'SUCCESS',
    };

    const wrapper = mount(ArcGISIntegrationCard, {
      props: { integration, menuItems: [] },
    });

    const text = wrapper.text();
    expect(text).toContain('Last Run:');
    expect(text).not.toContain('Last Run: Never');
    expect(text).toContain('Next Run:');
    // formatted strings should include year
    expect(text).toMatch(/2026/);
  });

  it('falls back to schedule next run when execution fields are missing', () => {
    const integration = makeBaseIntegration();

    const wrapper = mount(ArcGISIntegrationCard, {
      props: { integration, menuItems: [] },
    });

    const nextRunLabel = wrapper
      .findAll('.info-row .info-cell .label')
      .find(l => l.text() === 'Next Run:');
    expect(nextRunLabel).toBeTruthy();
    const nextRunValue = nextRunLabel!.element.parentElement!.querySelector('.value');
    expect(nextRunValue?.textContent).toBeTruthy();
  });

  it('displays dynamic document type label when provided', () => {
    const integration = makeBaseIntegration();
    integration.dynamicDocumentType = 'doc-123';
    integration.dynamicDocumentTypeLabel = 'Testing with PNG';

    const wrapper = mount(ArcGISIntegrationCard, {
      props: { integration, menuItems: [] },
    });

    // Should display as a chip in the chips-row
    const dynamicDocChip = wrapper.find('.dynamic-doc-chip');
    expect(dynamicDocChip.exists()).toBe(true);
    expect(dynamicDocChip.text()).toContain('Testing with PNG');
    expect(dynamicDocChip.text()).not.toContain('doc-123'); // Should show label, not raw value
  });

  it('displays dynamic document type raw value when label is null', () => {
    const integration = makeBaseIntegration();
    integration.dynamicDocumentType = 'doc-456';
    integration.dynamicDocumentTypeLabel = null;

    const wrapper = mount(ArcGISIntegrationCard, {
      props: { integration, menuItems: [] },
    });

    const dynamicDocChip = wrapper.find('.dynamic-doc-chip');
    expect(dynamicDocChip.exists()).toBe(true);
    expect(dynamicDocChip.text()).toContain('doc-456');
  });

  it('hides dynamic document type when not configured', () => {
    const integration = makeBaseIntegration();
    integration.dynamicDocumentType = '';
    integration.dynamicDocumentTypeLabel = null;

    const wrapper = mount(ArcGISIntegrationCard, {
      props: { integration, menuItems: [] },
    });

    const dynamicDocChip = wrapper.find('.dynamic-doc-chip');
    expect(dynamicDocChip.exists()).toBe(false);
  });

  it('hides dynamic document type when value is only whitespace', () => {
    const integration = makeBaseIntegration();
    integration.dynamicDocumentType = '   ';
    integration.dynamicDocumentTypeLabel = null;

    const wrapper = mount(ArcGISIntegrationCard, {
      props: { integration, menuItems: [] },
    });

    const dynamicDocChip = wrapper.find('.dynamic-doc-chip');
    expect(dynamicDocChip.exists()).toBe(false);
  });

  it('shows not scheduled when there is no frequency pattern or next run', () => {
    const wrapper = mount(ArcGISIntegrationCard, {
      props: {
        integration: {
          ...makeBaseIntegration(),
          frequencyPattern: '',
          nextRunAtUtc: undefined,
        },
        menuItems: [],
      },
    });

    expect(wrapper.text()).toContain('Schedule:');
    expect(wrapper.text()).toContain('Not scheduled');
    expect(wrapper.text()).toContain('Next Run:');
  });

  it('falls back to the raw time formatter when schedule parsing fails', () => {
    const wrapper = mount(ArcGISIntegrationCard, {
      props: {
        integration: {
          ...makeBaseIntegration(),
          executionDate: 'bad-date',
          executionTime: '09:30:00',
        },
        menuItems: [],
      },
    });

    expect(wrapper.text()).toContain('9:30 AM');
  });

  it.each([
    ['SUCCESS', 'dx-icon-check', 'Execution completed successfully'],
    ['FAILED', 'dx-icon-close', 'Execution failed \u2014 check logs for details'],
    ['RUNNING', 'dx-icon-refresh', 'Currently executing'],
    ['ABORTED', 'dx-icon-remove', 'Execution was manually aborted'],
    ['SCHEDULED', 'dx-icon-clock', 'Scheduled — waiting for executor to pick up'],
  ])('renders status UI for %s', async (status, iconClass, tooltipText) => {
    const integration = makeBaseIntegration() as unknown as ArcGISIntegrationSummaryResponse &
      ArcGISIntegrationExecutionSummary;
    integration.execution = {
      lastSuccessTimeUtc: '2026-01-15T10:00:00Z',
      nextRunAtUtc: '2026-01-16T09:00:00Z',
      lastStatus: status as any,
    };

    const wrapper = mount(ArcGISIntegrationCard, {
      props: { integration, menuItems: [] },
    });

    expect(wrapper.find(`i.${iconClass}`).exists()).toBe(true);
    expect(wrapper.findComponent({ name: 'CommonTooltip' }).props('text')).toBe(tooltipText);
  });

  it('uses last attempt time when no successful execution exists', () => {
    const integration = makeBaseIntegration() as unknown as ArcGISIntegrationSummaryResponse &
      ArcGISIntegrationExecutionSummary;
    integration.lastAttemptTimeUtc = '2026-01-14T08:15:00Z';
    integration.lastStatus = 'FAILED';

    const wrapper = mount(ArcGISIntegrationCard, {
      props: { integration, menuItems: [] },
    });

    expect(wrapper.text()).not.toContain('Never Run');
  });

  it('falls back to default card labels when core fields are missing', () => {
    const wrapper = mount(ArcGISIntegrationCard, {
      props: {
        integration: {
          ...makeBaseIntegration(),
          name: '',
          isEnabled: undefined,
          itemSubtypeLabel: '',
          itemSubtype: 'Raw subtype',
          nextRunAtUtc: undefined,
        } as any,
        menuItems: [],
      },
    });

    expect(wrapper.text()).toContain('Unnamed Integration');
    expect(wrapper.text()).toContain('Enabled');
    expect(wrapper.text()).toContain('Raw subtype');
    expect(wrapper.text()).toContain('Next Run:');
    expect(wrapper.text()).toContain('Not scheduled');
  });

  it('shows Never Run when there is no success or attempt timestamp', () => {
    const integration = makeBaseIntegration() as unknown as ArcGISIntegrationSummaryResponse &
      ArcGISIntegrationExecutionSummary;
    integration.execution = {
      lastSuccessTimeUtc: undefined,
      lastAttemptTimeUtc: undefined,
      nextRunAtUtc: undefined,
      lastStatus: undefined,
    } as any;

    const wrapper = mount(ArcGISIntegrationCard, {
      props: { integration, menuItems: [] },
    });

    expect(wrapper.text()).toContain('Last Run:');
    expect(wrapper.text()).toContain('Never Run');
  });

  it('falls back to default status presentation for unknown execution states', () => {
    const integration = makeBaseIntegration() as unknown as ArcGISIntegrationSummaryResponse &
      ArcGISIntegrationExecutionSummary;
    integration.execution = {
      lastSuccessTimeUtc: '2026-01-15T10:00:00Z',
      nextRunAtUtc: '2026-01-16T09:00:00Z',
      lastStatus: 'PAUSED' as any,
    };

    const wrapper = mount(ArcGISIntegrationCard, {
      props: { integration, menuItems: [] },
    });

    // Unknown status: IntegrationStatusIcon renders nothing
    expect(wrapper.find('i.dx-icon-check').exists()).toBe(false);
    expect(wrapper.find('i.dx-icon-close').exists()).toBe(false);
    expect(wrapper.find('.custom-tooltip').exists()).toBe(false);
    expect(wrapper.text()).not.toContain('Never Run');
  });

  it('shows and hides the status tooltip while hovering the status icon', async () => {
    const integration = makeBaseIntegration() as unknown as ArcGISIntegrationSummaryResponse &
      ArcGISIntegrationExecutionSummary;
    integration.execution = {
      lastSuccessTimeUtc: '2026-01-15T10:00:00Z',
      nextRunAtUtc: '2026-01-16T09:00:00Z',
      lastStatus: 'SUCCESS',
    };

    const wrapper = mount(ArcGISIntegrationCard, {
      props: { integration, menuItems: [] },
    });

    const statusIcon = wrapper.find('i.dx-icon-check');
    expect(statusIcon.exists()).toBe(true);

    await statusIcon.trigger('mouseenter', { clientX: 10, clientY: 20 });
    let tooltip = wrapper.find('.custom-tooltip');
    expect(tooltip.exists()).toBe(true);
    expect(tooltip.attributes('style')).toContain('left: 22px;');
    expect(tooltip.attributes('style')).toContain('top: 32px;');

    await statusIcon.trigger('mousemove', { clientX: 30, clientY: 40 });
    tooltip = wrapper.find('.custom-tooltip');
    expect(tooltip.attributes('style')).toContain('left: 42px;');
    expect(tooltip.attributes('style')).toContain('top: 52px;');

    await statusIcon.trigger('mouseleave');
    expect(wrapper.find('.custom-tooltip').exists()).toBe(false);
  });

  it('emits open and action events from the card shell', async () => {
    const wrapper = mount(ArcGISIntegrationCard, {
      props: {
        integration: makeBaseIntegration(),
        menuItems: [{ id: 'delete', label: 'Delete' } as any],
      },
      global: {
        stubs: {
          ActionMenu: {
            props: ['items'],
            emits: ['action'],
            template:
              '<button class="action-menu" @click.stop="$emit(\'action\', items[0].id)">menu</button>',
          },
          Tooltip: { template: '<div />' },
        },
      },
    });

    await wrapper.find('.action-menu').trigger('click');
    await wrapper.find('.integration-card').trigger('click');

    expect(wrapper.emitted('action')).toEqual([['delete']]);
    expect(wrapper.emitted('open')).toEqual([['int-1']]);
  });

  it('renders Updated label in card view', () => {
    const wrapper = mount(ArcGISIntegrationCard, {
      props: { integration: makeBaseIntegration(), menuItems: [] },
    });
    expect(wrapper.text()).toContain('U:');
  });
});
