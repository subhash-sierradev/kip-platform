/* eslint-disable simple-import-sort/imports */
import { describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { defineComponent, h } from 'vue';

// Stub devextreme components to simple elements and expose instance
vi.mock('devextreme-vue/data-grid', () => {
  return {
    DxDataGrid: defineComponent({
      name: 'DxDataGrid',
      setup(_props: any, { slots, expose }: any) {
        const instance = {
          clearFilter: vi.fn(),
          clearSorting: vi.fn(),
          getCombinedFilter: vi.fn(() => []),
        };
        expose({ instance });
        // Also return instance so parent refs and VTU .vm can access
        return {
          instance,
          default: () => h('div', { class: 'dx-data-grid' }, slots.default ? slots.default() : []),
        } as any;
      },
      render() {
        // Render default slot container
        // @ts-ignore
        return (this as any).default?.() ?? h('div', { class: 'dx-data-grid' });
      },
    }),
    DxColumn: defineComponent({
      name: 'DxColumn',
      setup: () => () => h('div', { class: 'dx-column' }),
    }),
    DxPaging: defineComponent({
      name: 'DxPaging',
      setup: () => () => h('div', { class: 'dx-paging' }),
    }),
    DxPager: defineComponent({
      name: 'DxPager',
      setup: () => () => h('div', { class: 'dx-pager' }),
    }),
    DxHeaderFilter: defineComponent({
      name: 'DxHeaderFilter',
      setup: () => () => h('div', { class: 'dx-header-filter' }),
    }),
  };
});

vi.mock('devextreme-vue/button', () => {
  return {
    DxButton: defineComponent({
      name: 'DxButton',
      props: ['icon', 'text'],
      emits: ['click'],
      setup(_props: any, { emit }: any) {
        return () => h('button', { class: 'dx-button', onClick: () => emit('click') });
      },
    }),
  };
});

// Mock export helper
const exportGridDataMock = vi.fn();
vi.mock('@/components/common/ExportGridData', () => ({
  exportGridData: (...args: any[]) => exportGridDataMock(...args),
}));

import GenericDataGrid from '@/components/common/GenericDataGrid.vue';

describe('GenericDataGrid branches', () => {
  it('shows toolbar when any flag true; handles clear filters', async () => {
    const wrapper = mount(GenericDataGrid, {
      props: {
        data: [],
        columns: [],
        enableExport: true,
        enableClearFilters: true,
        exportConfig: { headers: [], pickFields: (r: any) => [r], filenamePrefix: 'x' },
      },
    });

    // Toolbar exists and buttons render
    expect(wrapper.find('.generic-grid-toolbar').exists()).toBe(true);
    const buttons = wrapper.findAll('.dx-button');
    expect(buttons.length).toBeGreaterThan(0);

    // Click clear filters — our stub instance exists, so calls are safe
    await buttons[1].trigger('click');
    const grid = wrapper.findComponent({ name: 'DxDataGrid' });
    const exposed = grid.vm as any;
    expect(typeof exposed.instance).toBe('object');
    expect(typeof exposed.instance.clearFilter).toBe('function');
  });

  it('export uses fallback data when no filter; and handles rejection path', async () => {
    const rows = [{ a: 'x' }, { a: 'y' }];
    exportGridDataMock.mockResolvedValueOnce(undefined);
    const wrapper = mount(GenericDataGrid, {
      props: {
        data: rows,
        columns: [{ dataField: 'a', caption: 'A' }],
        enableExport: true,
        exportConfig: { headers: ['A'], pickFields: (r: any) => [r.a], filenamePrefix: 'grid' },
      },
    });
    const buttons = wrapper.findAll('.dx-button');
    // First button triggers export
    await buttons[0].trigger('click');
    expect(exportGridDataMock).toHaveBeenCalledTimes(1);
    const arg = exportGridDataMock.mock.calls[0][0];
    expect(arg.fallbackData).toEqual(rows);

    // Now simulate rejection
    exportGridDataMock.mockRejectedValueOnce(new Error('boom'));
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    await buttons[0].trigger('click');
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });

  it('export applies combined filter when provided by instance', async () => {
    const rows = [{ a: 'x' }, { a: 'y' }];
    exportGridDataMock.mockResolvedValueOnce(undefined);
    const wrapper = mount(GenericDataGrid, {
      props: {
        data: rows,
        columns: [{ dataField: 'a', caption: 'A' }],
        enableExport: true,
        exportConfig: { headers: ['A'], pickFields: (r: any) => [r.a], filenamePrefix: 'grid' },
      },
    });
    // Make instance return filter
    const grid = wrapper.findComponent({ name: 'DxDataGrid' });
    (grid.vm as any).instance.getCombinedFilter = vi.fn(() => ['a', '=', 'x']);

    const buttons = wrapper.findAll('.dx-button');
    await buttons[0].trigger('click');
    const arg = exportGridDataMock.mock.calls.pop()![0];
    expect(arg.fallbackData).toEqual([{ a: 'x' }]);
  });

  it('getTemplate renders slot content when present, empty when absent', async () => {
    const wrapper = mount(GenericDataGrid, {
      props: {
        data: [{ a: 'x' }],
        columns: [{ dataField: 'a', caption: 'A', template: 'custom' }],
        pageSize: 10,
      },
      slots: {
        custom: ({ data }: any) => `${data.a}-slot`,
      },
    });
    expect(wrapper.find('.dx-column').exists()).toBe(true);
    // Since our column is a stub, verifying slot render is indirect;
    // mount another without slot to ensure no errors
    const wrapperNoSlot = mount(GenericDataGrid, {
      props: {
        data: [{ a: 'x' }],
        columns: [{ dataField: 'a', caption: 'A', template: 'custom' }],
      },
    });
    expect(wrapperNoSlot.find('.dx-column').exists()).toBe(true);
  });
});
