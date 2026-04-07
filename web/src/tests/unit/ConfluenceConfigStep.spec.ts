import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import ConfluenceConfigStep from '@/components/outbound/confluenceintegration/wizard/steps/ConfluenceConfigStep.vue';
import type { ConfluencePageConfigData } from '@/types/ConfluenceFormData';

const hoisted = vi.hoisted(() => ({
  getAllActiveLanguagesMock: vi.fn(),
  getSpacesByConnectionIdMock: vi.fn(),
  getPagesByConnectionIdAndSpaceMock: vi.fn(),
}));

vi.mock('devextreme-vue/tag-box', () => ({
  DxTagBox: {
    name: 'DxTagBox',
    props: ['value'],
    emits: ['update:value'],
    template:
      '<input data-testid="langs-input" :value="(value || []).join(\',\')" @input="$emit(\'update:value\', String($event.target.value).split(\',\').filter(Boolean))" />',
  },
}));
vi.mock('devextreme-vue/switch', () => ({
  DxSwitch: {
    name: 'DxSwitch',
    props: ['value'],
    emits: ['value-changed'],
    template:
      '<button data-testid="toc-toggle" type="button" @click="$emit(\'value-changed\', { value: !value })">toggle</button>',
  },
}));
vi.mock('devextreme-vue/select-box', () => ({
  DxSelectBox: {
    name: 'DxSelectBox',
    props: ['value', 'dataSource', 'valueExpr', 'disabled'],
    emits: ['update:value', 'value-changed'],
    template: `
      <select
        data-testid="dx-select"
        :value="value"
        :disabled="disabled"
        @change="
          $emit('update:value', $event.target.value);
          $emit('value-changed', { value: $event.target.value });
        "
      >
        <option
          v-for="item in (dataSource || [])"
          :key="item[valueExpr || 'key'] || item.title || item.key || ''"
          :value="item[valueExpr || 'key'] || item.title || item.key || ''"
        >
          {{ item.displayText || item.name || item.title || item.key || '' }}
        </option>
      </select>
    `,
  },
}));

vi.mock('@/api/services/MasterDataService', () => ({
  MasterDataService: {
    getAllActiveLanguages: hoisted.getAllActiveLanguagesMock,
  },
}));

vi.mock('@/api/services/ConfluenceIntegrationService', () => ({
  ConfluenceIntegrationService: {
    getSpacesByConnectionId: hoisted.getSpacesByConnectionIdMock,
    getPagesByConnectionIdAndSpace: hoisted.getPagesByConnectionIdAndSpaceMock,
  },
}));

const createModelValue = (
  overrides: Partial<ConfluencePageConfigData> = {}
): ConfluencePageConfigData => ({
  confluenceSpaceKey: '',
  confluenceSpaceKeyFolderKey: '',
  languageCodes: ['en'],
  reportNameTemplate: 'Aggregated Daily Report - {date}',
  includeTableOfContents: true,
  ...overrides,
});

describe('ConfluenceConfigStep', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    hoisted.getAllActiveLanguagesMock.mockResolvedValue([
      { code: 'en', name: 'English', nativeName: 'English' },
      { code: 'fr', name: 'French', nativeName: 'Francais' },
    ]);
    hoisted.getSpacesByConnectionIdMock.mockResolvedValue([
      { key: 'SPACE1', name: 'Space One' },
      { key: 'SPACE2', name: 'Space Two' },
    ]);
    hoisted.getPagesByConnectionIdAndSpaceMock.mockResolvedValue([
      { id: '1', title: 'Folder A' },
      { id: '2', title: 'Folder B', parentTitle: 'Parent' },
    ]);
  });

  it('loads languages and spaces on mount when connection id is provided', async () => {
    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue({ confluenceSpaceKey: 'SPACE1' }),
        connectionId: 'conn-1',
      },
    });

    await flushPromises();

    expect(hoisted.getAllActiveLanguagesMock).toHaveBeenCalledTimes(1);
    expect(hoisted.getSpacesByConnectionIdMock).toHaveBeenCalledWith('conn-1');
    expect(hoisted.getPagesByConnectionIdAndSpaceMock).toHaveBeenCalledWith('conn-1', 'SPACE1');

    const validationEvents = wrapper.emitted('validation-change') ?? [];
    expect(validationEvents.length).toBeGreaterThan(0);
    expect(validationEvents.at(-1)).toEqual([true]);
  });

  it('emits invalid state when required fields are missing and shows required helper', async () => {
    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue({
          confluenceSpaceKey: '',
          languageCodes: [],
          reportNameTemplate: '',
        }),
      },
    });

    await flushPromises();

    expect(hoisted.getSpacesByConnectionIdMock).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('Complete the Confluence connection step to load spaces.');

    const validationEvents = wrapper.emitted('validation-change') ?? [];
    expect(validationEvents.at(-1)).toEqual([false]);
  });

  it('watches connection id changes and loads spaces when it becomes available', async () => {
    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue(),
      },
    });

    await flushPromises();
    expect(hoisted.getSpacesByConnectionIdMock).not.toHaveBeenCalled();

    await wrapper.setProps({ connectionId: 'conn-late' });
    await flushPromises();

    expect(hoisted.getSpacesByConnectionIdMock).toHaveBeenCalledWith('conn-late');
  });

  it('loads pages when selected space changes', async () => {
    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue({ confluenceSpaceKey: '' }),
        connectionId: 'conn-pages',
      },
    });

    await flushPromises();

    const selects = wrapper.findAll('[data-testid="dx-select"]');
    expect(selects.length).toBeGreaterThan(0);

    await selects[0].setValue('SPACE2');
    await flushPromises();

    expect(hoisted.getPagesByConnectionIdAndSpaceMock).toHaveBeenCalledWith('conn-pages', 'SPACE2');
  });

  it('shows retry for spaces error and retries load', async () => {
    hoisted.getSpacesByConnectionIdMock.mockRejectedValueOnce(new Error('spaces down'));

    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue(),
        connectionId: 'conn-error',
      },
    });

    await flushPromises();
    expect(wrapper.text()).toContain('spaces down');

    const retryButtons = wrapper.findAll('button.cc-retry-btn');
    expect(retryButtons.length).toBeGreaterThan(0);

    await retryButtons[0].trigger('click');
    await flushPromises();

    expect(hoisted.getSpacesByConnectionIdMock).toHaveBeenCalledTimes(2);
  });

  it('shows retry for pages error and retries load with current key', async () => {
    hoisted.getPagesByConnectionIdAndSpaceMock.mockRejectedValueOnce(new Error('pages down'));

    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue({ confluenceSpaceKey: 'SPACE1' }),
        connectionId: 'conn-pages-error',
      },
    });

    await flushPromises();
    expect(wrapper.text()).toContain('pages down');

    const retryButtons = wrapper.findAll('button.cc-retry-btn');
    expect(retryButtons.length).toBeGreaterThan(0);

    // In pages-error state this is the pages retry button
    await retryButtons.at(-1)!.trigger('click');
    await flushPromises();

    expect(hoisted.getPagesByConnectionIdAndSpaceMock).toHaveBeenCalledWith(
      'conn-pages-error',
      'SPACE1'
    );
    expect(hoisted.getPagesByConnectionIdAndSpaceMock).toHaveBeenCalledTimes(2);
  });

  it('sets folder key to ROOT when the user selects the space root option', async () => {
    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue({
          confluenceSpaceKey: 'SPACE1',
          confluenceSpaceKeyFolderKey: 'Folder A',
          confluenceSpaceFolderLabel: 'Folder A',
        }),
        connectionId: 'conn-root',
      },
    });

    await flushPromises();

    const selects = wrapper.findAll('[data-testid="dx-select"]');
    await selects[1].setValue('');
    await flushPromises();

    const updateEvents = wrapper.emitted('update:modelValue') ?? [];
    expect(updateEvents.length).toBeGreaterThan(0);
    const latestValue = updateEvents.at(-1)?.[0] as ConfluencePageConfigData;

    expect(latestValue.confluenceSpaceKeyFolderKey).toBe('ROOT');
    expect(latestValue.confluenceSpaceFolderLabel).toBe('');
  });
});
