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
});
