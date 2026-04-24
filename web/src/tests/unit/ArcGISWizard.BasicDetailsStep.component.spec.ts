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

  it('sets the integration name maxlength to the configured max', () => {
    const wrapper = mount(BasicDetailsStep, {
      props: {
        modelValue: { name: '', description: '' },
      },
    });

    expect(wrapper.find('input.bd-input').attributes('maxlength')).toBe('100');
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

  it('shows a required-name error after interaction when the name is cleared', async () => {
    const wrapper = mount(BasicDetailsStep, {
      props: {
        modelValue: { name: '', description: '' },
      },
    });

    const input = wrapper.find('input.bd-input');
    await input.setValue('Temporary Name');
    vi.advanceTimersByTime(350);
    await nextTick();

    await input.setValue('');
    await nextTick();

    expect(wrapper.find('.bd-helper-error').text()).toBe('Integration name is required');
    const validationEvents = wrapper.emitted('validation-change') ?? [];
    expect(validationEvents.at(-1)).toEqual([false]);
  });

  it('updates local state from props and marks edit mode fields as interacted', async () => {
    const wrapper = mount(BasicDetailsStep, {
      props: {
        modelValue: { name: '', description: '' },
        editMode: true,
        originalName: 'Original Name',
        normalizedNames: ['other_name'],
      },
    });

    await wrapper.setProps({
      modelValue: { name: 'Original Name', description: 'Updated description' },
    });
    await nextTick();
    vi.advanceTimersByTime(350);
    await nextTick();

    expect((wrapper.find('input.bd-input').element as HTMLInputElement).value).toBe(
      'Original Name'
    );
    expect((wrapper.find('textarea.bd-textarea').element as HTMLTextAreaElement).value).toBe(
      'Updated description'
    );

    const validationEvents = wrapper.emitted('validation-change') ?? [];
    expect(validationEvents.at(-1)).toEqual([true]);
  });

  it('revalidates against updated normalized names', async () => {
    const wrapper = mount(BasicDetailsStep, {
      props: {
        modelValue: { name: '', description: '' },
        normalizedNames: ['first'],
      },
    });

    await wrapper.setProps({ normalizedNames: ['first', 'duplicate name'] });
    checkDuplicateNameMock.mockReturnValueOnce(true);
    await wrapper.find('input.bd-input').setValue('Duplicate Name');
    vi.advanceTimersByTime(350);
    await nextTick();

    expect(wrapper.find('.bd-helper-error').text()).toContain('already exists');
  });

  it('treats a prefilled create-mode name as valid even before user interaction', async () => {
    const wrapper = mount(BasicDetailsStep, {
      props: {
        modelValue: { name: 'Prefilled Name', description: '' },
        normalizedNames: ['other_name'],
      },
    });

    await nextTick();

    const validationEvents = wrapper.emitted('validation-change') ?? [];
    expect(validationEvents.at(-1)).toEqual([true]);
    expect(checkDuplicateNameMock).not.toHaveBeenCalled();
  });

  it('does not run duplicate checks for whitespace-only names and shows required validation', async () => {
    const wrapper = mount(BasicDetailsStep, {
      props: {
        modelValue: { name: '', description: '' },
        normalizedNames: ['duplicate_name'],
      },
    });

    const input = wrapper.find('input.bd-input');
    await input.setValue('   ');
    await nextTick();

    expect(checkDuplicateNameMock).not.toHaveBeenCalled();
    expect(wrapper.find('.bd-helper-error').text()).toBe('Integration name is required');
  });

  it('clears normalized names when the prop becomes undefined and validates a unique name', async () => {
    const wrapper = mount(BasicDetailsStep, {
      props: {
        modelValue: { name: '', description: '' },
        normalizedNames: ['duplicate_name'],
      },
    });

    await wrapper.setProps({ normalizedNames: undefined });
    await wrapper.find('input.bd-input').setValue('Duplicate Name');
    vi.advanceTimersByTime(350);
    await nextTick();

    expect(checkDuplicateNameMock).toHaveBeenCalledWith('Duplicate Name', [], undefined, false);
    const validationEvents = wrapper.emitted('validation-change') ?? [];
    expect(validationEvents.at(-1)).toEqual([true]);
  });

  it('does not run duplicate validation for prop-driven create-mode updates before interaction', async () => {
    const wrapper = mount(BasicDetailsStep, {
      props: {
        modelValue: { name: '', description: '' },
        normalizedNames: ['prefilled_name'],
      },
    });

    await wrapper.setProps({
      modelValue: { name: 'Prefilled From Parent', description: 'Loaded from parent' },
    });
    vi.advanceTimersByTime(350);
    await nextTick();

    expect(checkDuplicateNameMock).not.toHaveBeenCalled();
    expect((wrapper.find('input.bd-input').element as HTMLInputElement).value).toBe(
      'Prefilled From Parent'
    );

    const validationEvents = wrapper.emitted('validation-change') ?? [];
    expect(validationEvents.at(-1)).toEqual([true]);
  });

  it('cancels the previous debounce when the name changes again before validation runs', async () => {
    const wrapper = mount(BasicDetailsStep, {
      props: {
        modelValue: { name: '', description: '' },
        normalizedNames: ['other_name'],
      },
    });

    const input = wrapper.find('input.bd-input');
    await input.setValue('First Name');
    await input.setValue('Second Name');
    vi.advanceTimersByTime(350);
    await nextTick();

    expect(checkDuplicateNameMock).toHaveBeenCalledTimes(1);
    expect(checkDuplicateNameMock).toHaveBeenLastCalledWith(
      'Second Name',
      ['other_name'],
      undefined,
      false
    );

    const validationEvents = wrapper.emitted('validation-change') ?? [];
    expect(validationEvents.at(-1)).toEqual([true]);
  });

  it('truncates integration names longer than the configured max before updating the model', async () => {
    const wrapper = mount(BasicDetailsStep, {
      props: {
        modelValue: { name: '', description: '' },
      },
    });

    const input = wrapper.find('input.bd-input');
    const longName = 'a'.repeat(125);

    await input.setValue(longName);
    vi.advanceTimersByTime(350);
    await nextTick();

    const modelEvents = wrapper.emitted('update:modelValue') ?? [];
    const lastModel = modelEvents.at(-1)?.[0] as { name?: string } | undefined;
    expect(String(lastModel?.name).length).toBe(100);
    expect((input.element as HTMLInputElement).value.length).toBe(100);
  });
});
