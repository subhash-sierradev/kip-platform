/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { nextTick } from 'vue';
import BasicDetailsStep from '@/components/outbound/jirawebhooks/wizard/BasicDetailsStep.vue';

// Mock character counter composable
vi.mock('@/composables/useCharacterCounter', () => ({
  useCharacterCounter: () => ({
    counterClass: 'counter-normal',
    counterText: '0/500',
    truncateValue: (v: string) => v.slice(0, 500),
  }),
}));

// Mock duplicate check utility
vi.mock('@/utils/globalNormalizedUtils', () => ({
  checkDuplicateName: vi.fn(
    (name: string, existing: string[], original?: string, editMode?: boolean) => {
      const normalized = name.toLowerCase().replace(/\s+/g, ' ').trim();
      const originalNormalized = original?.toLowerCase().replace(/\s+/g, ' ').trim();

      if (editMode && normalized === originalNormalized) {
        return false;
      }

      return existing.some(ex => ex.toLowerCase().replace(/\s+/g, ' ').trim() === normalized);
    }
  ),
}));

describe('BasicDetailsStep', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  describe('Component Rendering', () => {
    it('renders with default title "Create Jira Webhook"', () => {
      const wrapper = mount(BasicDetailsStep, {
        props: { integrationName: '', description: '' },
      });
      expect(wrapper.find('.bd-title').text()).toBe('Create Jira Webhook');
    });

    it('renders with "Update Jira Webhook" title in edit mode', () => {
      const wrapper = mount(BasicDetailsStep, {
        props: { integrationName: 'Test', description: '', editMode: true },
      });
      expect(wrapper.find('.bd-title').text()).toBe('Update Jira Webhook');
    });

    it('renders with "Clone Jira Webhook" title in clone mode', () => {
      const wrapper = mount(BasicDetailsStep, {
        props: { integrationName: 'Test', description: '', cloneMode: true },
      });
      expect(wrapper.find('.bd-title').text()).toBe('Clone Jira Webhook');
    });

    it('renders name input field with placeholder', () => {
      const wrapper = mount(BasicDetailsStep, {
        props: { integrationName: '', description: '' },
      });
      const nameInput = wrapper.find('input.bd-input');
      expect(nameInput.exists()).toBe(true);
      expect(nameInput.attributes('placeholder')).toBe('Jira Webhook Name');
    });

    it('renders description textarea with placeholder', () => {
      const wrapper = mount(BasicDetailsStep, {
        props: { integrationName: '', description: '' },
      });
      const textarea = wrapper.find('textarea.bd-textarea');
      expect(textarea.exists()).toBe(true);
      expect(textarea.attributes('placeholder')).toContain('Provide a brief description');
    });

    it('renders character counter for description', () => {
      const wrapper = mount(BasicDetailsStep, {
        props: { integrationName: 'Test', description: '' },
      });
      const counter = wrapper.find('.bd-desc-counter');
      expect(counter.exists()).toBe(true);
      expect(counter.text()).toBe('0/500');
    });
  });

  describe('Name Input and Validation', () => {
    it('emits update:integrationName on name input', async () => {
      const wrapper = mount(BasicDetailsStep, {
        props: { integrationName: '', description: '' },
      });

      const nameInput = wrapper.find('input.bd-input');
      await nameInput.setValue('My Jira Hook');

      const nameEmits = wrapper.emitted('update:integrationName');
      expect(nameEmits).toBeTruthy();
      expect(nameEmits?.[0][0]).toBe('My Jira Hook');
    });

    it('shows required error for empty name after interaction', async () => {
      const wrapper = mount(BasicDetailsStep, {
        props: { integrationName: '', description: '' },
      });

      const nameInput = wrapper.find('input.bd-input');
      await nameInput.setValue('Test');
      await vi.runAllTimersAsync();
      await nextTick();

      await nameInput.setValue('');
      await vi.runAllTimersAsync();
      await nextTick();

      expect(wrapper.find('.bd-helper-error').exists()).toBe(true);
      expect(wrapper.find('.bd-helper-error').text()).toBe('Jira webhook name is required');
      expect(wrapper.find('.bd-input-error').exists()).toBe(true);
    });

    it('does not show error for empty name before interaction', async () => {
      const wrapper = mount(BasicDetailsStep, {
        props: { integrationName: '', description: '' },
      });

      await nextTick();

      const helper = wrapper.find('.bd-helper');
      expect(helper.classes()).not.toContain('bd-helper-error');
    });

    it('emits validation-change false for empty name after interaction', async () => {
      const wrapper = mount(BasicDetailsStep, {
        props: { integrationName: '', description: '' },
      });

      const nameInput = wrapper.find('input.bd-input');
      await nameInput.setValue('Test');
      await vi.runAllTimersAsync();

      await nameInput.setValue('');
      await vi.runAllTimersAsync();
      await nextTick();

      const events = wrapper.emitted('validation-change');
      expect(events).toBeTruthy();
      const last = events![events!.length - 1][0];
      expect(last).toBe(false);
    });

    it('emits validation-change true for valid unique name', async () => {
      const wrapper = mount(BasicDetailsStep, {
        props: {
          integrationName: '',
          description: '',
          normalizedNames: ['existing webhook'],
        },
      });

      const nameInput = wrapper.find('input.bd-input');
      await nameInput.setValue('Unique Name');
      await vi.runAllTimersAsync();
      await nextTick();

      const events = wrapper.emitted('validation-change');
      expect(events).toBeTruthy();
      const last = events![events!.length - 1][0];
      expect(last).toBe(true);
    });

    it('shows duplicate error for existing name', async () => {
      const wrapper = mount(BasicDetailsStep, {
        props: {
          integrationName: '',
          description: '',
          normalizedNames: ['Existing Webhook'],
        },
      });

      const nameInput = wrapper.find('input.bd-input');
      await nameInput.setValue('existing webhook');
      await vi.runAllTimersAsync();
      await nextTick();

      expect(wrapper.find('.bd-helper-error').exists()).toBe(true);
      expect(wrapper.find('.bd-helper-error').text()).toBe('Jira webhook name already exists');

      const events = wrapper.emitted('validation-change');
      expect(events).toBeTruthy();
      const last = events![events!.length - 1][0];
      expect(last).toBe(false);
    });

    it('allows original name in edit mode', async () => {
      const wrapper = mount(BasicDetailsStep, {
        props: {
          integrationName: 'My Webhook',
          description: '',
          editMode: true,
          originalName: 'My Webhook',
          normalizedNames: ['My Webhook', 'Other Webhook'],
        },
      });

      const nameInput = wrapper.find('input.bd-input');
      await nameInput.setValue('My Webhook');
      await vi.runAllTimersAsync();
      await nextTick();

      expect(wrapper.find('.bd-helper-error').exists()).toBe(false);

      const events = wrapper.emitted('validation-change');
      expect(events).toBeTruthy();
      const last = events![events!.length - 1][0];
      expect(last).toBe(true);
    });

    it('validates on mount with existing name', async () => {
      const wrapper = mount(BasicDetailsStep, {
        props: {
          integrationName: 'Test Webhook',
          description: '',
        },
      });

      await nextTick();
      await vi.runAllTimersAsync();

      const events = wrapper.emitted('validation-change');
      expect(events).toBeTruthy();
    });

    it('debounces validation to avoid excessive checks', async () => {
      const wrapper = mount(BasicDetailsStep, {
        props: { integrationName: '', description: '' },
      });

      const nameInput = wrapper.find('input.bd-input');
      await nameInput.setValue('T');
      await nameInput.setValue('Te');
      await nameInput.setValue('Tes');
      await nameInput.setValue('Test');

      await vi.runAllTimersAsync();
      await nextTick();

      const events = wrapper.emitted('validation-change');
      expect(events).toBeTruthy();
      // Should have debounced to fewer emissions than inputs
      expect(events!.length).toBeLessThan(5);
    });

    it('updates normalizedNames when prop changes', async () => {
      const wrapper = mount(BasicDetailsStep, {
        props: {
          integrationName: 'Test',
          description: '',
          normalizedNames: ['Original'],
        },
      });

      await wrapper.setProps({ normalizedNames: ['Original', 'New Name'] });
      await nextTick();

      const nameInput = wrapper.find('input.bd-input');
      await nameInput.setValue('new name');
      await vi.runAllTimersAsync();
      await nextTick();

      expect(wrapper.find('.bd-helper-error').exists()).toBe(true);
    });
  });

  describe('Description Input', () => {
    it('emits update:description on description input', async () => {
      const wrapper = mount(BasicDetailsStep, {
        props: { integrationName: 'Test', description: '' },
      });

      const textarea = wrapper.find('textarea.bd-textarea');
      await textarea.setValue('This is a test description');

      const descEmits = wrapper.emitted('update:description');
      expect(descEmits).toBeTruthy();
      expect(descEmits?.[0][0]).toBe('This is a test description');
    });

    it('renders initial description value', () => {
      const wrapper = mount(BasicDetailsStep, {
        props: {
          integrationName: 'Test',
          description: 'Initial description',
        },
      });

      const textarea = wrapper.find('textarea.bd-textarea');
      expect((textarea.element as HTMLTextAreaElement).value).toBe('Initial description');
    });

    it('has maxlength attribute set to 500', () => {
      const wrapper = mount(BasicDetailsStep, {
        props: { integrationName: 'Test', description: '' },
      });

      const textarea = wrapper.find('textarea.bd-textarea');
      expect(textarea.attributes('maxlength')).toBe('500');
    });

    it('truncates description using character counter utility', async () => {
      const wrapper = mount(BasicDetailsStep, {
        props: { integrationName: 'Test', description: '' },
      });

      const longText = 'a'.repeat(600);
      const textarea = wrapper.find('textarea.bd-textarea');
      await textarea.setValue(longText);

      const descEmits = wrapper.emitted('update:description');
      expect(descEmits).toBeTruthy();
      expect((descEmits as any)?.[0][0].length).toBe(500);
    });
  });

  describe('Props Watching', () => {
    it('revalidates when integrationName prop changes', async () => {
      const wrapper = mount(BasicDetailsStep, {
        props: { integrationName: 'Original', description: '' },
      });

      await vi.runAllTimersAsync();
      const initialEvents = wrapper.emitted('validation-change')?.length || 0;

      await wrapper.setProps({ integrationName: 'Updated' });
      await vi.runAllTimersAsync();
      await nextTick();

      const updatedEvents = wrapper.emitted('validation-change')?.length || 0;
      expect(updatedEvents).toBeGreaterThan(initialEvents);
    });
  });

  describe('CSS Classes', () => {
    it('applies error class to input when validation fails', async () => {
      const wrapper = mount(BasicDetailsStep, {
        props: {
          integrationName: '',
          description: '',
          normalizedNames: ['Duplicate'],
        },
      });

      const nameInput = wrapper.find('input.bd-input');
      await nameInput.setValue('duplicate');
      await vi.runAllTimersAsync();
      await nextTick();

      expect(nameInput.classes()).toContain('bd-input-error');
    });

    it('applies normal helper class when no error', async () => {
      const wrapper = mount(BasicDetailsStep, {
        props: { integrationName: 'Valid', description: '' },
      });

      await vi.runAllTimersAsync();
      await nextTick();

      const helper = wrapper.find('.bd-helper');
      expect(helper.classes()).toContain('bd-helper-normal');
    });

    it('applies error helper class when validation fails', async () => {
      const wrapper = mount(BasicDetailsStep, {
        props: { integrationName: '', description: '' },
      });

      const nameInput = wrapper.find('input.bd-input');
      await nameInput.setValue('Test');
      await nameInput.setValue('');
      await vi.runAllTimersAsync();
      await nextTick();

      const helper = wrapper.find('.bd-helper');
      expect(helper.classes()).toContain('bd-helper-error');
    });
  });
});
