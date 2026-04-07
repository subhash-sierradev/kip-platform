import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { IntegrationConnectionService } from '@/api/services/IntegrationConnectionService';
import ConnectionStep from '@/components/common/connectionstep/ConnectionStep.vue';
import ExistingConnectionPanel from '@/components/common/connectionstep/ExistingConnectionPanel.vue';
import NewConnectionForm from '@/components/common/connectionstep/NewConnectionForm.vue';
import type { ConnectionStepData } from '@/types/ConnectionStepData';
import { JIRA_CONNECTION_CONFIG } from '@/utils/connectionStepConfig';

vi.mock('@/api/services/IntegrationConnectionService', () => ({
  IntegrationConnectionService: {
    getAllConnections: vi.fn(),
    testExistingConnection: vi.fn(),
    testAndCreateConnection: vi.fn(),
  },
}));

describe('ConnectionStep (existing connection)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue([] as any);
  });

  it('emits connection-success after successful verify of existing connection', async () => {
    vi.mocked(IntegrationConnectionService.testExistingConnection).mockResolvedValue({
      success: true,
      connectionStatus: 'SUCCESS',
      message: 'Connection verified',
    } as any);

    const modelValue: ConnectionStepData = {
      connectionMethod: 'existing',
      baseUrl: '',
      credentialType: 'BASIC_AUTH',
      username: '',
      password: '',
      connectionName: '',
      connected: false,
      existingConnectionId: 'conn-1',
      createdConnectionId: undefined,
    };

    const wrapper = mount(ConnectionStep, {
      props: {
        modelValue,
        config: JIRA_CONNECTION_CONFIG,
      },
    });

    await flushPromises();

    // With an existing connection selected but not yet verified,
    // the step must start invalid (Next must be blocked).
    const emittedBeforeVerify = wrapper.emitted() as Record<string, unknown[][]>;
    expect(emittedBeforeVerify['validation-change']).toBeTruthy();
    expect(
      (emittedBeforeVerify['validation-change'] ?? []).some(args => (args[0] as boolean) === false)
    ).toBe(true);

    const verifyBtn = wrapper.find('button.cs-btn-outlined-primary');
    expect(verifyBtn.exists()).toBe(true);
    expect(verifyBtn.attributes('disabled')).toBeUndefined();

    await verifyBtn.trigger('click');
    await flushPromises();

    const emitted = wrapper.emitted() as Record<string, unknown[][]>;
    expect(emitted['connection-success']).toBeTruthy();
    expect(emitted['connection-success']?.[0]?.[0]).toBe('conn-1');

    // Should also mark step valid
    expect(emitted['validation-change']).toBeTruthy();
    expect(emitted['validation-change']?.some(args => (args[0] as boolean) === true)).toBe(true);
  });

  it('should refresh connections list when switching to existing method', async () => {
    const mockConnectionsInitial = [
      {
        id: 'conn-1',
        name: 'Initial Connection',
        integrationSecret: { secretName: 'secret-1', baseUrl: 'https://initial.com' },
        lastConnectionStatus: 'SUCCESS',
      },
    ];

    const mockConnectionsAfterRefresh = [
      {
        id: 'conn-1',
        name: 'Initial Connection',
        integrationSecret: { secretName: 'secret-1', baseUrl: 'https://initial.com' },
        lastConnectionStatus: 'SUCCESS',
      },
      {
        id: 'conn-2',
        name: 'Newly Created Connection',
        integrationSecret: { secretName: 'secret-2', baseUrl: 'https://new.com' },
        lastConnectionStatus: 'SUCCESS',
      },
    ];

    vi.mocked(IntegrationConnectionService.getAllConnections)
      .mockResolvedValueOnce(mockConnectionsInitial as any)
      .mockResolvedValueOnce(mockConnectionsAfterRefresh as any);

    const modelValue: ConnectionStepData = {
      connectionMethod: 'new',
      baseUrl: '',
      credentialType: 'BASIC_AUTH',
      username: '',
      password: '',
      connectionName: '',
      connected: false,
      existingConnectionId: undefined,
      createdConnectionId: undefined,
    };

    const wrapper = mount(ConnectionStep, {
      props: {
        modelValue,
        config: JIRA_CONNECTION_CONFIG,
      },
    });

    await flushPromises();

    // Initial fetch should have 1 connection
    expect(IntegrationConnectionService.getAllConnections).toHaveBeenCalledTimes(1);

    // Switch to 'existing' method
    const existingRadio = wrapper.findAll('input[type="radio"]')[1];
    await existingRadio.setValue(true);
    await flushPromises();

    // Should have fetched connections again
    expect(IntegrationConnectionService.getAllConnections).toHaveBeenCalledTimes(2);
  });

  it('should auto-refresh connections after successful new connection creation', async () => {
    const mockConnectionsInitial = [
      {
        id: 'conn-1',
        name: 'Existing Connection',
        integrationSecret: { secretName: 'secret-1', baseUrl: 'https://existing.com' },
        lastConnectionStatus: 'SUCCESS',
      },
    ];

    const mockConnectionsAfterCreation = [
      {
        id: 'conn-1',
        name: 'Existing Connection',
        integrationSecret: { secretName: 'secret-1', baseUrl: 'https://existing.com' },
        lastConnectionStatus: 'SUCCESS',
      },
      {
        id: 'conn-new',
        name: 'Newly Created Connection',
        integrationSecret: { secretName: 'secret-new', baseUrl: 'https://new.com' },
        lastConnectionStatus: 'SUCCESS',
      },
    ];

    vi.mocked(IntegrationConnectionService.getAllConnections)
      .mockResolvedValueOnce(mockConnectionsInitial as any)
      .mockResolvedValueOnce(mockConnectionsAfterCreation as any);

    vi.mocked(IntegrationConnectionService.testAndCreateConnection).mockResolvedValue({
      id: 'conn-new',
      name: 'Newly Created Connection',
      integrationSecret: { secretName: 'secret-new', baseUrl: 'https://new.com' },
      lastConnectionStatus: 'SUCCESS',
      message: 'Connection successful',
    } as any);

    const modelValue: ConnectionStepData = {
      connectionMethod: 'new',
      baseUrl: 'https://new.com',
      credentialType: 'BASIC_AUTH',
      username: 'testuser',
      password: 'testpass',
      connectionName: 'Newly Created Connection',
      connected: false,
      existingConnectionId: undefined,
      createdConnectionId: undefined,
    };

    const wrapper = mount(ConnectionStep, {
      props: {
        modelValue,
        config: JIRA_CONNECTION_CONFIG,
      },
    });

    await flushPromises();

    // Initial fetch
    expect(IntegrationConnectionService.getAllConnections).toHaveBeenCalledTimes(1);

    // Find and click Test Connection button
    const testButton = wrapper.find('button.cs-test-btn');
    await testButton.trigger('click');
    await flushPromises();

    // After successful test and creation, connections should be refreshed
    expect(IntegrationConnectionService.getAllConnections).toHaveBeenCalledTimes(2);

    // Should have emitted connection-success
    const emitted = wrapper.emitted() as Record<string, unknown[][]>;
    expect(emitted['connection-success']).toBeTruthy();
    expect(emitted['connection-success']?.[0]?.[0]).toBe('conn-new');
  });

  it('renders the expected child panel for each connection method', async () => {
    const modelValue: ConnectionStepData = {
      connectionMethod: 'existing',
      baseUrl: '',
      credentialType: 'BASIC_AUTH',
      username: '',
      password: '',
      connectionName: '',
      connected: false,
      existingConnectionId: undefined,
      createdConnectionId: undefined,
    };

    const wrapper = mount(ConnectionStep, {
      props: {
        modelValue,
        config: JIRA_CONNECTION_CONFIG,
      },
    });

    await flushPromises();

    expect(wrapper.findComponent(ExistingConnectionPanel).exists()).toBe(true);
    expect(wrapper.findComponent(NewConnectionForm).exists()).toBe(false);

    await wrapper.setProps({
      modelValue: {
        ...modelValue,
        connectionMethod: 'new',
      },
    });
    await flushPromises();

    expect(wrapper.findComponent(ExistingConnectionPanel).exists()).toBe(false);
    expect(wrapper.findComponent(NewConnectionForm).exists()).toBe(true);
  });
});
