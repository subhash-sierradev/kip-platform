/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi } from 'vitest';
import { nextTick } from 'vue';
import { ARCGIS_UNIFIED_STEP_CONFIG } from '@/utils/unifiedIntegrationStepConfig';
import IntegrationDetailsStep from '@/components/common/combinedstep/IntegrationDetailsStep.vue';

vi.mock('@/api/services/KwIntegrationService', () => ({
  KwDocService: {
    getSubItemTypes: vi.fn().mockResolvedValue([]),
    getDynamicDocuments: vi.fn(),
  },
}));

// Keep counter simple to avoid DOM noise
vi.mock('@/composables/useCharacterCounter', () => ({
  useCharacterCounter: () => ({ counterClass: 'counter-normal', counterText: '0/500' }),
}));

describe('ArcGIS Wizard - BasicDetailsStep', () => {
  it('emits validation-change false when name is empty (immediate)', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: '', description: '', itemType: 'DOCUMENT', subType: '' },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });
    await nextTick();
    const events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    // Last event should be false when name is empty
    expect(events![events!.length - 1][0]).toBe(false);
  });

  it('updates model and emits validation-change true when name provided', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: {
          name: '',
          description: '',
          itemType: 'DOCUMENT',
          subType: 'DOCUMENT_PDF',
          subTypeLabel: 'PDF Document',
        },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });
    const input = wrapper.find('input.uis-input');
    await input.setValue('Roads ArcGIS');
    await nextTick();
    await new Promise(resolve => setTimeout(resolve, 350));
    await nextTick();

    const updateEvents = wrapper.emitted('update:modelValue');
    expect(updateEvents).toBeTruthy();
    const lastModel = updateEvents![updateEvents!.length - 1][0] as {
      name: string;
      description: string;
    };
    expect(lastModel.name).toBe('Roads ArcGIS');

    const validEvents = wrapper.emitted('validation-change');
    expect(validEvents).toBeTruthy();
    expect(validEvents![validEvents!.length - 1][0]).toBe(true);
  });
});
