import { mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';

import BasicDetailsStep from '@/components/outbound/arcgisintegration/wizard/steps/BasicDetailsStep.vue';

const checkDuplicateNameMock = vi.fn();

vi.mock('@/composables/useCharacterCounter', () => ({
  useCharacterCounter: () => ({
    counterClass: 'counter-normal',
    counterText: '0/500',
  }),
}));

vi.mock('@/utils/globalNormalizedUtils', () => ({
  checkDuplicateName: (...args: unknown[]) => checkDuplicateNameMock(...args),
}));

describe('ArcGIS Wizard BasicDetailsStep.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
    checkDuplicateNameMock.mockReturnValue(false);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('emits invalid initially when untouched and name is empty', async () => {
    const wrapper = mount(BasicDetailsStep, {
      props: {
        modelValue: { name: '', description: '' },
      },
    });

    await nextTick();

    const validationEvents = wrapper.emitted('validation-change') ?? [];
    expect(validationEvents.length).toBeGreaterThan(0);
    expect(validationEvents.at(-1)).toEqual([false]);
  });

  it('shows duplicate-name error and emits invalid after debounce', async () => {
    checkDuplicateNameMock.mockReturnValueOnce(true);

    const wrapper = mount(BasicDetailsStep, {
      props: {
        modelValue: { name: '', description: '' },
        normalizedNames: ['existing_name'],
      },
    });

    const nameInput = wrapper.find('input.bd-input');
    await nameInput.setValue('Existing Name');

    vi.advanceTimersByTime(350);
    await nextTick();

    expect(checkDuplicateNameMock).toHaveBeenCalled();
    expect(wrapper.find('.bd-helper-error').text()).toContain('already exists');

    const validationEvents = wrapper.emitted('validation-change') ?? [];
    expect(validationEvents.at(-1)).toEqual([false]);
  });

  it('emits valid for unique name and emits model updates for description', async () => {
    const wrapper = mount(BasicDetailsStep, {
      props: {
        modelValue: { name: '', description: '' },
        normalizedNames: ['other'],
      },
    });

    await wrapper.find('input.bd-input').setValue('Unique ArcGIS Name');
    vi.advanceTimersByTime(350);
    await nextTick();

    const validationEvents = wrapper.emitted('validation-change') ?? [];
    expect(validationEvents.at(-1)).toEqual([true]);

    await wrapper.find('textarea.bd-textarea').setValue('ArcGIS details');
    const modelEvents = wrapper.emitted('update:modelValue') ?? [];
    expect(modelEvents.length).toBeGreaterThan(0);
    expect(modelEvents.at(-1)?.[0]).toMatchObject({
      name: 'Unique ArcGIS Name',
      description: 'ArcGIS details',
    });
  });

  it('accepts original name in edit mode and emits valid', async () => {
    const wrapper = mount(BasicDetailsStep, {
      props: {
        modelValue: { name: 'Original Name', description: '' },
        editMode: true,
        originalName: 'Original Name',
        normalizedNames: ['original_name', 'other_name'],
      },
    });

    // Watcher marks interaction in edit mode; re-enter same name to trigger debounce branch.
    await wrapper.find('input.bd-input').setValue('Original Name');
    vi.advanceTimersByTime(350);
    await nextTick();

    const validationEvents = wrapper.emitted('validation-change') ?? [];
    expect(validationEvents.at(-1)).toEqual([true]);
  });
});
