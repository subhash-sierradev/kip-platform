/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect } from 'vitest';
import DashboardToolbar from '@/components/common/DashboardToolbar.vue';
import type { DashboardSortOption } from '@/types/dashboard';

describe('DashboardToolbar', () => {
  const defaultSortOptions: DashboardSortOption[] = [
    { value: 'createdDate', label: 'Created Date' },
    { value: 'name', label: 'Name' },
    { value: 'updatedDate', label: 'Updated Date' },
  ];

  const baseProps = {
    search: '',
    sortBy: 'createdDate',
    sortOptions: defaultSortOptions,
    viewMode: 'grid' as const,
    pageSize: 6,
    currentPage: 1,
    totalCount: 12,
    pageSizeOptions: [6, 12, 24],
  };

  it('renders search input with correct placeholder', () => {
    const wrapper = mount(DashboardToolbar, {
      props: {
        ...baseProps,
        searchPlaceholder: 'Search test items...',
      },
    });

    const searchInput = wrapper.find('input[type="text"]');
    expect(searchInput.exists()).toBe(true);
    expect(searchInput.attributes('placeholder')).toBe('Search test items...');
  });

  it('renders all sort options correctly', () => {
    const wrapper = mount(DashboardToolbar, { props: baseProps });

    const select = wrapper.find('#sortBySelect');
    const options = select.findAll('option');

    expect(options).toHaveLength(3);
    expect(options[0].text()).toBe('Created Date');
    expect(options[0].attributes('value')).toBe('createdDate');
    expect(options[1].text()).toBe('Name');
    expect(options[1].attributes('value')).toBe('name');
    expect(options[2].text()).toBe('Updated Date');
    expect(options[2].attributes('value')).toBe('updatedDate');
  });

  it('renders view mode buttons with correct active state', () => {
    const wrapper = mount(DashboardToolbar, {
      props: { ...baseProps, viewMode: 'list' },
    });

    const buttons = wrapper.findAll('.sort-and-view button');
    const gridButton = buttons[0];
    const listButton = buttons[1];

    expect(gridButton.classes()).not.toContain('active');
    expect(listButton.classes()).toContain('active');
  });

  it('renders pagination controls correctly', () => {
    const wrapper = mount(DashboardToolbar, {
      props: { ...baseProps, currentPage: 2, totalCount: 25 },
    });

    expect(wrapper.find('.current-page').text()).toBe('2');
    expect(wrapper.find('.page-info').text()).toBe('7-12 of 25');

    const pageSizeOptions = wrapper.findAll('#pageSizeSelect option');
    expect(pageSizeOptions).toHaveLength(3);
    expect(pageSizeOptions[0].attributes('value')).toBe('6');
    expect(pageSizeOptions[1].attributes('value')).toBe('12');
    expect(pageSizeOptions[2].attributes('value')).toBe('24');
  });

  it('renders create button with custom text and icon', () => {
    const wrapper = mount(DashboardToolbar, {
      props: {
        ...baseProps,
        createButtonText: '+ Add Custom Item',
        createButtonIcon: 'dx-icon-plus',
      },
    });

    const createButton = wrapper.find('.create-webhook-btn');
    expect(createButton.text()).toBe('+ Add Custom Item');

    const icon = createButton.find('i.dx-icon-plus');
    expect(icon.exists()).toBe(true);
  });

  it('emits search updates correctly', async () => {
    const wrapper = mount(DashboardToolbar, { props: baseProps });

    const searchInput = wrapper.find('input[type="text"]');
    await searchInput.setValue('test search');

    expect(wrapper.emitted('update:search')).toBeTruthy();
    expect(wrapper.emitted('update:search')![0]).toEqual(['test search']);
  });

  it('emits sort updates correctly', async () => {
    const wrapper = mount(DashboardToolbar, { props: baseProps });

    const select = wrapper.find('#sortBySelect');
    await select.setValue('name');

    expect(wrapper.emitted('update:sortBy')).toBeTruthy();
    expect(wrapper.emitted('update:sortBy')![0]).toEqual(['name']);
  });

  it('emits page size updates correctly', async () => {
    const wrapper = mount(DashboardToolbar, { props: baseProps });

    const select = wrapper.find('#pageSizeSelect');
    await select.setValue('12');

    expect(wrapper.emitted('update:pageSize')).toBeTruthy();
    expect(wrapper.emitted('update:pageSize')![0]).toEqual([12]);
  });

  it('emits view mode changes correctly', async () => {
    const wrapper = mount(DashboardToolbar, { props: baseProps });

    const buttons = wrapper.findAll('.sort-and-view button');
    const listButton = buttons[1];

    await listButton.trigger('click');

    expect(wrapper.emitted('setViewMode')).toBeTruthy();
    expect(wrapper.emitted('setViewMode')![0]).toEqual(['list']);
  });

  it('emits pagination events correctly', async () => {
    const wrapper = mount(DashboardToolbar, {
      props: { ...baseProps, currentPage: 2, totalCount: 25 },
    });

    const arrows = wrapper.findAll('.page-arrow');
    const prevArrow = arrows[0];
    const nextArrow = arrows[1];

    await prevArrow.trigger('click');
    expect(wrapper.emitted('prevPage')).toBeTruthy();

    await nextArrow.trigger('click');
    expect(wrapper.emitted('nextPage')).toBeTruthy();
  });

  it('emits create event when create button clicked', async () => {
    const wrapper = mount(DashboardToolbar, { props: baseProps });

    const createButton = wrapper.find('.create-webhook-btn');
    await createButton.trigger('click');

    expect(wrapper.emitted('create')).toBeTruthy();
    expect(wrapper.emitted('create')![0]).toEqual([]);
  });

  it('handles empty state correctly', () => {
    const wrapper = mount(DashboardToolbar, {
      props: { ...baseProps, totalCount: 0, currentPage: 1 },
    });

    expect(wrapper.find('.current-page').text()).toBe('0');
    expect(wrapper.find('.page-info').text()).toBe('0-0 of 0');

    const arrows = wrapper.findAll('.page-arrow');
    expect(arrows[0].classes()).toContain('disabled');
    expect(arrows[1].classes()).toContain('disabled');
  });

  it('disables pagination arrows at boundaries', () => {
    // Test first page
    const wrapperFirstPage = mount(DashboardToolbar, {
      props: { ...baseProps, currentPage: 1, totalCount: 25 },
    });

    let arrows = wrapperFirstPage.findAll('.page-arrow');
    expect(arrows[0].classes()).toContain('disabled'); // prev disabled
    expect(arrows[1].classes()).not.toContain('disabled'); // next enabled

    // Test last page
    const wrapperLastPage = mount(DashboardToolbar, {
      props: { ...baseProps, currentPage: 5, totalCount: 25, pageSize: 6 },
    });

    arrows = wrapperLastPage.findAll('.page-arrow');
    expect(arrows[0].classes()).not.toContain('disabled'); // prev enabled
    expect(arrows[1].classes()).toContain('disabled'); // next disabled
  });

  it('prevents pagination clicks when disabled', async () => {
    const wrapper = mount(DashboardToolbar, {
      props: { ...baseProps, currentPage: 1, totalCount: 0 },
    });

    const arrows = wrapper.findAll('.page-arrow');
    const prevArrow = arrows[0];
    const nextArrow = arrows[1];

    await prevArrow.trigger('click');
    await nextArrow.trigger('click');

    expect(wrapper.emitted('prevPage')).toBeFalsy();
    expect(wrapper.emitted('nextPage')).toBeFalsy();
  });

  it('calculates pagination display correctly', () => {
    const testCases = [
      { currentPage: 1, pageSize: 6, totalCount: 25, expectedStart: 1, expectedEnd: 6 },
      { currentPage: 2, pageSize: 6, totalCount: 25, expectedStart: 7, expectedEnd: 12 },
      { currentPage: 5, pageSize: 6, totalCount: 25, expectedStart: 25, expectedEnd: 25 },
      { currentPage: 1, pageSize: 10, totalCount: 5, expectedStart: 1, expectedEnd: 5 },
    ];

    testCases.forEach(({ currentPage, pageSize, totalCount, expectedStart, expectedEnd }) => {
      const wrapper = mount(DashboardToolbar, {
        props: { ...baseProps, currentPage, pageSize, totalCount },
      });

      expect(wrapper.find('.page-info').text()).toBe(
        `${expectedStart}-${expectedEnd} of ${totalCount}`
      );
    });
  });

  it('uses default props when not provided', () => {
    const minimalProps = {
      search: '',
      sortBy: 'createdDate',
      sortOptions: defaultSortOptions,
      viewMode: 'grid' as const,
      pageSize: 6,
      currentPage: 1,
      totalCount: 0,
      pageSizeOptions: [6, 12],
    };

    const wrapper = mount(DashboardToolbar, { props: minimalProps });

    const _searchInput = wrapper.find('input[type="text"]');
    expect(_searchInput.attributes('placeholder')).toBe('Search items...');

    const createButton = wrapper.find('.create-webhook-btn');
    expect(createButton.text()).toBe('+ Add Item');
    expect(createButton.find('i').exists()).toBe(false);
  });

  it('handles accessibility correctly', () => {
    const wrapper = mount(DashboardToolbar, { props: baseProps });

    const _searchInput = wrapper.find('input[type="text"]');
    const sortSelect = wrapper.find('#sortBySelect');
    const pageSizeSelect = wrapper.find('#pageSizeSelect');
    const arrows = wrapper.findAll('.page-arrow');
    const buttons = wrapper.findAll('.sort-and-view button');

    expect(sortSelect.attributes('aria-label')).toBe('Sort items by field');
    expect(pageSizeSelect.attributes('aria-label')).toBe('Select page size');
    expect(arrows[0].attributes('aria-label')).toBe('Previous page');
    expect(arrows[1].attributes('aria-label')).toBe('Next page');
    expect(buttons[0].attributes('aria-label')).toBe('Grid View');
    expect(buttons[1].attributes('aria-label')).toBe('List View');
  });
});
