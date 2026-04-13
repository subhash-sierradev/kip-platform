import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';

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

  it('falls back to the raw space key when no matching space option exists', async () => {
    hoisted.getSpacesByConnectionIdMock.mockResolvedValueOnce([]);

    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue({ confluenceSpaceKey: '' }),
        connectionId: 'conn-fallback',
      },
    });

    await flushPromises();

    (wrapper.vm as any).localData.confluenceSpaceKey = 'UNKNOWN_SPACE';
    await flushPromises();

    expect((wrapper.vm as any).localData.confluenceSpaceKey).toBe('UNKNOWN_SPACE');
    expect((wrapper.vm as any).localData.confluenceSpaceLabel).toBe('UNKNOWN_SPACE');
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

  it('keeps the raw folder id as the label when the selected folder cannot be resolved', async () => {
    hoisted.getPagesByConnectionIdAndSpaceMock.mockResolvedValueOnce([]);

    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue({
          confluenceSpaceKey: 'SPACE1',
          confluenceSpaceKeyFolderKey: '',
        }),
        connectionId: 'conn-folder-fallback',
      },
    });

    await flushPromises();

    (wrapper.vm as any).onFolderChanged({ value: 'folder-99' });
    await flushPromises();

    expect((wrapper.vm as any).localData.confluenceSpaceKeyFolderKey).toBe('folder-99');
    expect((wrapper.vm as any).localData.confluenceSpaceFolderLabel).toBe('folder-99');
  });

  it('shows language retry errors and retries loading languages', async () => {
    hoisted.getAllActiveLanguagesMock.mockRejectedValueOnce({ body: { message: 'langs down' } });

    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue(),
      },
    });

    await flushPromises();

    expect(wrapper.text()).toContain('langs down');

    await wrapper.find('button.cc-retry-btn').trigger('click');
    await flushPromises();

    expect(hoisted.getAllActiveLanguagesMock).toHaveBeenCalledTimes(2);
  });

  it('renders loading helpers while languages, spaces, and pages are loading', async () => {
    hoisted.getAllActiveLanguagesMock.mockImplementationOnce(() => new Promise(() => {}));
    hoisted.getSpacesByConnectionIdMock.mockImplementationOnce(() => new Promise(() => {}));
    hoisted.getPagesByConnectionIdAndSpaceMock.mockImplementationOnce(() => new Promise(() => {}));

    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue({ confluenceSpaceKey: 'SPACE1' }),
        connectionId: 'conn-loading',
      },
    });

    await nextTick();

    expect(wrapper.text()).toContain('Loading languages');
    expect(wrapper.text()).toContain('Loading spaces');
    expect(wrapper.text()).toContain('Loading folders');
  });

  it('resolves existing space and folder labels after loading remote options', async () => {
    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue({
          confluenceSpaceKey: 'SPACE1',
          confluenceSpaceLabel: 'SPACE1',
          confluenceSpaceKeyFolderKey: '2',
          confluenceSpaceFolderLabel: '2',
        }),
        connectionId: 'conn-labels',
      },
    });

    await flushPromises();

    const updates = wrapper.emitted('update:modelValue') ?? [];
    const latestValue = updates.at(-1)?.[0] as ConfluencePageConfigData;

    expect(latestValue.confluenceSpaceLabel).toBe('Space One (SPACE1)');
    expect(latestValue.confluenceSpaceFolderLabel).toBe('Parent > Folder B');
  });

  it('clears the space key error after a valid space is selected', async () => {
    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue({
          confluenceSpaceKey: '',
          languageCodes: ['en'],
          reportNameTemplate: 'Template',
        }),
        connectionId: 'conn-valid-space',
      },
    });

    await flushPromises();
    (wrapper.vm as any).validateSpaceKey();
    await flushPromises();
    expect(wrapper.text()).toContain('Space Key is required.');

    const selects = wrapper.findAll('[data-testid="dx-select"]');
    await selects[0].setValue('SPACE1');
    await flushPromises();

    expect(wrapper.text()).not.toContain('Space Key is required.');
  });

  it('updates the table-of-contents label and hides the preview for blank templates', async () => {
    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue({ reportNameTemplate: '   ', includeTableOfContents: true }),
      },
    });

    await flushPromises();
    expect(wrapper.find('.cc-preview').exists()).toBe(false);

    await wrapper.find('[data-testid="toc-toggle"]').trigger('click');
    await flushPromises();

    expect((wrapper.vm as any).localData.includeTableOfContents).toBe(false);
    expect(wrapper.text()).toContain('Disabled');
  });

  it('clears pages and space labels when the selected space is removed', async () => {
    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue({
          confluenceSpaceKey: 'SPACE1',
          confluenceSpaceLabel: 'Space One (SPACE1)',
          confluenceSpaceKeyFolderKey: '2',
          confluenceSpaceFolderLabel: 'Parent > Folder B',
        }),
        connectionId: 'conn-clear',
      },
    });

    await flushPromises();

    const selects = wrapper.findAll('[data-testid="dx-select"]');
    await selects[0].setValue('');
    await flushPromises();

    expect(wrapper.text()).toContain('Space Key is required.');

    const updates = wrapper.emitted('update:modelValue') ?? [];
    const latestValue = updates.at(-1)?.[0] as ConfluencePageConfigData;
    expect(latestValue.confluenceSpaceLabel).toBe('');
    expect(latestValue.confluenceSpaceKeyFolderKey).toBe('ROOT');
    expect(latestValue.confluenceSpaceFolderLabel).toBe('');
  });

  it('does not attempt to load spaces or pages when mounted without a connection id', async () => {
    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue({
          confluenceSpaceKey: 'SPACE1',
          confluenceSpaceKeyFolderKey: 'ROOT',
        }),
      },
    });

    await flushPromises();

    expect(hoisted.getAllActiveLanguagesMock).toHaveBeenCalledTimes(1);
    expect(hoisted.getSpacesByConnectionIdMock).not.toHaveBeenCalled();
    expect(hoisted.getPagesByConnectionIdAndSpaceMock).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('Complete the Confluence connection step to load spaces.');
  });

  it('resolves the space root folder label when existing folder key matches the selected space', async () => {
    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue({
          confluenceSpaceKey: 'SPACE1',
          confluenceSpaceKeyFolderKey: 'SPACE1',
          confluenceSpaceFolderLabel: 'Old Folder',
        }),
        connectionId: 'conn-root-label',
      },
    });

    await flushPromises();

    const updates = wrapper.emitted('update:modelValue') ?? [];
    const latestValue = updates.at(-1)?.[0] as ConfluencePageConfigData;
    expect(latestValue.confluenceSpaceKeyFolderKey).toBe('ROOT');
    expect(latestValue.confluenceSpaceFolderLabel).toBe('');
  });

  it('updates local state when modelValue changes from the parent', async () => {
    const wrapper = mount(ConfluenceConfigStep, {
      props: {
        modelValue: createModelValue({
          reportNameTemplate: 'Initial {date}',
          languageCodes: ['en'],
        }),
      },
    });

    await flushPromises();

    await wrapper.setProps({
      modelValue: createModelValue({
        reportNameTemplate: 'Updated {date}',
        languageCodes: ['fr'],
      }),
    });
    await flushPromises();

    const reportInput = wrapper.find('input.cc-input');
    expect((reportInput.element as HTMLInputElement).value).toBe('Updated {date}');
  });
});
