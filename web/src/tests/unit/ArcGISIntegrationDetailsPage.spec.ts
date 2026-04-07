/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi } from 'vitest';

vi.mock('@/components/common/StandardDetailPageLayout.vue', () => ({
  default: {
    name: 'StandardDetailPageLayoutStub',
    props: ['title'],
    template: '<div class="standard-layout-stub">{{ title }}</div>',
  },
}));

vi.mock('@/components/outbound/arcgisintegration/wizard/ArcGISIntegrationWizard.vue', () => ({
  default: {
    name: 'ArcGISIntegrationWizardStub',
    props: ['open', 'mode', 'integrationId'],
    emits: ['close', 'integration-updated', 'integration-created'],
    template: '<div v-if="open" class="arcgis-wizard-stub">{{ mode }}</div>',
  },
}));

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: 'A1' } }),
  useRouter: () => ({ back: vi.fn() }),
}));

vi.mock('@/api/services/ArcGISIntegrationService', () => ({
  ArcGISIntegrationService: {
    getArcGISIntegrationById: vi
      .fn()
      .mockResolvedValue({ id: 'A1', name: 'Geo Import', isEnabled: true, version: 1 }),
  },
}));

vi.mock('devextreme-vue', () => ({
  DxButton: { name: 'DxButton', template: '<button />' },
  DxLoadPanel: { name: 'DxLoadPanel', template: '<div />' },
}));
vi.mock('@/components/common/Tooltip.vue', () => ({
  default: { name: 'CommonTooltip', template: '<div />' },
}));

import ArcGISIntegrationDetailsPage from '@/components/outbound/arcgisintegration/details/ArcGISIntegrationDetailsPage.vue';

describe('ArcGISIntegrationDetailsPage', () => {
  it('loads details and renders title', async () => {
    const setBreadcrumbTitle = vi.fn();
    const wrapper = mount(ArcGISIntegrationDetailsPage, {
      global: {
        provide: { setBreadcrumbTitle },
      },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('Geo Import');
    expect(setBreadcrumbTitle).toHaveBeenCalledWith('ArcGIS Integration Details');
  });

  it('shows error UI on service failure', async () => {
    const { ArcGISIntegrationService } = await import('@/api/services/ArcGISIntegrationService');
    (ArcGISIntegrationService.getArcGISIntegrationById as any).mockRejectedValueOnce(
      new Error('nope')
    );

    const wrapper = mount(ArcGISIntegrationDetailsPage, {
      global: { provide: { setBreadcrumbTitle: vi.fn() } },
    });
    await new Promise(r => setTimeout(r, 0));
    // With the StandardDetailPageLayout stub, we assert the computed title
    expect(wrapper.text()).toContain('Error loading integration');
  });

  it('handles clone-requested event and opens clone wizard', async () => {
    const wrapper = mount(ArcGISIntegrationDetailsPage, {
      global: {
        provide: { setBreadcrumbTitle: vi.fn() },
      },
    });
    await new Promise(r => setTimeout(r, 0));

    // Get the component instance to call internal handlers
    const vm = wrapper.vm as any;

    // Simulate clone-requested event from BasicDetailsTab
    vm.openCloneWizard();

    // Verify clone wizard state is set
    expect(vm.cloneWizardOpen).toBe(true);
    expect(vm.cloningIntegrationId).toBe('A1');
  });
});
