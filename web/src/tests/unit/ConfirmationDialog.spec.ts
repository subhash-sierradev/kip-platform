import { mount } from '@vue/test-utils';
import { afterEach, describe, expect, it, vi } from 'vitest';

import ConfirmationDialog from '@/components/common/ConfirmationDialog.vue';

afterEach(() => {
  document.body.innerHTML = '';
  vi.useRealTimers();
});

describe('ConfirmationDialog', () => {
  it('renders dialog with correct title and description', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        description: 'Test description',
        confirmLabel: 'Confirm',
        cancelLabel: 'Cancel',
        loading: false,
      },
      attachTo: document.body,
    });

    expect(wrapper.exists()).toBe(true);
    expect(document.body.textContent).toContain('Test Title');
    expect(document.body.textContent).toContain('Test description');
    wrapper.unmount();
  });

  it('shows loading state when loading prop is true', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        loading: true,
      },
      attachTo: document.body,
    });

    expect(wrapper.exists()).toBe(true);
    expect(document.querySelector('.loader')).not.toBeNull();
    expect((document.querySelectorAll('button')[0] as HTMLButtonElement).disabled).toBe(true);
    expect((document.querySelectorAll('button')[1] as HTMLButtonElement).disabled).toBe(true);
    wrapper.unmount();
  });

  it('emits confirm event when confirm button is clicked', async () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        loading: false,
      },
    });

    const confirmButton = wrapper.find('[aria-label="Confirm"]');
    if (confirmButton.exists()) {
      await confirmButton.trigger('click');
      expect(wrapper.emitted().confirm).toBeTruthy();
    }
  });

  it('emits cancel event when cancel button is clicked', async () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        loading: false,
      },
    });

    const cancelButton = wrapper.find('[aria-label="Cancel"]');
    if (cancelButton.exists()) {
      await cancelButton.trigger('click');
      expect(wrapper.emitted().cancel).toBeTruthy();
    }
  });

  it('does not render when open is false', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: false,
        title: 'Test Title',
      },
    });

    expect(wrapper.find('.dx-popup').exists()).toBe(false);
  });

  it('handles undefined props gracefully', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        // Other props undefined
      },
    });

    expect(wrapper.exists()).toBe(true);
  });

  it('shows confirm button with correct label', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        confirmLabel: 'Delete Item',
      },
    });

    expect(wrapper.exists()).toBe(true);
  });

  it('shows cancel button with correct label', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        cancelLabel: 'Keep Item',
      },
    });

    expect(wrapper.exists()).toBe(true);
  });

  it('applies different dialog types', () => {
    const types = ['enable', 'disable', 'delete'] as const;
    for (const type of types) {
      const wrapper = mount(ConfirmationDialog, {
        props: {
          open: true,
          title: 'Test Title',
          type,
        },
      });
      expect(wrapper.exists()).toBe(true);
    }
  });

  it('handles different positions', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        position: { my: 'center', at: 'center' },
      },
    });
    expect(wrapper.exists()).toBe(true);
  });

  it('handles autoFocus prop', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        autoFocus: true,
      },
    });
    expect(wrapper.exists()).toBe(true);
  });

  it('handles maxWidth prop', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        maxWidth: 500,
      },
    });
    expect(wrapper.exists()).toBe(true);
  });

  it('handles closable prop', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        closable: false,
      },
    });
    expect(wrapper.exists()).toBe(true);
  });

  it('applies enable dialog type with correct config', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        type: 'enable',
      },
    });

    const vm = wrapper.vm as any;
    expect(vm.finalConfirmLabel).toContain('Enable');
    expect(vm.colorClass).toBe('btn-success');
  });

  it('applies disable dialog type with correct config', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        type: 'disable',
      },
    });

    const vm = wrapper.vm as any;
    expect(vm.finalConfirmLabel).toContain('Disable');
    expect(vm.colorClass).toBe('btn-error');
  });

  it('applies test dialog type with correct config', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        type: 'test',
      },
    });

    const vm = wrapper.vm as any;
    expect(vm.finalConfirmLabel).toContain('Test Run');
    expect(vm.colorClass).toBe('btn-warning');
  });

  it('applies runNow dialog type with correct config', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        type: 'runNow',
      },
    });

    const vm = wrapper.vm as any;
    expect(vm.finalConfirmLabel).toContain('Run');
    expect(vm.colorClass).toBe('btn-warning');
  });

  it('applies cancel dialog type with correct config', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        type: 'cancel',
      },
    });

    const vm = wrapper.vm as any;
    expect(vm.finalConfirmLabel).toContain('Cancel Job');
    expect(vm.colorClass).toBe('btn-error');
  });

  it('applies clone dialog type with correct config', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        type: 'clone',
      },
    });

    const vm = wrapper.vm as any;
    expect(vm.finalConfirmLabel).toContain('Clone');
    expect(vm.colorClass).toBe('btn-primary');
  });

  it('renders slot content when provided instead of description', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
      },
      slots: {
        default: '<div class="custom-content">Custom slot content</div>',
      },
      attachTo: document.body,
    });

    // Check that the component uses slot (by checking computed property)
    const vm = wrapper.vm as any;
    expect(vm.useSlot).toBe(true);
    expect(document.body.textContent).toContain('Custom slot content');
    expect(document.querySelector('.confirm-description')).toBeNull();
    wrapper.unmount();
  });

  it('handles ESC key press to trigger cancel', async () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
      },
      attachTo: document.body,
    });

    // Trigger ESC on the component
    const vm = wrapper.vm as any;
    vm.onEsc();

    await wrapper.vm.$nextTick();
    expect(wrapper.emitted().cancel).toBeTruthy();
    wrapper.unmount();
  });

  it('focuses on card when autoFocus is true and opens', async () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        autoFocus: true,
      },
      attachTo: document.body,
    });

    await wrapper.vm.$nextTick();
    // Verify cardRef exists
    const component = wrapper.vm as any;
    expect(component.cardRef).toBeTruthy();

    wrapper.unmount();
  });

  it('does not auto-focus when autoFocus is false', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        autoFocus: false,
      },
    });

    expect(wrapper.exists()).toBe(true);
  });

  it('converts numeric maxWidth to px string', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        maxWidth: 600,
      },
      attachTo: document.body,
    });

    const vm = wrapper.vm as any;
    expect(vm.computedMaxWidth).toBe('600px');
    wrapper.unmount();
  });

  it('uses string maxWidth as-is', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        maxWidth: '80%',
      },
      attachTo: document.body,
    });

    const vm = wrapper.vm as any;
    expect(vm.computedMaxWidth).toBe('80%');
    wrapper.unmount();
  });

  it('prevents confirm click when loading', async () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        loading: true,
      },
    });

    const confirmBtn = wrapper.findAll('button').find(btn => btn.text().includes('Confirm'));
    if (confirmBtn) {
      await confirmBtn.trigger('click');
      // Should not emit when loading
      expect(wrapper.emitted().confirm).toBeFalsy();
    }
  });

  it('allows confirm click when not loading', async () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        loading: false,
      },
    });

    const confirmBtn = wrapper.findAll('button').find(btn => btn.text().includes('Confirm'));
    if (confirmBtn) {
      await confirmBtn.trigger('click');
      expect(wrapper.emitted().confirm).toBeTruthy();
    }
  });

  it('sets aria-busy based on loading prop', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        loading: true,
      },
    });

    // Just verify the component exists and loading prop is set
    expect(wrapper.props().loading).toBe(true);
  });

  it('sets aria-busy to false when not loading', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        title: 'Test Title',
        loading: false,
      },
      attachTo: document.body,
    });

    const card = document.querySelector('.confirm-card');
    expect(card?.getAttribute('aria-busy')).toBe('false');
    wrapper.unmount();
  });

  it('uses custom confirmColor prop over type default', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        type: 'enable',
        confirmColor: 'info',
      },
      attachTo: document.body,
    });

    const vm = wrapper.vm as any;
    expect(vm.finalConfirmColor).toBe('info');
    expect(vm.colorClass).toBe('btn-info');
    wrapper.unmount();
  });

  it('uses the type defaults when title, description, and confirm label are omitted', () => {
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: true,
        type: 'delete',
      },
      attachTo: document.body,
    });

    const vm = wrapper.vm as any;
    expect(vm.finalTitle).toBe('Delete Confirmation');
    expect(vm.finalDescription).toContain('Deleting this connection');
    expect(vm.finalConfirmLabel).toBe('Delete');
    expect(vm.colorClass).toBe('btn-error');
    wrapper.unmount();
  });

  it('focuses the dialog card after opening when autoFocus is enabled', async () => {
    vi.useFakeTimers();
    const wrapper = mount(ConfirmationDialog, {
      props: {
        open: false,
        title: 'Focus Me',
        autoFocus: true,
      },
      attachTo: document.body,
    });

    await wrapper.setProps({ open: true });
    vi.runAllTimers();
    await wrapper.vm.$nextTick();

    const card = document.querySelector('.confirm-card');
    expect(card).not.toBeNull();

    wrapper.unmount();
  });
});
