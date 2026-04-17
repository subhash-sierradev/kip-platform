import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import IntegrationStatusIcon from '@/components/common/IntegrationStatusIcon.vue';

describe('IntegrationStatusIcon', () => {
  it.each([
    ['SUCCESS', 'dx-icon-check', 'rgb(40, 167, 69)'],
    ['FAILED', 'dx-icon-close', 'rgb(220, 53, 69)'],
    ['RUNNING', 'dx-icon-refresh', 'rgb(0, 123, 255)'],
    ['RETRYING', 'dx-icon-refresh', 'rgb(0, 123, 255)'],
    ['ABORTED', 'dx-icon-remove', 'rgb(255, 152, 0)'],
    ['SCHEDULED', 'dx-icon-clock', 'rgb(108, 117, 125)'],
    ['PENDING', 'dx-icon-clock', 'rgb(108, 117, 125)'],
  ])('renders correct icon and color for %s status', (status, expectedIcon, expectedColor) => {
    const wrapper = mount(IntegrationStatusIcon, { props: { status } });
    const icon = wrapper.find('i');
    expect(icon.exists()).toBe(true);
    expect(icon.classes()).toContain(expectedIcon);
    expect(icon.classes()).toContain('integration-status-icon');
    expect(icon.attributes('style')).toContain(expectedColor);
  });

  it.each([
    ['success', 'dx-icon-check'],
    ['failed', 'dx-icon-close'],
  ])('is case-insensitive for status %s', (status, expectedIcon) => {
    const wrapper = mount(IntegrationStatusIcon, { props: { status } });
    expect(wrapper.find('i').classes()).toContain(expectedIcon);
  });

  it('renders nothing when status is null', () => {
    const wrapper = mount(IntegrationStatusIcon, { props: { status: null } });
    expect(wrapper.find('i').exists()).toBe(false);
  });

  it('renders nothing when status is undefined', () => {
    const wrapper = mount(IntegrationStatusIcon, { props: {} });
    expect(wrapper.find('i').exists()).toBe(false);
  });

  it('renders nothing for an unrecognized status', () => {
    const wrapper = mount(IntegrationStatusIcon, { props: { status: 'UNKNOWN' } });
    expect(wrapper.find('i').exists()).toBe(false);
  });

  it('shows tooltip on mouseenter and hides on mouseleave', async () => {
    const wrapper = mount(IntegrationStatusIcon, { props: { status: 'SUCCESS' } });
    const icon = wrapper.find('i');

    expect(wrapper.find('.custom-tooltip').exists()).toBe(false);

    await icon.trigger('mouseenter', { clientX: 100, clientY: 200 });
    expect(wrapper.find('.custom-tooltip').exists()).toBe(true);

    await icon.trigger('mouseleave');
    expect(wrapper.find('.custom-tooltip').exists()).toBe(false);
  });

  it('tooltip text matches the status message', async () => {
    const wrapper = mount(IntegrationStatusIcon, { props: { status: 'FAILED' } });
    await wrapper.find('i').trigger('mouseenter', { clientX: 0, clientY: 0 });
    expect(wrapper.find('.custom-tooltip').text()).toContain('check logs');
  });

  it('icon is keyboard-focusable with tabindex="0"', () => {
    const wrapper = mount(IntegrationStatusIcon, { props: { status: 'SUCCESS' } });
    expect(wrapper.find('i').attributes('tabindex')).toBe('0');
  });

  it('shows tooltip on focus and hides on blur', async () => {
    const wrapper = mount(IntegrationStatusIcon, { props: { status: 'SUCCESS' } });
    const icon = wrapper.find('i');

    expect(wrapper.find('.custom-tooltip').exists()).toBe(false);

    await icon.trigger('focus');
    expect(wrapper.find('.custom-tooltip').exists()).toBe(true);

    await icon.trigger('blur');
    expect(wrapper.find('.custom-tooltip').exists()).toBe(false);
  });

  it('sets aria-describedby when tooltip is visible and removes it when hidden', async () => {
    const wrapper = mount(IntegrationStatusIcon, { props: { status: 'SUCCESS' } });
    const icon = wrapper.find('i');

    expect(icon.attributes('aria-describedby')).toBeUndefined();

    await icon.trigger('mouseenter', { clientX: 100, clientY: 200 });
    const tooltipId = icon.attributes('aria-describedby');
    expect(tooltipId).toBeDefined();
    expect(tooltipId).toMatch(/^status-tooltip-/);

    await icon.trigger('mouseleave');
    expect(icon.attributes('aria-describedby')).toBeUndefined();
  });

  it('tooltip element has matching id and role="tooltip"', async () => {
    const wrapper = mount(IntegrationStatusIcon, { props: { status: 'SUCCESS' } });
    await wrapper.find('i').trigger('focus');

    const tooltip = wrapper.find('.custom-tooltip');
    expect(tooltip.exists()).toBe(true);
    expect(tooltip.attributes('role')).toBe('tooltip');
    expect(tooltip.attributes('id')).toMatch(/^status-tooltip-/);
    expect(tooltip.attributes('id')).toBe(wrapper.find('i').attributes('aria-describedby'));
  });
});
