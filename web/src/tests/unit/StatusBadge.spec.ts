import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import StatusBadge from '@/components/common/StatusBadge.vue';

describe('StatusBadge', () => {
  describe('Component Rendering & Props', () => {
    it('applies classes based on status and shows label', () => {
      const cases = [
        { status: 'SUCCESS', cls: 'success', label: 'Success' },
        { status: 'FAILED', cls: 'failed', label: 'Failed' },
        { status: 'ACTIVE', cls: 'active', label: 'Active' },
      ];
      for (const c of cases) {
        const wrapper = mount(StatusBadge, { props: { status: c.status } });
        expect(wrapper.classes()).toContain('status-badge');
        expect(wrapper.classes()).toContain(c.cls);
        expect(wrapper.text()).toContain(c.label);
      }
    });

    it('defaults to neutral class and neutral label when status missing', () => {
      const wrapper = mount(StatusBadge);
      expect(wrapper.classes()).toContain('neutral');
      expect(wrapper.text()).toContain('neutral');
    });

    it('defaults to neutral when status is undefined', () => {
      const wrapper = mount(StatusBadge, { props: { status: undefined } });
      expect(wrapper.classes()).toContain('neutral');
      expect(wrapper.text()).toBe('neutral');
    });

    it('defaults to neutral when status is empty string', () => {
      const wrapper = mount(StatusBadge, { props: { status: '' } });
      expect(wrapper.classes()).toContain('neutral');
      expect(wrapper.text()).toContain('Unknown');
    });

    it('applies custom label when provided', () => {
      const wrapper = mount(StatusBadge, {
        props: {
          status: 'success',
          customLabel: 'All Good!',
        },
      });
      expect(wrapper.text()).toContain('All Good!');
    });

    it('applies custom label over default label', () => {
      const wrapper = mount(StatusBadge, {
        props: {
          status: 'failed',
          customLabel: 'Custom Error Occurred',
        },
      });
      expect(wrapper.text()).toBe('Custom Error Occurred');
      expect(wrapper.text()).not.toContain('Failed');
    });

    it('applies custom size class', () => {
      const wrapper = mount(StatusBadge, {
        props: {
          status: 'success',
          size: 'large',
        },
      });
      expect(wrapper.classes()).toContain('size-large');
    });

    it('handles different size variants', () => {
      const sizes = ['small', 'medium', 'large'] as const;
      for (const size of sizes) {
        const wrapper = mount(StatusBadge, {
          props: {
            status: 'success',
            size,
          },
        });
        expect(wrapper.classes()).toContain(`size-${size}`);
      }
    });

    it('defaults to medium size when size not specified', () => {
      const wrapper = mount(StatusBadge, { props: { status: 'success' } });
      expect(wrapper.classes()).toContain('size-medium');
    });
  });

  describe('Status Class Mapping', () => {
    it('maps status correctly for different cases', () => {
      const statusCases = [
        { status: 'error', expectedClass: 'failed', expectedLabel: 'error' },
        { status: 'danger', expectedClass: 'failed', expectedLabel: 'danger' },
        { status: 'enabled', expectedClass: 'active', expectedLabel: 'Enabled' },
        { status: 'disabled', expectedClass: 'disabled', expectedLabel: 'Disabled' },
        { status: 'inactive', expectedClass: 'disabled', expectedLabel: 'inactive' },
        { status: 'warning', expectedClass: 'warning', expectedLabel: 'Warning' },
        { status: 'info', expectedClass: 'info', expectedLabel: 'Info' },
        { status: 'unknown', expectedClass: 'neutral', expectedLabel: 'unknown' },
      ];

      for (const testCase of statusCases) {
        const wrapper = mount(StatusBadge, {
          props: {
            status: testCase.status,
          },
        });
        expect(wrapper.classes()).toContain(testCase.expectedClass);
        expect(wrapper.text()).toContain(testCase.expectedLabel);
      }
    });

    it('maps "error" to failed class', () => {
      const wrapper = mount(StatusBadge, { props: { status: 'error' } });
      expect(wrapper.classes()).toContain('failed');
      expect(wrapper.text()).toBe('error');
    });

    it('maps "danger" to failed class', () => {
      const wrapper = mount(StatusBadge, { props: { status: 'danger' } });
      expect(wrapper.classes()).toContain('failed');
      expect(wrapper.text()).toBe('danger');
    });

    it('maps "enabled" to active class', () => {
      const wrapper = mount(StatusBadge, { props: { status: 'enabled' } });
      expect(wrapper.classes()).toContain('active');
      expect(wrapper.text()).toBe('Enabled');
    });

    it('maps "inactive" to disabled class', () => {
      const wrapper = mount(StatusBadge, { props: { status: 'inactive' } });
      expect(wrapper.classes()).toContain('disabled');
      expect(wrapper.text()).toBe('inactive');
    });

    it('handles case-insensitive status values', () => {
      const wrapper = mount(StatusBadge, { props: { status: 'SUCCESS' } });
      expect(wrapper.classes()).toContain('success');
    });

    it('handles mixed case status values', () => {
      const wrapper = mount(StatusBadge, { props: { status: 'WaRnInG' } });
      expect(wrapper.classes()).toContain('warning');
    });

    it('returns original status for unknown values', () => {
      const wrapper = mount(StatusBadge, { props: { status: 'custom-status' } });
      expect(wrapper.classes()).toContain('neutral');
      expect(wrapper.text()).toBe('custom-status');
    });
  });

  describe('Label Computation', () => {
    it('computes correct labels for all standard statuses', () => {
      const labelCases = [
        { status: 'success', expectedLabel: 'Success' },
        { status: 'failed', expectedLabel: 'Failed' },
        { status: 'active', expectedLabel: 'Active' },
        { status: 'enabled', expectedLabel: 'Enabled' },
        { status: 'disabled', expectedLabel: 'Disabled' },
        { status: 'warning', expectedLabel: 'Warning' },
        { status: 'info', expectedLabel: 'Info' },
      ];

      for (const testCase of labelCases) {
        const wrapper = mount(StatusBadge, { props: { status: testCase.status } });
        expect(wrapper.text()).toBe(testCase.expectedLabel);
      }
    });

    it('returns label for default neutral status when status is undefined', () => {
      const wrapper = mount(StatusBadge, { props: { status: undefined } });
      expect(wrapper.text()).toBe('neutral');
    });

    it('returns original status for unmapped values', () => {
      const wrapper = mount(StatusBadge, { props: { status: 'pending' } });
      expect(wrapper.text()).toBe('pending');
    });

    it('prefers customLabel over computed label', () => {
      const wrapper = mount(StatusBadge, {
        props: {
          status: 'success',
          customLabel: 'Override Label',
        },
      });
      expect(wrapper.text()).toBe('Override Label');
    });
  });

  describe('Icon Rendering', () => {
    it('shows icon when showIcon prop is true', () => {
      const wrapper = mount(StatusBadge, {
        props: {
          status: 'success',
          showIcon: true,
        },
      });
      expect(wrapper.find('.status-icon').exists()).toBe(true);
    });

    it('does not show icon when showIcon prop is false', () => {
      const wrapper = mount(StatusBadge, {
        props: {
          status: 'success',
          showIcon: false,
        },
      });
      expect(wrapper.find('.status-icon').exists()).toBe(false);
    });

    it('does not show icon by default', () => {
      const wrapper = mount(StatusBadge, { props: { status: 'success' } });
      expect(wrapper.find('.status-icon').exists()).toBe(false);
    });

    it('shows correct icon for success status', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'success', showIcon: true },
      });
      const icon = wrapper.find('.status-icon');
      expect(icon.classes()).toContain('dx-icon-check');
    });

    it('shows correct icon for active status', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'active', showIcon: true },
      });
      const icon = wrapper.find('.status-icon');
      expect(icon.classes()).toContain('dx-icon-check');
    });

    it('shows correct icon for failed status', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'failed', showIcon: true },
      });
      const icon = wrapper.find('.status-icon');
      expect(icon.classes()).toContain('dx-icon-close');
    });

    it('shows correct icon for disabled status', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'disabled', showIcon: true },
      });
      const icon = wrapper.find('.status-icon');
      expect(icon.classes()).toContain('dx-icon-cursorprohibition');
    });

    it('shows correct icon for warning status', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'warning', showIcon: true },
      });
      const icon = wrapper.find('.status-icon');
      expect(icon.classes()).toContain('dx-icon-warning');
    });

    it('shows correct icon for info status', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'info', showIcon: true },
      });
      const icon = wrapper.find('.status-icon');
      expect(icon.classes()).toContain('dx-icon-info');
    });

    it('shows help icon for neutral/unknown status', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'neutral', showIcon: true },
      });
      const icon = wrapper.find('.status-icon');
      expect(icon.classes()).toContain('dx-icon-help');
    });

    it('shows help icon for unmapped status', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'unknown-status', showIcon: true },
      });
      const icon = wrapper.find('.status-icon');
      expect(icon.classes()).toContain('dx-icon-help');
    });

    it('icon has aria-hidden attribute', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'success', showIcon: true },
      });
      expect(wrapper.find('.status-icon').attributes('aria-hidden')).toBe('true');
    });
  });

  describe('Accessibility (aria-label)', () => {
    it('uses custom aria label when provided', () => {
      const wrapper = mount(StatusBadge, {
        props: {
          status: 'success',
          customAriaLabel: 'Custom Status Message',
        },
      });
      expect(wrapper.attributes('aria-label')).toBe('Custom Status Message');
    });

    it('generates default aria-label without customAriaLabel', () => {
      const wrapper = mount(StatusBadge, { props: { status: 'success' } });
      expect(wrapper.attributes('aria-label')).toBe('Status: Success');
    });

    it('includes computed label in default aria-label', () => {
      const wrapper = mount(StatusBadge, { props: { status: 'warning' } });
      expect(wrapper.attributes('aria-label')).toBe('Status: Warning');
    });

    it('uses custom label in default aria-label when customLabel provided', () => {
      const wrapper = mount(StatusBadge, {
        props: {
          status: 'success',
          customLabel: 'All Systems Go',
        },
      });
      expect(wrapper.attributes('aria-label')).toBe('Status: All Systems Go');
    });

    it('prioritizes customAriaLabel over customLabel', () => {
      const wrapper = mount(StatusBadge, {
        props: {
          status: 'success',
          customLabel: 'All Systems Go',
          customAriaLabel: 'Screen reader message',
        },
      });
      expect(wrapper.attributes('aria-label')).toBe('Screen reader message');
    });
  });

  describe('Edge Cases', () => {
    it('handles null status gracefully', () => {
      const wrapper = mount(StatusBadge, { props: { status: null as any } });
      expect(wrapper.classes()).toContain('neutral');
      expect(wrapper.text()).toBe('Unknown');
    });

    it('handles numeric status values', () => {
      const wrapper = mount(StatusBadge, { props: { status: '404' } });
      expect(wrapper.classes()).toContain('neutral');
      expect(wrapper.text()).toBe('404');
    });

    it('handles special characters in status', () => {
      const wrapper = mount(StatusBadge, { props: { status: 'status-with-dashes' } });
      expect(wrapper.text()).toBe('status-with-dashes');
    });

    it('handles very long status strings', () => {
      const longStatus = 'very-long-status-string-that-keeps-going';
      const wrapper = mount(StatusBadge, { props: { status: longStatus } });
      expect(wrapper.text()).toBe(longStatus);
    });

    it('handles unicode characters in status', () => {
      const wrapper = mount(StatusBadge, { props: { status: 'état✓' } });
      expect(wrapper.text()).toBe('état✓');
    });
  });
});
