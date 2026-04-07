/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import SampleDataStep from '@/components/outbound/jirawebhooks/wizard/SampleDataStep.vue';

describe('SampleDataStep', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Initial Rendering', () => {
    it('renders with empty jsonSample prop', () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });
      expect(wrapper.find('.sd-title').text()).toBe(
        'Sample Webhook Payload for Jira Field Mapping'
      );
      expect(wrapper.find('textarea.sd-textarea').exists()).toBe(true);
      expect(wrapper.find('.sd-btn-orange').text()).toContain('Upload File');
      expect(wrapper.find('.sd-btn-gray').text()).toContain('From Clipboard');
      expect(wrapper.find('.sd-btn-format').text()).toContain('Format JSON');
    });

    it('renders with valid jsonSample prop and validates on mount', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '{"test": true}' } });
      await wrapper.vm.$nextTick();
      expect(wrapper.find('.sd-alert').exists()).toBe(false);
      expect(wrapper.find('.sd-btn-format').attributes('disabled')).toBeUndefined();
    });

    it('renders with invalid jsonSample prop and shows error on mount', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: 'bad json' } });
      await wrapper.vm.$nextTick();
      expect(wrapper.find('.sd-alert').exists()).toBe(true);
      expect(wrapper.find('.sd-alert').text()).toBe('Invalid JSON. Please fix the payload.');
    });
  });

  describe('JSON Validation', () => {
    it('validates empty input without showing error', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });
      const textarea = wrapper.find('textarea.sd-textarea');
      await textarea.setValue('');

      expect(wrapper.find('.sd-alert').exists()).toBe(false);
      // Check the last emitted value (after mount and setValue)
      const emissions = wrapper.emitted('json-validity-change');
      expect(emissions?.[emissions.length - 1]).toEqual([false]);
    });

    it('validates whitespace-only input as empty', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });
      const textarea = wrapper.find('textarea.sd-textarea');
      await textarea.setValue('   \n\t  ');

      expect(wrapper.find('.sd-alert').exists()).toBe(false);
      const emissions = wrapper.emitted('json-validity-change');
      expect(emissions?.[emissions.length - 1]).toEqual([false]);
    });

    it('validates JSON and enables Format button when valid', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });
      const formatBtn = wrapper.find('.sd-btn-format');
      expect(formatBtn.attributes('disabled')).toBeDefined();

      const textarea = wrapper.find('textarea.sd-textarea');
      await textarea.setValue('{"a":1}');

      // After valid input, button enabled
      expect(formatBtn.attributes('disabled')).toBeUndefined();
      const validityEmissions = wrapper.emitted('json-validity-change');
      expect(validityEmissions?.[validityEmissions.length - 1]).toEqual([true]);
      const updateEmissions = wrapper.emitted('update:jsonSample');
      expect(updateEmissions?.[updateEmissions.length - 1]).toEqual(['{"a":1}']);

      // Clicking format keeps valid JSON formatted
      await formatBtn.trigger('click');
      expect((textarea.element as HTMLTextAreaElement).value).toContain('"a": 1');
    });

    it('shows error for invalid JSON', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });
      const textarea = wrapper.find('textarea.sd-textarea');
      await textarea.setValue('not-json');

      expect(wrapper.find('.sd-alert').exists()).toBe(true);
      expect(wrapper.find('.sd-alert').text()).toBe('Invalid JSON. Please fix the payload.');
      const emissions = wrapper.emitted('json-validity-change');
      expect(emissions?.[emissions.length - 1]).toEqual([false]);
    });

    it('detects duplicate keys in JSON object', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });
      const textarea = wrapper.find('textarea.sd-textarea');
      await textarea.setValue('{"name": "John", "name": "Jane"}');

      expect(wrapper.find('.sd-alert').exists()).toBe(true);
      expect(wrapper.find('.sd-alert').text()).toContain('Duplicate field found: "name"');
      const emissions = wrapper.emitted('json-validity-change');
      expect(emissions?.[emissions.length - 1]).toEqual([false]);
    });

    it('detects duplicate keys in nested objects', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });
      const textarea = wrapper.find('textarea.sd-textarea');
      await textarea.setValue('{"user": {"id": 1, "id": 2}}');

      expect(wrapper.find('.sd-alert').exists()).toBe(true);
      expect(wrapper.find('.sd-alert').text()).toContain('Duplicate field found: "id"');
    });

    it('allows same keys in different object levels', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });
      const textarea = wrapper.find('textarea.sd-textarea');
      await textarea.setValue('{"id": 1, "user": {"id": 2}}');

      expect(wrapper.find('.sd-alert').exists()).toBe(false);
      const emissions = wrapper.emitted('json-validity-change');
      expect(emissions?.[emissions.length - 1]).toEqual([true]);
    });

    it('handles JSON with escaped quotes in string values', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });
      const textarea = wrapper.find('textarea.sd-textarea');
      await textarea.setValue('{"message": "He said \\"hello\\""}');

      expect(wrapper.find('.sd-alert').exists()).toBe(false);
      const emissions = wrapper.emitted('json-validity-change');
      expect(emissions?.[emissions.length - 1]).toEqual([true]);
    });

    it('handles complex nested JSON structures', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });
      const textarea = wrapper.find('textarea.sd-textarea');
      const complexJson =
        '{"form": {"title": "Test", "nested": {"deep": [1,2,3]}}, "users": [{"name": "A"}]}';
      await textarea.setValue(complexJson);

      expect(wrapper.find('.sd-alert').exists()).toBe(false);
      const emissions = wrapper.emitted('json-validity-change');
      expect(emissions?.[emissions.length - 1]).toEqual([true]);
    });
  });

  describe('Props Watcher', () => {
    it('updates local state when jsonSample prop changes', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '{"a": 1}' } });
      await wrapper.vm.$nextTick();

      await wrapper.setProps({ jsonSample: '{"b": 2}' });
      await wrapper.vm.$nextTick();

      const textarea = wrapper.find('textarea.sd-textarea');
      expect((textarea.element as HTMLTextAreaElement).value).toBe('{"b": 2}');
    });

    it('validates when prop changes to invalid JSON', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '{"a": 1}' } });
      await wrapper.vm.$nextTick();

      await wrapper.setProps({ jsonSample: 'invalid' });
      await wrapper.vm.$nextTick();

      expect(wrapper.find('.sd-alert').exists()).toBe(true);
    });
  });

  describe('Format JSON Button', () => {
    it('formats valid JSON with proper indentation', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });
      const textarea = wrapper.find('textarea.sd-textarea');
      await textarea.setValue('{"a":1,"b":2}');

      const formatBtn = wrapper.find('.sd-btn-format');
      await formatBtn.trigger('click');

      const formatted = (textarea.element as HTMLTextAreaElement).value;
      expect(formatted).toContain('"a": 1');
      expect(formatted).toContain('"b": 2');
      expect(formatted.split('\n').length).toBeGreaterThan(1);
    });

    it('does nothing when JSON is invalid', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });
      const textarea = wrapper.find('textarea.sd-textarea');
      await textarea.setValue('not-json');

      const formatBtn = wrapper.find('.sd-btn-format');
      await formatBtn.trigger('click');

      expect((textarea.element as HTMLTextAreaElement).value).toBe('not-json');
    });

    it('emits update:jsonSample when formatting', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });
      const textarea = wrapper.find('textarea.sd-textarea');
      await textarea.setValue('{"a":1}');

      const formatBtn = wrapper.find('.sd-btn-format');
      await formatBtn.trigger('click');

      const emitted = wrapper.emitted('update:jsonSample');
      expect(emitted).toBeDefined();
      expect(emitted?.[emitted.length - 1][0]).toContain('"a": 1');
    });
  });

  describe('Upload File Button', () => {
    it('creates file input with correct attributes', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });

      const mockClick = vi.fn();
      const mockInput = document.createElement('input');
      mockInput.click = mockClick;

      const createElementSpy = vi.spyOn(document, 'createElement').mockReturnValue(mockInput);

      const uploadBtn = wrapper.find('.sd-btn-orange');
      await uploadBtn.trigger('click');

      expect(createElementSpy).toHaveBeenCalledWith('input');
      expect(mockInput.type).toBe('file');
      expect(mockInput.accept).toBe('.json');
      expect(mockClick).toHaveBeenCalled();

      createElementSpy.mockRestore();
    });
  });

  describe('Paste from Clipboard Button', () => {
    it('attempts to read from clipboard when clicked', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });

      const mockReadText = vi.fn().mockResolvedValue('{"clipboard":true}');
      Object.defineProperty(navigator, 'clipboard', {
        value: { readText: mockReadText },
        writable: true,
        configurable: true,
      });

      const clipboardBtn = wrapper.find('.sd-btn-gray');
      await clipboardBtn.trigger('click');
      await vi.dynamicImportSettled();

      expect(mockReadText).toHaveBeenCalled();
    });

    it('shows error message when clipboard read fails', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });

      const mockReadText = vi.fn().mockRejectedValue(new Error('Permission denied'));
      Object.defineProperty(navigator, 'clipboard', {
        value: { readText: mockReadText },
        writable: true,
        configurable: true,
      });

      const clipboardBtn = wrapper.find('.sd-btn-gray');
      await clipboardBtn.trigger('click');

      // Wait for promises to settle and component to update
      await new Promise(resolve => setTimeout(resolve, 0));
      await wrapper.vm.$nextTick();

      expect(wrapper.find('.sd-alert').exists()).toBe(true);
      expect(wrapper.find('.sd-alert').text()).toBe('Clipboard read failed. Paste manually.');
    });
  });

  describe('CSS Classes', () => {
    it('applies error class to textarea when JSON is invalid', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });
      const textarea = wrapper.find('textarea.sd-textarea');

      await textarea.setValue('bad json');
      await wrapper.vm.$nextTick();

      expect(textarea.classes()).toContain('sd-textarea-error');
    });

    it('does not apply error class when JSON is valid', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });
      const textarea = wrapper.find('textarea.sd-textarea');

      await textarea.setValue('{"valid": true}');
      await wrapper.vm.$nextTick();

      expect(textarea.classes()).not.toContain('sd-textarea-error');
    });

    it('format button is disabled when JSON is invalid', async () => {
      const wrapper = mount(SampleDataStep, { props: { jsonSample: '' } });
      await wrapper.vm.$nextTick();

      const formatBtn = wrapper.find('.sd-btn-format');
      expect(formatBtn.attributes('disabled')).toBeDefined();
    });
  });
});
