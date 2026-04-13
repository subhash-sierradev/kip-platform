import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import StatusChipForDataTable from '@/components/common/StatusChipForDataTable.vue';

describe('StatusChipForDataTable', () => {
  it('renders label and success variant for success values', () => {
    const wrapper = mount(StatusChipForDataTable, {
      props: { status: 'SUCCESS', label: 'Success' },
    });

    expect(wrapper.text()).toBe('Success');
    expect(wrapper.classes()).toContain('kw-status-chip');
    expect(wrapper.classes()).toContain('kw-status-chip--success');
  });

  it('renders error variant for required values', () => {
    const wrapper = mount(StatusChipForDataTable, {
      props: { status: 'REQUIRED', label: 'Yes' },
    });

    expect(wrapper.text()).toBe('Yes');
    expect(wrapper.classes()).toContain('kw-status-chip--error');
  });

  it('renders warning variant for execution values', () => {
    const wrapper = mount(StatusChipForDataTable, {
      props: { status: 'RUN_NOW', label: 'Run Now' },
    });

    expect(wrapper.text()).toBe('Run Now');
    expect(wrapper.classes()).toContain('kw-status-chip--warning');
  });

  it('defaults to neutral variant for unknown values', () => {
    const wrapper = mount(StatusChipForDataTable, {
      props: { status: 'SOMETHING_NEW', label: 'Something' },
    });

    expect(wrapper.text()).toBe('Something');
    expect(wrapper.classes()).toContain('kw-status-chip--neutral');
  });

  it('falls back to UNKNOWN label for empty values', () => {
    const wrapper = mount(StatusChipForDataTable, {
      props: { status: '', label: '' },
    });

    expect(wrapper.text()).toBe('UNKNOWN');
    expect(wrapper.classes()).toContain('kw-status-chip--neutral');
  });

  it('uses ariaLabel when provided', () => {
    const wrapper = mount(StatusChipForDataTable, {
      props: { status: 'SUCCESS', label: 'Success', ariaLabel: 'Execution success' },
    });

    expect(wrapper.attributes('aria-label')).toBe('Execution success');
  });

  it('prefers an explicit variant prop over derived status mapping', () => {
    const wrapper = mount(StatusChipForDataTable, {
      props: { status: 'SUCCESS', label: 'Success', variant: 'warning' },
    });

    expect(wrapper.classes()).toContain('kw-status-chip--warning');
    expect(wrapper.classes()).not.toContain('kw-status-chip--success');
  });

  it('falls back to the label for variant resolution when status is not provided', () => {
    const wrapper = mount(StatusChipForDataTable, {
      props: { status: null, label: 'deleted' },
    });

    expect(wrapper.text()).toBe('deleted');
    expect(wrapper.classes()).toContain('kw-status-chip--error');
  });

  it('uses the resolved label as the aria-label when ariaLabel is an empty string', () => {
    const wrapper = mount(StatusChipForDataTable, {
      props: { status: null, label: 'Queued', ariaLabel: '' },
    });

    expect(wrapper.attributes('aria-label')).toBe('Queued');
  });

  it('normalizes whitespace-only status values to UNKNOWN and a neutral variant', () => {
    const wrapper = mount(StatusChipForDataTable, {
      props: { status: '   ', label: null },
    });

    expect(wrapper.text()).toBe('UNKNOWN');
    expect(wrapper.classes()).toContain('kw-status-chip--neutral');
    expect(wrapper.attributes('aria-label')).toBe('UNKNOWN');
  });

  it('falls back to UNKNOWN when both status and label are missing', () => {
    const wrapper = mount(StatusChipForDataTable, {
      props: { status: null, label: null },
    });

    expect(wrapper.text()).toBe('UNKNOWN');
    expect(wrapper.classes()).toContain('kw-status-chip--neutral');
    expect(wrapper.attributes('aria-label')).toBe('UNKNOWN');
  });
});
