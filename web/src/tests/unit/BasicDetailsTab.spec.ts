/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi } from 'vitest';
import BasicDetailsTab from '@/components/outbound/arcgisintegration/details/BasicDetailsTab.vue';

vi.mock('devextreme-vue', () => ({
  DxButton: { props: ['text'], template: '<button><slot />{{ text }}</button>' },
}));

vi.mock('@/components/common/ConfirmationDialog.vue', () => ({
  default: { template: '<div />' },
}));

// Stub EntityManageActions to expose buttons matching legacy selectors
vi.mock('@/components/common/EntityManageActions.vue', () => ({
  default: {
    props: ['actions', 'title'],
    emits: ['action'],
    template: `
      <div class="actions-stub">
        <button
          class="action-button toggle-btn"
          @click="$emit('action', 'toggle')"
        >{{ (actions || []).find(x => x.id === 'toggle')?.label || 'Toggle' }}</button>
        <button
          class="action-button clone-btn"
          @click="$emit('action', 'clone'); $parent.$emit('refresh')"
        >{{ (actions || []).find(x => x.id === 'clone')?.label || 'Clone' }}</button>
      </div>
    `,
  },
}));

vi.mock('@/composables/useArcGISIntegrationActions', () => ({
  useArcGISIntegrationActions: () => ({
    deleteIntegration: vi.fn(async () => true),
    cloneIntegration: vi.fn(async () => 'new-id'),
  }),
  useArcGISIntegrationEditor: () => ({
    loading: false,
    updateIntegrationFromWizard: vi.fn().mockResolvedValue(true),
  }),
  useArcGISIntegrationStatus: () => ({
    toggleIntegrationStatus: vi.fn(async () => true),
  }),
  useArcGISIntegrationTrigger: () => ({
    triggerJobExecution: vi.fn().mockResolvedValue(undefined),
  }),
}));

vi.mock('@/store/toast', () => ({
  useToastStore: () => ({ showInfo: vi.fn(), showError: vi.fn() }),
}));

describe('BasicDetailsTab', () => {
  const integration = {
    name: 'Roads Integration',
    description: 'Sync roads from ArcGIS',
    createdBy: 'Alice',
    createdDate: '2024-01-01T00:00:00Z',
    lastModifiedBy: 'Bob',
    lastModifiedDate: '2024-01-02T00:00:00Z',
    itemType: 'Document',
    itemSubtypeLabel: 'Subtype-A',
    dynamicDocumentType: 'DynamicType',
    dynamicDocumentTypeLabel: 'DynamicType',
    isEnabled: true,
  } as any;

  it('renders integration info and configuration', () => {
    const wrapper = mount(BasicDetailsTab, {
      props: { integrationData: integration, integrationId: 'abc', loading: false },
    });

    expect(wrapper.text()).toContain('Roads Integration');
    expect(wrapper.text()).toContain('Sync roads from ArcGIS');
    expect(wrapper.text()).toContain('Alice');
    expect(wrapper.text()).toContain('Bob');
    // Document type shows itemType or dynamicDocumentType
    expect(wrapper.text()).toContain('Document');
    expect(wrapper.text()).toContain('Subtype-A');
    expect(wrapper.text()).toContain('DynamicType');

    // Toggle button reflects enabled state
    const toggleBtn = wrapper.find('button.action-button.toggle-btn');
    expect(toggleBtn.text()).toContain('Disable Integration');
  });

  it('shows dynamicDocumentType when label is null', () => {
    const wrapper = mount(BasicDetailsTab, {
      props: {
        integrationData: {
          ...integration,
          dynamicDocumentType: 'A Test for today',
          dynamicDocumentTypeLabel: null,
        },
        integrationId: 'abc',
        loading: false,
      },
    });

    expect(wrapper.text()).toContain('Dynamic Document Type');
    expect(wrapper.text()).toContain('A Test for today');
  });

  it('emits clone-requested on clone action', async () => {
    const wrapper = mount(BasicDetailsTab, {
      props: { integrationData: integration, integrationId: 'abc', loading: false },
    });
    const cloneBtn = wrapper.find('button.action-button.clone-btn');
    await cloneBtn.trigger('click');
    // Verify clone-requested emit was emitted
    expect(wrapper.emitted('clone-requested')).toBeTruthy();
  });

  it('shows empty state when no data and not loading', () => {
    const wrapper = mount(BasicDetailsTab, {
      props: { integrationData: null, integrationId: 'abc', loading: false },
    });
    expect(wrapper.find('.empty-state').exists()).toBe(true);
    expect(wrapper.text()).toContain('No Integration Data Found');
  });
});
