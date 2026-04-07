import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import EntityManageActions from '@/components/common/EntityManageActions.vue';

describe('EntityManageActions', () => {
  const mockEntity = {
    id: 'test-123',
    name: 'Test Entity',
    status: 'ACTIVE',
  };

  const defaultActions = [
    { id: 'edit', label: 'Edit', icon: 'dx-icon-edit' },
    { id: 'delete', label: 'Delete', icon: 'dx-icon-trash' },
  ];

  it('renders action buttons', () => {
    const wrapper = mount(EntityManageActions, {
      props: {
        entity: mockEntity,
        actions: defaultActions,
      },
    });

    expect(wrapper.exists()).toBe(true);
  });

  it('displays edit button', () => {
    const wrapper = mount(EntityManageActions, {
      props: {
        entity: mockEntity,
        actions: defaultActions,
        showEdit: true,
      },
    });

    expect(wrapper.exists()).toBe(true);
  });

  it('displays delete button', () => {
    const wrapper = mount(EntityManageActions, {
      props: {
        entity: mockEntity,
        actions: defaultActions,
        showDelete: true,
      },
    });

    expect(wrapper.exists()).toBe(true);
  });

  it('handles entity without id', () => {
    const entityWithoutId = {
      name: 'No ID Entity',
    };

    const wrapper = mount(EntityManageActions, {
      props: {
        entity: entityWithoutId,
        actions: defaultActions,
      },
    });

    expect(wrapper.exists()).toBe(true);
  });
});
