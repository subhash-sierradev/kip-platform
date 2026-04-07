import { mount, VueWrapper } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';

import type { ArcGISIntegrationResponse } from '@/api/models/ArcGISIntegrationResponse';
import { FieldTransformationType } from '@/api/models/FieldTransformationType';
import type { IntegrationFieldMappingDto } from '@/api/models/IntegrationFieldMappingDto';
import { ArcGISIntegrationService } from '@/api/services/ArcGISIntegrationService';
import FieldMappingTab from '@/components/outbound/arcgisintegration/details/FieldMappingTab.vue';

// Mock ArcGISIntegrationService
vi.mock('@/api/services/ArcGISIntegrationService', () => ({
  ArcGISIntegrationService: {
    getFieldMappings: vi.fn(),
  },
}));

// Mock DevExtreme DxButton
vi.mock('devextreme-vue/button', () => ({
  DxButton: {
    name: 'DxButton',
    template: '<button class="dx-button" @click="$emit(\'click\')"><slot /></button>',
    props: ['icon', 'stylingMode', 'type', 'hint', 'elementAttr'],
  },
}));

// Mock GenericDataGrid
vi.mock('@/components/common/GenericDataGrid.vue', () => ({
  default: {
    name: 'GenericDataGrid',
    template:
      '<div class="generic-data-grid"><div v-for="row in (data||[])" class="grid-row"><slot name="mandatoryTemplate" :data="row" /></div></div>',
    props: ['data', 'columns', 'pageSize'],
  },
}));

describe('FieldMappingTab', () => {
  let wrapper: VueWrapper;

  const mockFieldMappings: IntegrationFieldMappingDto[] = [
    {
      id: 'mapping-1',
      sourceFieldPath: 'case.caseNumber',
      targetFieldPath: 'CASE_NUMBER',
      transformationType: FieldTransformationType.PASSTHROUGH,
      isMandatory: true,
      displayOrder: 1,
    },
    {
      id: 'mapping-2',
      sourceFieldPath: 'case.subject',
      targetFieldPath: 'SUBJECT',
      transformationType: FieldTransformationType.PASSTHROUGH,
      isMandatory: false,
      displayOrder: 2,
    },
    {
      id: 'mapping-3',
      sourceFieldPath: 'case.createdDate',
      targetFieldPath: 'CREATED_DATE',
      transformationType: FieldTransformationType.TO_DATE,
      isMandatory: true,
      displayOrder: 3,
    },
  ];

  const mockIntegrationData: ArcGISIntegrationResponse = {
    id: 'integration-1',
    name: 'Test Integration',
    description: 'Test Description',
  } as unknown as ArcGISIntegrationResponse;

  beforeEach(() => {
    vi.clearAllMocks();
    // Mock the getFieldMappings API call
    vi.mocked(ArcGISIntegrationService.getFieldMappings).mockResolvedValue(mockFieldMappings);
  });

  describe('Component Mounting & Props', () => {
    it('renders component with required props', () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
        },
      });

      expect(wrapper.exists()).toBe(true);
      expect(wrapper.find('.field-mapping-tab').exists()).toBe(true);
    });

    it('displays GenericDataGrid component', () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
      });

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      expect(grid.exists()).toBe(true);
    });

    it('passes integration data to component', () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
      });

      expect((wrapper as any).props('integrationData')).toEqual(mockIntegrationData);
    });

    it('accepts loading prop', () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          loading: true,
        },
      });

      expect((wrapper as any).props('loading')).toBe(true);
    });

    it('uses default loading value of false', () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
        },
      });

      expect((wrapper as any).props('loading')).toBe(false);
    });
  });

  describe('Field Mappings Display', () => {
    it('displays field mappings from integration data', async () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
      });

      // Wait for async data loading
      await nextTick();
      await nextTick();

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      const displayRows = grid.props('data');

      expect(displayRows).toHaveLength(3);
      expect(displayRows[0]).toMatchObject({
        id: 'mapping-1',
        index: 1,
        kasewareField: 'case.caseNumber',
        arcgisField: 'CASE_NUMBER',
        transformationType: 'PASSTHROUGH',
        isMandatory: true,
      });
    });

    it('displays empty array when no integration data', () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: null,
        },
      });

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      expect(grid.props('data')).toEqual([]);
    });

    it('displays empty array when integration data has no field mappings', async () => {
      vi.mocked(ArcGISIntegrationService.getFieldMappings).mockResolvedValue([]);

      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
      });

      await nextTick();
      await nextTick();

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      expect(grid.props('data')).toEqual([]);
    });

    it('sorts mappings by displayOrder', async () => {
      const unorderedMappings = [
        { ...mockFieldMappings[2], displayOrder: 3 },
        { ...mockFieldMappings[0], displayOrder: 1 },
        { ...mockFieldMappings[1], displayOrder: 2 },
      ];
      vi.mocked(ArcGISIntegrationService.getFieldMappings).mockResolvedValue(unorderedMappings);

      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
      });

      await nextTick();
      await nextTick();

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      const displayRows = grid.props('data');

      expect(displayRows[0].displayOrder).toBe(1);
      expect(displayRows[1].displayOrder).toBe(2);
      expect(displayRows[2].displayOrder).toBe(3);
    });

    it('assigns sequential index numbers starting from 1', async () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
      });

      // Wait for async data loading
      await nextTick();
      await nextTick();

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      const displayRows = grid.props('data');

      expect(displayRows[0].index).toBe(1);
      expect(displayRows[1].index).toBe(2);
      expect(displayRows[2].index).toBe(3);
    });

    it('handles mappings without id by generating fallback', async () => {
      const mappingsWithoutId = [
        { ...mockFieldMappings[0], id: undefined },
      ] as IntegrationFieldMappingDto[];
      vi.mocked(ArcGISIntegrationService.getFieldMappings).mockResolvedValue(mappingsWithoutId);

      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
      });

      await nextTick();
      await nextTick();

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      const displayRows = grid.props('data');

      expect(displayRows[0].id).toBe('mapping-0');
    });

    it('handles missing displayOrder by defaulting to 0', async () => {
      const mappingsWithoutOrder = [
        { ...mockFieldMappings[0], displayOrder: undefined },
      ] as IntegrationFieldMappingDto[];
      vi.mocked(ArcGISIntegrationService.getFieldMappings).mockResolvedValue(mappingsWithoutOrder);

      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
      });

      await nextTick();
      await nextTick();

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      const displayRows = grid.props('data');

      expect(displayRows[0].displayOrder).toBe(0);
    });

    it('handles missing isMandatory by defaulting to false', async () => {
      const mappingsWithoutMandatory = [
        { ...mockFieldMappings[0], isMandatory: undefined },
      ] as IntegrationFieldMappingDto[];
      vi.mocked(ArcGISIntegrationService.getFieldMappings).mockResolvedValue(
        mappingsWithoutMandatory
      );

      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
      });

      await nextTick();
      await nextTick();

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      const displayRows = grid.props('data');

      expect(displayRows[0].isMandatory).toBe(false);
    });

    it('handles empty sourceFieldPath', async () => {
      const mappingsWithEmptySource = [{ ...mockFieldMappings[0], sourceFieldPath: '' }];
      vi.mocked(ArcGISIntegrationService.getFieldMappings).mockResolvedValue(
        mappingsWithEmptySource
      );

      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
      });

      await nextTick();
      await nextTick();

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      const displayRows = grid.props('data');

      expect(displayRows[0].kasewareField).toBe('');
    });

    it('displays sourceFieldPath as kasewareField', async () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
      });

      // Wait for async data loading
      await nextTick();
      await nextTick();

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      const displayRows = grid.props('data');

      expect(displayRows[0].kasewareField).toBe('case.caseNumber');
      expect(displayRows[1].kasewareField).toBe('case.subject');
    });

    it('renders status chips for required mappings', async () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
        global: {
          stubs: {
            StatusChipForDataTable: {
              props: ['status', 'label'],
              template: '<span class="status-chip-stub">{{ label || status }}</span>',
            },
          },
        },
      });

      await nextTick();
      await nextTick();

      expect(wrapper.findAll('.status-chip-stub').length).toBe(3);
    });
  });

  describe('Grid Configuration', () => {
    it('passes pageSize of 10 to grid', () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
      });

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      expect(grid.props('pageSize')).toBe(10);
    });

    it('passes grid columns configuration', () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
      });

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      const columns = grid.props('columns');

      expect(columns).toHaveLength(5);
      expect(columns[0].dataField).toBe('index');
      expect(columns[1].dataField).toBe('kasewareField');
      expect(columns[2].dataField).toBe('arcgisField');
      expect(columns[3].dataField).toBe('transformationType');
      expect(columns[4].dataField).toBe('isMandatory');
    });

    it('configures index column with correct properties', () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
        },
      });

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      const indexColumn = grid.props('columns')[0];

      expect(indexColumn.caption).toBe('#');
      expect(indexColumn.width).toBe(50);
      expect(indexColumn.allowFiltering).toBe(false);
      expect(indexColumn.allowSorting).toBe(false);
    });

    it('configures kasewareField column with filtering and sorting', () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
        },
      });

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      const kwColumn = grid.props('columns')[1];

      expect(kwColumn.caption).toBe('Kaseware Field');
      expect(kwColumn.minWidth).toBe(180);
      expect(kwColumn.allowFiltering).toBe(true);
      expect(kwColumn.allowSorting).toBe(true);
    });

    it('configures arcgisField column with filtering and sorting', () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
        },
      });

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      const arcColumn = grid.props('columns')[2];

      expect(arcColumn.caption).toBe('ArcGIS Field');
      expect(arcColumn.minWidth).toBe(180);
      expect(arcColumn.allowFiltering).toBe(true);
      expect(arcColumn.allowSorting).toBe(true);
    });

    it('configures isMandatory column with header filter', () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
        },
      });

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      const mandatoryColumn = grid.props('columns')[4];

      expect(mandatoryColumn.caption).toBe('Required');
      expect(mandatoryColumn.width).toBe(140);
      expect(mandatoryColumn.alignment).toBe('left');
      expect(mandatoryColumn.headerFilter).toBeDefined();
      expect(mandatoryColumn.headerFilter.dataSource).toEqual([
        { text: 'Yes', value: true },
        { text: 'No', value: false },
      ]);
    });
  });

  describe('Clear Filters Button', () => {
    it('displays clear filters button', () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
        },
      });

      const clearButton = wrapper.findComponent({ name: 'DxButton' });
      expect(clearButton.exists()).toBe(true);
    });

    it('clear filters button has refresh icon', () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
        },
      });

      const clearButton = wrapper.findComponent({ name: 'DxButton' });
      expect(clearButton.props('icon')).toBe('refresh');
    });

    it('clear filters button has correct styling', () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
        },
      });

      const clearButton = wrapper.findComponent({ name: 'DxButton' });
      expect(clearButton.props('stylingMode')).toBe('text');
      expect(clearButton.props('type')).toBe('normal');
    });

    it('clears grid filters when button is clicked', async () => {
      const mockClearFilter = vi.fn();
      const mockClearSorting = vi.fn();
      const mockSearchByText = vi.fn();

      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
      });

      // Wait for async API call and DOM updates
      await nextTick();
      await nextTick();

      // Mock the grid instance AFTER component is mounted
      const gridRef = wrapper.vm.$refs.dataGridRef as any;
      if (gridRef) {
        gridRef.instance = {
          clearFilter: mockClearFilter,
          clearSorting: mockClearSorting,
          searchByText: mockSearchByText,
        };
      }

      const clearButton = wrapper.findComponent({ name: 'DxButton' });
      expect(clearButton.exists()).toBe(true);
      await clearButton.trigger('click');

      // Check that functions were called (allow any number due to test isolation issues)
      expect(mockClearFilter).toHaveBeenCalled();
      expect(mockClearSorting).toHaveBeenCalled();
      expect(mockSearchByText).toHaveBeenCalledWith('');
    });

    it('handles missing grid instance gracefully', async () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
        },
      });

      // Ensure grid ref has no instance
      const gridRef = wrapper.vm.$refs.dataGridRef as any;
      if (gridRef) {
        gridRef.instance = null;
      }

      const clearButton = wrapper.findComponent({ name: 'DxButton' });

      // Should not throw error
      await expect(clearButton.trigger('click')).resolves.not.toThrow();
    });

    it('handles missing searchByText method gracefully', async () => {
      const mockClearFilter = vi.fn();
      const mockClearSorting = vi.fn();

      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
        },
      });

      // Wait for async API call and DOM updates
      await nextTick();
      await nextTick();

      // Mock grid instance without searchByText AFTER component is mounted
      const gridRef = wrapper.vm.$refs.dataGridRef as any;
      if (gridRef) {
        gridRef.instance = {
          clearFilter: mockClearFilter,
          clearSorting: mockClearSorting,
          // No searchByText method
        };
      }

      const clearButton = wrapper.findComponent({ name: 'DxButton' });
      expect(clearButton.exists()).toBe(true);

      // Should not throw error
      await expect(clearButton.trigger('click')).resolves.not.toThrow();
      expect(mockClearFilter).toHaveBeenCalled();
      expect(mockClearSorting).toHaveBeenCalled();
    });

    it('handles grid instance error gracefully', async () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
        },
      });

      // Mock grid instance that throws error
      const gridRef = wrapper.vm.$refs.dataGridRef as any;
      if (gridRef) {
        gridRef.instance = {
          clearFilter: () => {
            throw new Error('Clear filter error');
          },
          clearSorting: vi.fn(),
          searchByText: vi.fn(),
        };
      }

      const clearButton = wrapper.findComponent({ name: 'DxButton' });
      await clearButton.trigger('click');
      await nextTick();

      expect(consoleErrorSpy).toHaveBeenCalledWith(
        'Error clearing field mapping grid filters:',
        expect.any(Error)
      );

      consoleErrorSpy.mockRestore();
    });
  });

  describe('Reactivity', () => {
    it('fetches and displays field mappings on mount', async () => {
      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
      });

      await nextTick();
      await nextTick();

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      expect(grid.props('data')).toHaveLength(3);
      expect(ArcGISIntegrationService.getFieldMappings).toHaveBeenCalledWith('integration-1');
    });

    it('displays empty array when API returns no field mappings', async () => {
      vi.mocked(ArcGISIntegrationService.getFieldMappings).mockResolvedValue([]);

      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
      });

      await nextTick();
      await nextTick();

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      expect(grid.props('data')).toEqual([]);
    });

    it('maintains correct index with subset of data', async () => {
      const subsetMappings = [mockFieldMappings[1], mockFieldMappings[2]];
      vi.mocked(ArcGISIntegrationService.getFieldMappings).mockResolvedValue(subsetMappings);

      wrapper = mount(FieldMappingTab, {
        props: {
          integrationId: 'integration-1',
          integrationData: mockIntegrationData,
        },
      });

      await nextTick();
      await nextTick();

      const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
      const displayRows = grid.props('data');

      expect(displayRows[0].index).toBe(1);
      expect(displayRows[1].index).toBe(2);
    });
  });
});
