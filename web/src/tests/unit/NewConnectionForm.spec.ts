import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import NewConnectionForm from '@/components/common/connectionstep/NewConnectionForm.vue';
import type {
  ConnectionStepConfig,
  ConnectionStepData,
  CredentialField,
  CredentialTypeOption,
} from '@/types/ConnectionStepData';

describe('NewConnectionForm', () => {
  const baseModel: ConnectionStepData = {
    connectionMethod: 'new',
    baseUrl: '',
    credentialType: 'BASIC_AUTH',
    connectionName: '',
    connected: false,
    username: '',
    password: '',
  };

  const config: ConnectionStepConfig = {
    serviceType: 'JIRA',
    baseUrlLabel: 'Base URL',
    baseUrlPlaceholder: 'https://example.com',
    supportsDynamicCredentials: true,
    showCredentialTypeSelector: true,
    defaultCredentialType: 'BASIC_AUTH',
    requiresConnectionName: true,
  };

  const fields: CredentialField[] = [
    { key: 'username', label: 'Username', type: 'text', required: true },
    { key: 'password', label: 'Password', type: 'password', required: true },
  ];

  const credentialTypes: CredentialTypeOption[] = [
    { value: 'BASIC_AUTH', label: 'Basic Auth', fields },
    { value: 'OAUTH2', label: 'OAuth2', fields: [] },
  ];

  const mountForm = () =>
    mount(NewConnectionForm, {
      props: {
        modelValue: { ...baseModel },
        config,
        credentialTypes,
        currentCredentialFields: fields,
        useTwoColCredentialGrid: false,
        canTestConnection: true,
        isTesting: false,
        tested: false,
        testSuccess: false,
        testMessage: '',
        testButtonText: 'Test Connection',
      },
      global: {
        stubs: { Eye: true, EyeOff: true },
      },
    });

  it('emits update:modelValue when base URL changes', async () => {
    const wrapper = mountForm();

    const urlInput = wrapper.find('input[type="url"]');
    await urlInput.setValue('https://jira.example.com');

    const emitted = wrapper.emitted('update:modelValue');
    expect(emitted).toBeTruthy();
    const lastPayload = emitted?.[emitted.length - 1]?.[0] as ConnectionStepData;
    expect(lastPayload.baseUrl).toBe('https://jira.example.com');
  });

  it('emits credential-type-change on select change', async () => {
    const wrapper = mountForm();

    await wrapper.find('select').setValue('OAUTH2');

    expect(wrapper.emitted('credential-type-change')).toEqual([[]]);
  });

  it('emits test-connection when test button is clicked', async () => {
    const wrapper = mountForm();

    await wrapper.find('button.cs-test-btn').trigger('click');

    expect(wrapper.emitted('test-connection')).toEqual([[]]);
  });

  it('toggles password visibility', async () => {
    const wrapper = mountForm();

    const passwordInput = wrapper.find('input[type="password"]');
    expect(passwordInput.exists()).toBe(true);

    await wrapper.find('button.cs-icon-btn').trigger('click');
    expect(wrapper.find('input[type="text"]').exists()).toBe(true);
  });
});
