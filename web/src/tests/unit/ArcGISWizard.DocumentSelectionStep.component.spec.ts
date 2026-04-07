import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';

import DocumentSelectionStep from '@/components/outbound/arcgisintegration/wizard/steps/DocumentSelectionStep.vue';

const hoisted = vi.hoisted(() => ({
  getSubItemTypesMock: vi.fn(),
  getDynamicDocumentsMock: vi.fn(),
}));

vi.mock('@/api/services/KwIntegrationService', () => ({
  KwDocService: {
    getSubItemTypes: hoisted.getSubItemTypesMock,
    getDynamicDocuments: hoisted.getDynamicDocumentsMock,
  },
}));

const flushPromises = async () => new Promise(resolve => setTimeout(resolve, 0));

const createModel = () => ({
  itemType: 'DOCUMENT',
  subType: '',
  subTypeLabel: '',
  dynamicDocument: '',
  dynamicDocumentLabel: '',
});

describe('ArcGIS Wizard DocumentSelectionStep.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    hoisted.getSubItemTypesMock.mockResolvedValue([
      { code: 'DOCUMENT_FINAL_DYNAMIC', displayValue: 'Dynamic Final' },
      { code: 'DOCUMENT_DRAFT_DYNAMIC', displayValue: 'Dynamic Draft' },
      { code: 'DOCUMENT_PDF', displayValue: 'PDF' },
    ]);
    hoisted.getDynamicDocumentsMock.mockResolvedValue([
      { id: 'doc-1', title: 'Doc One', tags: [] },
      { id: 'doc-2', title: 'Doc Two', tags: [] },
    ]);
  });

  it('loads subtypes on mount and retries when subtype load fails', async () => {
    hoisted.getSubItemTypesMock.mockRejectedValueOnce(new Error('subtypes failed'));

    const wrapper = mount(DocumentSelectionStep, {
      props: {
        modelValue: createModel(),
      },
    });

    await nextTick();
    await flushPromises();

    expect(wrapper.text()).toContain('subtypes failed');

    await wrapper.find('button.ds-retry-btn').trigger('click');
    await flushPromises();

    expect(hoisted.getSubItemTypesMock).toHaveBeenCalledTimes(2);
  });

  it('validates true for non-dynamic subtype and does not call dynamic docs api', async () => {
    const wrapper = mount(DocumentSelectionStep, {
      props: {
        modelValue: createModel(),
      },
    });

    await nextTick();
    await flushPromises();

    const selects = wrapper.findAll('select.ds-input');
    await selects[1].setValue('DOCUMENT_PDF');
    await nextTick();
    await flushPromises();

    expect(hoisted.getDynamicDocumentsMock).not.toHaveBeenCalled();

    const validationEvents = wrapper.emitted('validation-change') ?? [];
    expect(validationEvents.at(-1)).toEqual([true]);
  });

  it('requires dynamic document selection for dynamic subtype', async () => {
    const wrapper = mount(DocumentSelectionStep, {
      props: {
        modelValue: createModel(),
      },
    });

    await nextTick();
    await flushPromises();

    const selects = wrapper.findAll('select.ds-input');
    await selects[1].setValue('DOCUMENT_FINAL_DYNAMIC');
    await nextTick();
    await flushPromises();

    expect(hoisted.getDynamicDocumentsMock).toHaveBeenCalledWith(
      'DOCUMENT',
      'DOCUMENT_FINAL_DYNAMIC'
    );

    let validationEvents = wrapper.emitted('validation-change') ?? [];
    expect(validationEvents.at(-1)).toEqual([false]);

    const updatedSelects = wrapper.findAll('select.ds-input');
    await updatedSelects[2].setValue('doc-2');
    await nextTick();

    validationEvents = wrapper.emitted('validation-change') ?? [];
    expect(validationEvents.at(-1)).toEqual([true]);

    const modelEvents = wrapper.emitted('update:modelValue') ?? [];
    expect(modelEvents.at(-1)?.[0]).toMatchObject({
      dynamicDocument: 'doc-2',
      dynamicDocumentLabel: 'Doc Two',
    });
  });

  it('shows dynamic docs error and retry path', async () => {
    hoisted.getDynamicDocumentsMock.mockRejectedValueOnce(new Error('docs failed'));

    const wrapper = mount(DocumentSelectionStep, {
      props: {
        modelValue: createModel(),
      },
    });

    await nextTick();
    await flushPromises();

    const selects = wrapper.findAll('select.ds-input');
    await selects[1].setValue('DOCUMENT_DRAFT_DYNAMIC');
    await nextTick();
    await flushPromises();

    expect(wrapper.text()).toContain('docs failed');

    const retryButtons = wrapper.findAll('button.ds-retry-btn');
    await retryButtons.at(-1)!.trigger('click');
    await flushPromises();

    expect(hoisted.getDynamicDocumentsMock).toHaveBeenCalledTimes(2);
  });
});
