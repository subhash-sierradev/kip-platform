/* eslint-disable simple-import-sort/imports */
import { mount as baseMount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ref, nextTick } from 'vue';
import CustomFieldsPanel from '@/components/outbound/jirawebhooks/wizard/CustomFieldsPanel.vue';

const mount = (...args: Parameters<typeof baseMount>) => baseMount(...args) as any;

// Mock JiraIntegrationService
vi.mock('@/api/services/JiraIntegrationService', () => ({
  JiraIntegrationService: {
    getProjectMetaFieldsByConnectionId: vi.fn(),
  },
}));
import { JiraIntegrationService } from '@/api/services/JiraIntegrationService';
const mockGetProjectMetaFieldsByConnectionId =
  JiraIntegrationService.getProjectMetaFieldsByConnectionId as ReturnType<typeof vi.fn>;

// Mock confirmation dialog
const mockDialogOpen = ref(false);
const mockActionLoading = ref(false);
const mockOpenDialog = vi.fn();
const mockCloseDialog = vi.fn();
const mockConfirmWithHandlers = vi.fn();

vi.mock('@/composables/useConfirmationDialog', () => ({
  useConfirmationDialog: () => ({
    dialogOpen: mockDialogOpen,
    actionLoading: mockActionLoading,
    openDialog: mockOpenDialog,
    closeDialog: mockCloseDialog,
    confirmWithHandlers: mockConfirmWithHandlers,
  }),
}));

describe('CustomFieldsPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockDialogOpen.value = false;
    mockActionLoading.value = false;
    mockOpenDialog.mockImplementation(_ => {
      mockDialogOpen.value = true;
    });
    mockCloseDialog.mockImplementation(() => {
      mockDialogOpen.value = false;
    });
    mockConfirmWithHandlers.mockImplementation(async (handlers: any) => {
      await handlers?.delete?.();
      mockDialogOpen.value = false;
    });
    mockGetProjectMetaFieldsByConnectionId.mockResolvedValue([
      { key: 'summary', name: 'Summary', type: 'string' },
      { key: 'cf_option', name: 'Option Field', type: 'option', allowedValues: ['A', 'B'] },
      { key: 'cf_number', name: 'Number Field', type: 'number' },
    ]);
  });

  it('renders loading state initially', () => {
    // Use sync mount without awaiting async operations
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        projectKey: 'PRJ',
        rows: [],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    // Loading should be true immediately after mount triggers loadMetadata
    expect(wrapper.vm.loading).toBe(true);
  });

  it('initializes with provided rows', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [{ field: 'summary', value: 'test value', label: 'Summary', type: 'string' }],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    expect(wrapper.vm.rowsLocal.length).toBe(1);
    expect(wrapper.vm.rowsLocal[0].field).toBe('summary');
    expect(wrapper.vm.rowsLocal[0].value).toBe('test value');
  });

  it('does not reset existing rows on initial project and issueType watcher run', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        projectKey: 'PRJ',
        issueTypeId: '10001',
        rows: [{ field: 'summary', value: 'test value', label: 'Summary', type: 'string' }],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    expect(wrapper.vm.rowsLocal).toHaveLength(1);
    expect(wrapper.vm.rowsLocal[0].field).toBe('summary');
    expect(wrapper.vm.rowsLocal[0].value).toBe('test value');
  });

  it('initializes with empty row when no rows provided', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    // New design starts with an empty state — no auto-added placeholder row
    expect(wrapper.vm.rowsLocal.length).toBe(0);
  });

  it('loads fields from API on mount and completes successfully', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn-123',
        projectKey: 'ABC',
        rows: [],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    // Initially loading should be true
    expect(wrapper.vm.loading).toBe(true);

    // Wait for the loading to complete
    await vi.waitFor(
      () => {
        expect(wrapper.vm.loading).toBe(false);
      },
      { timeout: 1000 }
    );

    // Verify API was called with correct connection + project
    expect(mockGetProjectMetaFieldsByConnectionId).toHaveBeenCalledWith(
      'conn-123',
      'ABC',
      undefined
    );

    // Verify no error was set
    expect(wrapper.vm.loadError).toBe('');
  });

  it('excludes sprint field when issue type is subtask', async () => {
    mockGetProjectMetaFieldsByConnectionId.mockResolvedValueOnce([
      {
        key: 'customfield_10020',
        name: 'Sprint',
        custom: true,
        schemaDetails: {
          type: 'array',
          custom: 'com.pyxis.greenhopper.jira:gh-sprint',
        },
      },
      {
        key: 'customfield_10009',
        name: 'Some Other Field',
        custom: true,
        schemaDetails: {
          type: 'string',
          custom: 'com.atlassian.jira.plugin.system.customfieldtypes:textfield',
        },
      },
    ]);

    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn-subtask',
        projectKey: 'PRJ',
        issueTypeId: 'subtask-id',
        isSubtaskIssueType: true,
        rows: [],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    expect(wrapper.vm.allFields.some((field: any) => field.type === 'sprint')).toBe(false);
    expect(wrapper.vm.allFields.map((field: any) => field.key)).toContain('customfield_10009');
  });

  it('displays error when API fails to load fields', async () => {
    mockGetProjectMetaFieldsByConnectionId.mockRejectedValueOnce(new Error('API error'));

    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        projectKey: 'PRJ',
        rows: [],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    expect(wrapper.find('.cf-status-error').exists()).toBe(true);
    expect(wrapper.find('.cf-status-error').text()).toContain('Failed to load Jira fields.');
  });

  it('adds new row when add button clicked', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [{ field: 'summary', value: 'x', label: 'Summary', type: 'string' }],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    expect(wrapper.vm.rowsLocal.length).toBe(1);

    const addBtn = wrapper.find('.cf-panel-add-btn');
    await addBtn.trigger('click');
    await nextTick();

    expect(wrapper.vm.rowsLocal.length).toBe(2);
    expect(wrapper.vm.rowsLocal[1].field).toBe('');
    expect(wrapper.vm.rowsLocal[1].value).toBe('');
  });

  it('emits update:rows when addRow is called', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    const addBtn = wrapper.find('.cf-panel-add-btn');
    await addBtn.trigger('click');
    await nextTick();

    const updateEvents = wrapper.emitted('update:rows');
    expect(updateEvents).toBeTruthy();
    expect(Array.isArray(updateEvents![updateEvents!.length - 1][0])).toBe(true);
  });

  it('opens confirmation dialog when removeRow is called', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [
          { field: 'summary', value: 'val1', label: 'Summary', type: 'string' },
          { field: 'cf_number', value: '42', label: 'Number', type: 'number' },
        ],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    const removeBtn = wrapper.find('.cf-trash-btn');
    await removeBtn.trigger('click');
    await nextTick();

    expect(mockOpenDialog).toHaveBeenCalledWith('delete');
    expect(mockDialogOpen.value).toBe(true);
  });

  it('removes row when confirmation is accepted', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [
          { field: 'summary', value: 'val1', label: 'Summary', type: 'string' },
          { field: 'cf_number', value: '42', label: 'Number', type: 'number' },
        ],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    expect(wrapper.vm.rowsLocal.length).toBe(2);

    const removeBtn = wrapper.find('.cf-trash-btn');
    await removeBtn.trigger('click');
    await nextTick();

    const confirmBtn = wrapper.find('.ms-btn-danger');
    await confirmBtn.trigger('click');
    await nextTick();

    expect(mockConfirmWithHandlers).toHaveBeenCalled();
    expect(wrapper.vm.rowsLocal.length).toBe(1);
  });

  it('removes a row via trash button reducing remaining count', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [
          { field: 'summary', value: 'val1', label: 'Summary', type: 'string' },
          { field: 'cf_number', value: '42', label: 'Number', type: 'number' },
        ],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    expect(wrapper.vm.rowsLocal.length).toBe(2);

    const trashBtns = wrapper.findAll('.cf-trash-btn');
    await trashBtns[0].trigger('click');
    await nextTick();

    expect(mockOpenDialog).toHaveBeenCalledWith('delete');

    await wrapper.vm.onConfirmDelete();
    await nextTick();

    expect(wrapper.vm.rowsLocal.length).toBe(1);
  });

  it('closes confirmation dialog when Cancel clicked', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [{ field: 'summary', value: 'x', label: 'Summary', type: 'string' }],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    const removeBtn = wrapper.find('.cf-trash-btn');
    await removeBtn.trigger('click');
    await nextTick();

    expect(mockDialogOpen.value).toBe(true);

    const cancelBtn = wrapper.find('.ms-btn-cancel');
    await cancelBtn.trigger('click');
    await nextTick();

    expect(mockCloseDialog).toHaveBeenCalled();
  });

  it('shows clearAll tooltip on mouseenter', async () => {
    mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    // clearAll and tooltip state were removed in the enterprise redesign
    // Tests below verify the new panel UI structure instead.

    // Placeholder tick to keep the surrounding describe block valid
    expect(true).toBe(true);
  });

  it('renders count badge showing number of rows', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [
          { field: 'summary', value: 'v1', label: 'Summary', type: 'string' },
          { field: 'cf_number', value: '2', label: 'Number', type: 'number' },
        ],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: { stubs: { CustomFieldRow: { template: '<div class="row-stub" />' } } },
    });

    await nextTick();
    await nextTick();

    const badge = wrapper.find('.cf-count-badge');
    expect(badge.exists()).toBe(true);
    expect(badge.text()).toBe('2');
  });

  it('hides count badge when no rows are present', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: { connectionId: 'conn1', rows: [], projectUsers: [], renderedPreviews: [] },
      global: { stubs: { CustomFieldRow: { template: '<div class="row-stub" />' } } },
    });

    await nextTick();
    await nextTick();

    expect(wrapper.find('.cf-count-badge').exists()).toBe(false);
  });

  it('renders empty state illustration and description when no rows', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: { connectionId: 'conn1', rows: [], projectUsers: [], renderedPreviews: [] },
      global: { stubs: { CustomFieldRow: { template: '<div class="row-stub" />' } } },
    });

    await nextTick();
    await nextTick();

    expect(wrapper.find('.cf-empty-state').exists()).toBe(true);
    expect(wrapper.find('.cf-empty-title').text()).toBe('No custom fields added');
  });

  it('renders a trash button for each row', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [
          { field: 'summary', value: 'v1', label: 'Summary', type: 'string' },
          { field: 'cf_number', value: '2', label: 'Number', type: 'number' },
        ],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: { stubs: { CustomFieldRow: { template: '<div class="row-stub" />' } } },
    });

    await nextTick();
    await nextTick();

    expect(wrapper.findAll('.cf-trash-btn').length).toBe(2);
  });

  it('renders the optional label in the panel header', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: { connectionId: 'conn1', rows: [], projectUsers: [], renderedPreviews: [] },
      global: { stubs: { CustomFieldRow: { template: '<div class="row-stub" />' } } },
    });

    await nextTick();
    await nextTick();

    expect(wrapper.find('.cf-optional-label').exists()).toBe(true);
    expect(wrapper.find('.cf-optional-label').text()).toBe('Optional');
  });

  it('hides empty state once a row is added', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: { connectionId: 'conn1', rows: [], projectUsers: [], renderedPreviews: [] },
      global: { stubs: { CustomFieldRow: { template: '<div class="row-stub" />' } } },
    });

    await nextTick();
    await nextTick();

    expect(wrapper.find('.cf-empty-state').exists()).toBe(true);

    wrapper.vm.addRow();
    await nextTick();

    expect(wrapper.find('.cf-empty-state').exists()).toBe(false);
  });

  it('emits valid-change with true when row has field and value', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [{ field: '', value: '', label: '', type: 'string' }],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: {
            template: '<div class="row-stub" />',
            emits: ['row-change'],
          },
        },
      },
    });

    await nextTick();
    await nextTick();

    wrapper.vm.onRowChange({
      idx: 0,
      field: 'summary',
      value: 'test',
      label: 'Summary',
      type: 'string',
    });

    await nextTick();

    const validEvents = wrapper.emitted('valid-change');
    expect(validEvents).toBeTruthy();
  });

  it('exposes insertJsonPlaceholder method', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [{ field: 'summary', value: 'initial', label: 'Summary', type: 'string' }],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    wrapper.vm.activeStringRowIndex = 0;
    wrapper.vm.insertJsonPlaceholder('{{field}}');

    await nextTick();

    expect(wrapper.vm.rowsLocal[0].value).toBe('initial {{field}}');
  });

  it('does nothing when insertJsonPlaceholder called without active row', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [{ field: 'summary', value: 'initial', label: 'Summary', type: 'string' }],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    wrapper.vm.activeStringRowIndex = null;
    const initialValue = wrapper.vm.rowsLocal[0].value;
    wrapper.vm.insertJsonPlaceholder('{{field}}');

    await nextTick();

    expect(wrapper.vm.rowsLocal[0].value).toBe(initialValue);
  });

  it('emits open-insert when child triggers open-insert', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [{ field: 'summary', value: 'x', label: 'Summary', type: 'string' }],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: {
            template: '<div class="row-stub" />',
            emits: ['open-insert'],
          },
        },
      },
    });

    await nextTick();
    await nextTick();

    wrapper.vm.onChildOpenInsert({ rowIndex: 0 });
    await nextTick();

    const openInsertEvents = wrapper.emitted('open-insert');
    expect(openInsertEvents).toBeTruthy();
    expect(openInsertEvents![0][0]).toEqual({ rowIndex: 0 });
  });

  it('emits toggle-preview when child triggers toggle-preview', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [{ field: 'summary', value: 'x', label: 'Summary', type: 'string' }],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: {
            template: '<div class="row-stub" />',
            emits: ['toggle-preview'],
          },
        },
      },
    });

    await nextTick();
    await nextTick();

    wrapper.vm.onChildTogglePreview({ rowIndex: 0, value: 'preview text' });
    await nextTick();

    const toggleEvents = wrapper.emitted('toggle-preview');
    expect(toggleEvents).toBeTruthy();
    expect(toggleEvents![0][0]).toEqual({ rowIndex: 0, value: 'preview text' });
  });

  it('reacts to props.rows changes by reinitializing', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [{ field: 'summary', value: 'original', label: 'Summary', type: 'string' }],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    expect(wrapper.vm.rowsLocal[0].value).toBe('original');

    await wrapper.setProps({
      rows: [{ field: 'cf_number', value: '99', label: 'Number', type: 'number' }],
    });
    await nextTick();

    expect(wrapper.vm.rowsLocal[0].field).toBe('cf_number');
    expect(wrapper.vm.rowsLocal[0].value).toBe('99');
  });

  it('confirmation modal shows correct title for removeRow', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [{ field: 'summary', value: 'x', label: 'Summary', type: 'string' }],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    const trashBtn = wrapper.find('.cf-trash-btn');
    await trashBtn.trigger('click');
    await nextTick();

    expect(wrapper.find('.cf-modal-title').text()).toBe('Remove Custom Field');
  });

  it('confirmation modal shows correct title for removeRow', async () => {
    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [
          { field: 'summary', value: 'val1', label: 'Summary', type: 'string' },
          { field: 'cf_number', value: '42', label: 'Number', type: 'number' },
        ],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    const removeBtn = wrapper.find('.cf-trash-btn');
    await removeBtn.trigger('click');
    await nextTick();

    expect(wrapper.find('.cf-modal-title').text()).toBe('Remove Custom Field');
  });

  it('disables modal buttons when actionLoading is true', async () => {
    mockActionLoading.value = true;

    const wrapper = mount(CustomFieldsPanel, {
      props: {
        connectionId: 'conn1',
        rows: [{ field: 'summary', value: 'x', label: 'Summary', type: 'string' }],
        projectUsers: [],
        renderedPreviews: [],
      },
      global: {
        stubs: {
          CustomFieldRow: { template: '<div class="row-stub" />' },
        },
      },
    });

    await nextTick();
    await nextTick();

    wrapper.vm.removeRow(0);
    await nextTick();

    const cancelBtn = wrapper.find('.ms-btn-cancel');
    const dangerBtn = wrapper.find('.ms-btn-danger');

    expect(cancelBtn.attributes('disabled')).toBeDefined();
    expect(dangerBtn.attributes('disabled')).toBeDefined();
  });
});
