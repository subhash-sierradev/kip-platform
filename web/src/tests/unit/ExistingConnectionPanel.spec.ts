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
        verifyButtonText: 'Test Connection',
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
        verifyButtonText: 'Test Connection',
        getConnectionStatus,
        formatLastTested,
      },
    });

    const button = wrapper.find('button.cs-test-btn');
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
        verifyButtonText: 'Test Connection',
        getConnectionStatus,
        formatLastTested,
      },
    });

    await wrapper.find('button.cs-test-btn').trigger('click');

    expect(wrapper.emitted('verify-existing')).toEqual([[]]);
  });

  it('renders loading and empty dropdown states', async () => {
    const wrapper = mount(ExistingConnectionPanel, {
      props: {
        existingConnections: [],
        existingConnectionId: undefined,
        loading: true,
        activeCount: 0,
        failedCount: 0,
        isTestingExisting: false,
        existingTested: false,
        existingTestSuccess: false,
        existingTestMessage: '',
        verifyButtonText: 'Test Connection',
        getConnectionStatus,
        formatLastTested,
      },
    });

    await wrapper.find('.cs-dropdown-selected').trigger('click');
    expect(wrapper.find('.cs-loading-row').exists()).toBe(true);

    await wrapper.setProps({ loading: false });
    expect(wrapper.find('.cs-empty').text()).toContain('No saved connections found');
  });

  it('renders selected and list metadata without secret separators when secretName is absent', async () => {
    const wrapper = mount(ExistingConnectionPanel, {
      props: {
        existingConnections: [
          {
            ...connection,
            id: 'conn-2',
            secretName: undefined,
          },
        ],
        existingConnectionId: 'conn-2',
        loading: false,
        activeCount: 1,
        failedCount: 0,
        isTestingExisting: false,
        existingTested: false,
        existingTestSuccess: false,
        existingTestMessage: '',
        verifyButtonText: 'Test Connection',
        getConnectionStatus,
        formatLastTested,
      },
    });

    expect(wrapper.find('.cs-connection-meta-line').text()).toContain('1 connection');
    expect(wrapper.find('.cs-selected-separator').exists()).toBe(false);
    expect(wrapper.find('.cs-selected-id').exists()).toBe(false);

    await wrapper.find('.cs-dropdown-selected').trigger('click');
    expect(wrapper.find('.cs-meta-separator').exists()).toBe(false);
    expect(wrapper.find('.cs-connection-secret').exists()).toBe(false);
  });

  it('renders verification success and in-progress states', () => {
    const wrapper = mount(ExistingConnectionPanel, {
      props: {
        existingConnections: [connection],
        existingConnectionId: 'conn-1',
        loading: false,
        activeCount: 1,
        failedCount: 0,
        isTestingExisting: true,
        existingTested: true,
        existingTestSuccess: true,
        existingTestMessage: 'Verified',
        verifyButtonText: 'Verifying...',
        getConnectionStatus,
        formatLastTested,
      },
    });

    expect(wrapper.find('.cs-loading-spinner').exists()).toBe(true);
    expect(wrapper.find('.cs-test-btn-success').exists()).toBe(true);
    expect(wrapper.find('.cs-verification-success').text()).toContain('Verified');
  });
});
