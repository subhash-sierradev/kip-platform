/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { nextTick } from 'vue';
import { ARCGIS_UNIFIED_STEP_CONFIG } from '@/utils/unifiedIntegrationStepConfig';

vi.mock('@/api/services/KwIntegrationService', () => ({
  KwDocService: {
    getSubItemTypes: vi.fn(),
    getDynamicDocuments: vi.fn(),
  },
}));

import IntegrationDetailsStep from '@/components/common/combinedstep/IntegrationDetailsStep.vue';
import { KwDocService } from '@/api/services/KwIntegrationService';

const getSubItemTypesMock = vi.mocked(KwDocService.getSubItemTypes);
const getDynamicDocumentsMock = vi.mocked(KwDocService.getDynamicDocuments);
const flushPromises = async () => new Promise(resolve => setTimeout(resolve, 0));

describe('ArcGIS Wizard - DocumentSelectionStep', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    // Set up default mock implementations
    getSubItemTypesMock.mockResolvedValue([
      { code: 'DOCUMENT_FINAL_DYNAMIC', displayValue: 'Dynamic Document' },
      { code: 'DOCUMENT_DRAFT_DYNAMIC', displayValue: 'Draft Dynamic Document' },
      { code: 'DOCUMENT_PDF', displayValue: 'PDF Document' },
    ]);
  });

  const createModel = () => ({
    name: 'ArcGIS Integration',
    description: '',
    itemType: 'DOCUMENT',
    subType: '',
    dynamicDocument: '',
  });

  it('shows error and keeps Next disabled when dynamic docs API fails, and Retry re-attempts', async () => {
    getDynamicDocumentsMock.mockRejectedValueOnce(new Error('Boom'));
    getDynamicDocumentsMock.mockResolvedValueOnce([
      { id: 'doc-1', title: 'Title one', tags: [] },
    ] as any);

    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: createModel(),
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    await nextTick();
    await flushPromises();

    const selects = wrapper.findAll('select.uis-input');
    await selects[1].setValue('DOCUMENT_FINAL_DYNAMIC');
    await nextTick();
    await flushPromises();

    expect(getDynamicDocumentsMock).toHaveBeenCalledWith('DOCUMENT', 'DOCUMENT_FINAL_DYNAMIC');
    expect(wrapper.text()).toContain('Boom');

    let events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    expect(events![events!.length - 1][0]).toBe(false);

    const retryButton = wrapper.find('button.uis-retry-btn');
    expect(retryButton.exists()).toBe(true);
    await retryButton.trigger('click');
    await flushPromises();

    expect(getDynamicDocumentsMock).toHaveBeenCalledTimes(2);

    // After a successful retry, dropdown shows and still requires selection.
    const updatedSelects = wrapper.findAll('select.uis-input');
    expect(updatedSelects.length).toBe(3);

    events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    expect(events![events!.length - 1][0]).toBe(false);
  });

  it('normalizes legacy dynamicDocument title to id after load (backward compatible)', async () => {
    getDynamicDocumentsMock.mockResolvedValueOnce([
      { id: 'doc-1', title: 'Title one', tags: [] },
      { id: 'doc-2', title: 'Title two', tags: [] },
    ] as any);

    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: {
          name: 'ArcGIS Integration',
          description: '',
          itemType: 'DOCUMENT',
          subType: 'DOCUMENT_FINAL_DYNAMIC',
          dynamicDocument: 'Title one',
          dynamicDocumentLabel: '',
        },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    await nextTick();
    await flushPromises();

    expect(getDynamicDocumentsMock).toHaveBeenCalledWith('DOCUMENT', 'DOCUMENT_FINAL_DYNAMIC');

    const modelUpdates = wrapper.emitted('update:modelValue');
    expect(modelUpdates).toBeTruthy();
    expect(modelUpdates![modelUpdates!.length - 1][0]).toMatchObject({
      dynamicDocument: 'doc-1',
      dynamicDocumentLabel: 'Title one',
    });
  });

  it('keeps Next disabled when API returns empty (dynamic subtype) and keeps Dynamic Document select enabled', async () => {
    getDynamicDocumentsMock.mockResolvedValueOnce([] as any);

    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: createModel(),
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    await nextTick();
    await flushPromises();

    const selects = wrapper.findAll('select.uis-input');
    // selects[0] is Item Type (disabled), selects[1] is subtype
    await selects[1].setValue('DOCUMENT_FINAL_DYNAMIC');
    await nextTick();
    await flushPromises();

    expect(getDynamicDocumentsMock).toHaveBeenCalledWith('DOCUMENT', 'DOCUMENT_FINAL_DYNAMIC');

    // Dynamic Document dropdown should render (disabled) even when API returns empty
    const dynamicSelects = wrapper.findAll('select.uis-input');
    expect(dynamicSelects.length).toBe(3);
    expect((dynamicSelects[2].element as HTMLSelectElement).disabled).toBe(false);

    const events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    expect(events![events!.length - 1][0]).toBe(false);
  });

  it('does not call dynamic docs API for non-dynamic subtype and enables Next', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: createModel(),
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    await nextTick();
    await flushPromises();

    const selects = wrapper.findAll('select.uis-input');
    await selects[1].setValue('DOCUMENT_PDF');
    await nextTick();
    await flushPromises();

    expect(getDynamicDocumentsMock).not.toHaveBeenCalled();

    const dynamicSelects = wrapper.findAll('select.uis-input');
    expect(dynamicSelects.length).toBe(2);

    const events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    expect(events![events!.length - 1][0]).toBe(true);
  });

  it('clears dynamicDocument when switching away from dynamic subtype', async () => {
    getDynamicDocumentsMock.mockResolvedValueOnce([
      { id: 'doc-1', title: 'Title one', tags: [] },
    ] as any);

    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: createModel(),
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    await nextTick();
    await flushPromises();

    let selects = wrapper.findAll('select.uis-input');
    await selects[1].setValue('DOCUMENT_FINAL_DYNAMIC');
    await nextTick();
    await flushPromises();

    selects = wrapper.findAll('select.uis-input');
    expect(selects.length).toBe(3);
    await selects[2].setValue('doc-1');
    await nextTick();

    // Now switch to a non-dynamic subtype.
    selects = wrapper.findAll('select.uis-input');
    await selects[1].setValue('DOCUMENT_PDF');
    await nextTick();
    await flushPromises();

    const finalSelects = wrapper.findAll('select.uis-input');
    expect(finalSelects.length).toBe(2);

    const modelUpdates = wrapper.emitted('update:modelValue');
    expect(modelUpdates).toBeTruthy();
    expect(modelUpdates![modelUpdates!.length - 1][0]).toMatchObject({
      subType: 'DOCUMENT_PDF',
      dynamicDocument: '',
      dynamicDocumentLabel: '',
    });

    const events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    expect(events![events!.length - 1][0]).toBe(true);
  });

  it('clears selection when switching between dynamic subtypes', async () => {
    getDynamicDocumentsMock.mockResolvedValueOnce([
      { id: 'doc-1', title: 'Common', tags: [] },
    ] as any);
    getDynamicDocumentsMock.mockResolvedValueOnce([
      { id: 'doc-2', title: 'Common', tags: [] },
    ] as any);

    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: createModel(),
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    await nextTick();
    await flushPromises();

    let selects = wrapper.findAll('select.uis-input');
    await selects[1].setValue('DOCUMENT_FINAL_DYNAMIC');
    await nextTick();
    await flushPromises();

    selects = wrapper.findAll('select.uis-input');
    await selects[2].setValue('doc-1');
    await nextTick();

    // Switch to another dynamic subtype.
    selects = wrapper.findAll('select.uis-input');
    await selects[1].setValue('DOCUMENT_DRAFT_DYNAMIC');
    await nextTick();
    await flushPromises();

    const modelUpdates = wrapper.emitted('update:modelValue');
    expect(modelUpdates).toBeTruthy();
    expect(modelUpdates![modelUpdates!.length - 1][0]).toMatchObject({
      subType: 'DOCUMENT_DRAFT_DYNAMIC',
      dynamicDocument: '',
      dynamicDocumentLabel: '',
    });

    const events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    expect(events![events!.length - 1][0]).toBe(false);
  });

  it('clears selection when switching between dynamic subtypes if it is not valid for the new subtype', async () => {
    getDynamicDocumentsMock.mockResolvedValueOnce([
      { id: 'doc-1', title: 'OnlyInFinal', tags: [] },
    ] as any);
    getDynamicDocumentsMock.mockResolvedValueOnce([
      { id: 'doc-2', title: 'OnlyInDraft', tags: [] },
    ] as any);

    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: createModel(),
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    await nextTick();
    await flushPromises();

    let selects = wrapper.findAll('select.uis-input');
    await selects[1].setValue('DOCUMENT_FINAL_DYNAMIC');
    await nextTick();
    await flushPromises();

    selects = wrapper.findAll('select.uis-input');
    await selects[2].setValue('doc-1');
    await nextTick();

    // Switch to another dynamic subtype where the selected title doesn't exist.
    selects = wrapper.findAll('select.uis-input');
    await selects[1].setValue('DOCUMENT_DRAFT_DYNAMIC');
    await nextTick();
    await flushPromises();

    const modelUpdates = wrapper.emitted('update:modelValue');
    expect(modelUpdates).toBeTruthy();
    expect(modelUpdates![modelUpdates!.length - 1][0]).toMatchObject({
      subType: 'DOCUMENT_DRAFT_DYNAMIC',
      dynamicDocument: '',
      dynamicDocumentLabel: '',
    });

    const events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    expect(events![events!.length - 1][0]).toBe(false);
  });

  it('shows Dynamic Document dropdown when API returns data and enables Next after selection', async () => {
    getDynamicDocumentsMock.mockResolvedValueOnce([
      { id: 'doc-1', title: 'Testing comments', tags: [] },
      { id: 'doc-2', title: 'Title two', tags: ['TAG'] },
    ] as any);

    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: createModel(),
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    await nextTick();
    await flushPromises();

    const selects = wrapper.findAll('select.uis-input');
    await selects[1].setValue('DOCUMENT_FINAL_DYNAMIC');
    await nextTick();
    await flushPromises();

    // Dynamic dropdown should now exist
    const updatedSelects = wrapper.findAll('select.uis-input');
    expect(updatedSelects.length).toBe(3);

    // Select a dynamic document
    await updatedSelects[2].setValue('doc-2');
    await nextTick();

    const modelUpdates = wrapper.emitted('update:modelValue');
    expect(modelUpdates).toBeTruthy();
    expect(modelUpdates![modelUpdates!.length - 1][0]).toMatchObject({
      dynamicDocument: 'doc-2',
      dynamicDocumentLabel: 'Title two',
    });

    const events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    expect(events![events!.length - 1][0]).toBe(true);
  });
});
