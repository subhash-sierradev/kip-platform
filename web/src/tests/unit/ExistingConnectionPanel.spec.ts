import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import ExistingConnectionPanel from '@/components/common/connectionstep/ExistingConnectionPanel.vue';
import type { ConnectionStatusDetails, SavedConnection } from '@/types/ConnectionStepData';

describe('ExistingConnectionPanel', () => {
  const connection: SavedConnection = {
    id: 'conn-1',
    name: 'Primary Connection',
    secretName: 'secret-1',
    baseUrl: 'https://example.com',
    lastConnectionStatus: 'SUCCESS',
    lastConnectionTest: '2024-01-01T00:00:00Z',
  };

  const getConnectionStatus = (): ConnectionStatusDetails => ({
    label: 'Active',
    severity: 'success',
  });

  const formatLastTested = () => 'Today';

  it('emits select-existing when a connection is chosen', async () => {
    const wrapper = mount(ExistingConnectionPanel, {
      props: {
        existingConnections: [connection],
        existingConnectionId: undefined,
        loading: false,
        activeCount: 1,
        failedCount: 0,
        isTestingExisting: false,
        existingTested: false,
        existingTestSuccess: false,
        existingTestMessage: '',
        verifyButtonText: 'Verify',
        getConnectionStatus,
        formatLastTested,
      },
    });

    await wrapper.find('.cs-dropdown-selected').trigger('click');
    await wrapper.find('.cs-option').trigger('click');

    expect(wrapper.emitted('select-existing')).toEqual([[connection]]);
  });

  it('disables verify button when no existing connection is selected', async () => {
    const wrapper = mount(ExistingConnectionPanel, {
      props: {
        existingConnections: [connection],
        existingConnectionId: undefined,
        loading: false,
        activeCount: 1,
        failedCount: 0,
        isTestingExisting: false,
        existingTested: false,
        existingTestSuccess: false,
        existingTestMessage: '',
        verifyButtonText: 'Verify',
        getConnectionStatus,
        formatLastTested,
      },
    });

    const button = wrapper.find('button.cs-btn-outlined-primary');
    expect(button.attributes('disabled')).toBeDefined();

    await wrapper.setProps({ existingConnectionId: 'conn-1' });
    expect(button.attributes('disabled')).toBeUndefined();
  });

  it('emits verify-existing when verify is clicked', async () => {
    const wrapper = mount(ExistingConnectionPanel, {
      props: {
        existingConnections: [connection],
        existingConnectionId: 'conn-1',
        loading: false,
        activeCount: 1,
        failedCount: 0,
        isTestingExisting: false,
        existingTested: false,
        existingTestSuccess: false,
        existingTestMessage: '',
        verifyButtonText: 'Verify',
        getConnectionStatus,
        formatLastTested,
      },
    });

    await wrapper.find('button.cs-btn-outlined-primary').trigger('click');

    expect(wrapper.emitted('verify-existing')).toEqual([[]]);
  });
});
