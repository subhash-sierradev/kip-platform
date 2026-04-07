/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi } from 'vitest';
import { defineComponent } from 'vue';

// Mock DevExtreme Button component to ensure it renders as a simple button
vi.mock('devextreme-vue/button', () => {
  return {
    DxButton: defineComponent({
      name: 'DxButton',
      template: '<button class="dx-btn" @click="$emit(\'click\')"></button>',
    }),
  };
});

vi.mock('@/components/common/ExportGridData', () => ({
  exportGridData: vi.fn().mockResolvedValue(undefined),
}));
import { exportGridData } from '@/components/common/ExportGridData';

import GenericDataGrid from '@/components/common/GenericDataGrid.vue';

describe('GenericDataGrid', () => {
  const data = [
    { id: 1, name: 'A' },
    { id: 2, name: 'B' },
  ];
  const columns = [
    { dataField: 'id', caption: 'ID' },
    { dataField: 'name', caption: 'Name' },
  ];
  const exportConfig = {
    headers: ['ID', 'Name'],
    pickFields: (row: any) => [row.id, row.name],
    filenamePrefix: 'test',
  };

  const stubs = {
    DxDataGrid: { template: '<div class="dx-grid-stub"><slot /></div>' },
    DxColumn: true,
    DxPaging: true,
    DxPager: true,
    DxHeaderFilter: true,
    DxButton: {
      name: 'DxButton',
      template: '<button class="dx-btn" @click="$emit(\'click\')"></button>',
    },
  } as const;

  it('shows toolbar buttons when enabled and triggers export', async () => {
    const wrapper = mount(GenericDataGrid, {
      props: { data, columns, enableExport: true, enableClearFilters: true, exportConfig },
      global: { stubs },
    });
    const btns = wrapper.findAll('button.dx-btn');
    expect(btns.length).toBe(2);

    await btns[0].trigger('click');
    expect(exportGridData).toHaveBeenCalled();
    const args = (exportGridData as any).mock.calls[0][0];
    expect(args.headers).toEqual(['ID', 'Name']);
    expect(args.filenamePrefix).toBe('test');
    expect(args.fallbackData.length).toBe(2);
  });

  it('shows only export button when enableExport is true and enableClearFilters is false', () => {
    const wrapper = mount(GenericDataGrid, {
      props: { data, columns, enableExport: true, enableClearFilters: false, exportConfig },
      global: { stubs },
    });
    const btns = wrapper.findAll('button.dx-btn');
    expect(btns.length).toBe(1);
  });

  it('shows only clear filters button when enableClearFilters is true and enableExport is false', () => {
    const wrapper = mount(GenericDataGrid, {
      props: { data, columns, enableExport: false, enableClearFilters: true },
      global: { stubs },
    });
    const btns = wrapper.findAll('button.dx-btn');
    expect(btns.length).toBe(1);
  });

  it('hides toolbar when both enableExport and enableClearFilters are false', () => {
    const wrapper = mount(GenericDataGrid, {
      props: { data, columns, enableExport: false, enableClearFilters: false },
      global: { stubs },
    });
    const toolbar = wrapper.find('.generic-grid-toolbar');
    expect(toolbar.exists()).toBe(false);
  });

  it('renders column without dataField (caption-only column)', () => {
    const captionOnlyColumns = [
      { caption: 'Action', template: 'actionSlot' },
      { dataField: 'name', caption: 'Name' },
    ];
    const wrapper = mount(GenericDataGrid, {
      props: { data, columns: captionOnlyColumns },
      global: { stubs },
    });
    expect(wrapper.exists()).toBe(true);
  });

  it('handles template rendering with undefined slot gracefully', () => {
    const templateColumns = [
      { dataField: 'id', caption: 'ID', template: 'nonExistentSlot' },
      { dataField: 'name', caption: 'Name' },
    ];
    const wrapper = mount(GenericDataGrid, {
      props: { data, columns: templateColumns },
      global: { stubs },
    });
    expect(wrapper.exists()).toBe(true);
  });

  it('emits option-changed event when grid options change', async () => {
    const wrapper = mount(GenericDataGrid, {
      props: { data, columns },
      global: { stubs },
    });

    // Simulate option change by calling the emitOptionChanged function directly
    const component = wrapper.vm as any;
    const mockEvent = { name: 'paging.pageSize', value: 20 };
    component.emitOptionChanged(mockEvent);

    await wrapper.vm.$nextTick();
    expect(wrapper.emitted('option-changed')).toBeTruthy();
    expect(wrapper.emitted('option-changed')?.[0]).toEqual([mockEvent]);
  });

  it('handles column with HeaderFilter dataSource', () => {
    const columnsWithFilter = [
      {
        dataField: 'status',
        caption: 'Status',
        headerFilter: {
          dataSource: [
            { text: 'Active', value: true },
            { text: 'Inactive', value: false },
          ],
        },
      },
      { dataField: 'name', caption: 'Name' },
    ];
    const wrapper = mount(GenericDataGrid, {
      props: { data, columns: columnsWithFilter },
      global: { stubs },
    });
    expect(wrapper.exists()).toBe(true);
  });

  it('handles column with empty HeaderFilter dataSource', () => {
    const columnsWithEmptyFilter = [
      {
        dataField: 'status',
        caption: 'Status',
        headerFilter: { dataSource: [] },
      },
    ];
    const wrapper = mount(GenericDataGrid, {
      props: { data, columns: columnsWithEmptyFilter },
      global: { stubs },
    });
    expect(wrapper.exists()).toBe(true);
  });

  it('handles column visibility toggling', () => {
    const visibilityColumns = [
      { dataField: 'id', caption: 'ID', visible: false },
      { dataField: 'name', caption: 'Name', visible: true },
    ];
    const wrapper = mount(GenericDataGrid, {
      props: { data, columns: visibilityColumns },
      global: { stubs },
    });
    expect(wrapper.exists()).toBe(true);
  });

  it('handles column alignment variations', () => {
    const alignmentColumns = [
      { dataField: 'id', caption: 'ID', alignment: 'left' as const },
      { dataField: 'count', caption: 'Count', alignment: 'center' as const },
      { dataField: 'amount', caption: 'Amount', alignment: 'right' as const },
    ];
    const wrapper = mount(GenericDataGrid, {
      props: { data, columns: alignmentColumns },
      global: { stubs },
    });
    expect(wrapper.exists()).toBe(true);
  });

  it('handles column without allowSorting specified (defaults to true)', () => {
    const defaultSortingColumns = [
      { dataField: 'id', caption: 'ID' }, // allowSorting not specified
      { dataField: 'name', caption: 'Name', allowSorting: false },
    ];
    const wrapper = mount(GenericDataGrid, {
      props: { data, columns: defaultSortingColumns },
      global: { stubs },
    });
    expect(wrapper.exists()).toBe(true);
  });

  it('handles column without allowFiltering specified (defaults to true)', () => {
    const defaultFilteringColumns = [
      { dataField: 'id', caption: 'ID' }, // allowFiltering not specified
      { dataField: 'name', caption: 'Name', allowFiltering: false },
    ];
    const wrapper = mount(GenericDataGrid, {
      props: { data, columns: defaultFilteringColumns },
      global: { stubs },
    });
    expect(wrapper.exists()).toBe(true);
  });

  it('uses custom pageSize and pageIndex props', () => {
    const wrapper = mount(GenericDataGrid, {
      props: { data, columns, pageSize: 20, pageIndex: 2 },
      global: { stubs },
    });
    const component = wrapper.vm as any;
    expect(component.resolvedPageSize).toBe(20);
    expect(component.resolvedPageIndex).toBe(2);
  });

  it('handles clear filters button click', async () => {
    const mockClearFilter = vi.fn();
    const mockClearSorting = vi.fn();

    const wrapper = mount(GenericDataGrid, {
      props: { data, columns, enableClearFilters: true },
      global: { stubs },
    });

    // Mock the grid instance
    const component = wrapper.vm as any;
    component.gridRef = {
      instance: {
        clearFilter: mockClearFilter,
        clearSorting: mockClearSorting,
      },
    };

    await wrapper.find('button.dx-btn').trigger('click');
    expect(mockClearFilter).toHaveBeenCalled();
    expect(mockClearSorting).toHaveBeenCalled();
  });
});
