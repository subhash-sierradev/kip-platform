/* eslint-disable simple-import-sort/imports */
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { nextTick, ref } from 'vue';

const { mockFetchCredentialTypes, mockCanTestConnection, mockSubmitConnection } = vi.hoisted(
  () => ({
    mockFetchCredentialTypes: vi.fn(),
    mockCanTestConnection: { value: true },
    mockSubmitConnection: vi.fn(),
  })
);

vi.mock('@/composables/useCredentialTypes', () => ({
  useCredentialTypes: () => ({
    credentialTypes: ref([
      {
        value: 'BASIC_AUTH',
        label: 'Basic Auth',
        fields: [
          { key: 'username', label: 'Username', type: 'text', required: true },
          { key: 'password', label: 'Password', type: 'password', required: true },
        ],
      },
    ]),
    fetchCredentialTypes: mockFetchCredentialTypes,
  }),
}));

vi.mock('@/composables/useConnectionValidation', () => ({
  useConnectionValidation: () => ({
    canTestConnection: mockCanTestConnection,
  }),
}));

const NewConnectionFormStub = {
  props: [
    'modelValue',
    'tested',
    'testSuccess',
    'testMessage',
    'testButtonText',
    'isTesting',
    'canTestConnection',
  ],
  emits: ['update:modelValue', 'credential-type-change', 'test-connection'],
  template:
    "<div class=\"new-connection-form-stub\"><span class=\"test-state\">{{ tested }}|{{ testSuccess }}|{{ testMessage }}|{{ testButtonText }}</span><button class=\"seed-create-form\" @click=\"$emit('update:modelValue', { connectionMethod: 'new', baseUrl: 'https://jira.example.com', credentialType: 'BASIC_AUTH', username: 'user', password: 'secret', connectionName: 'Admin Connection', connected: false })\">seed</button><button class=\"submit-create-form\" :disabled=\"!canTestConnection\" @click=\"$emit('test-connection')\">submit</button></div>",
};

const AppModalStub = {
  props: ['open'],
  emits: ['update:open'],
  template: '<div v-if="open" class="app-modal-stub"><slot /></div>',
};

import AddConnectionDialog from '@/components/common/AddConnectionDialog.vue';
import { ServiceType } from '@/api/models/enums';

function mountDialog() {
  return mount(AddConnectionDialog, {
    props: {
      open: true,
      serviceType: ServiceType.JIRA,
      submitConnection: mockSubmitConnection,
    },
    global: {
      stubs: {
        AppModal: AppModalStub,
        NewConnectionForm: NewConnectionFormStub,
      },
    },
  });
}

async function openDialog() {
  await nextTick();
  await Promise.resolve();
  await nextTick();
}

describe('AddConnectionDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockCanTestConnection.value = true;
  });

  it('fetches credential types when opened', async () => {
    mountDialog();
    await openDialog();

    expect(mockFetchCredentialTypes).toHaveBeenCalled();
  });

  it('emits created and closes on successful create', async () => {
    mockSubmitConnection.mockResolvedValueOnce({
      lastConnectionStatus: 'SUCCESS',
      lastConnectionMessage: 'Created',
    });

    const wrapper = mountDialog();
    await openDialog();
    await wrapper.find('.seed-create-form').trigger('click');
    await wrapper.find('.submit-create-form').trigger('click');
    await nextTick();

    expect(mockSubmitConnection).toHaveBeenCalledWith({
      connectionMethod: 'new',
      baseUrl: 'https://jira.example.com',
      credentialType: 'BASIC_AUTH',
      username: 'user',
      password: 'secret',
      connectionName: 'Admin Connection',
      connected: false,
    });
    expect(wrapper.emitted('created')).toBeTruthy();
    expect(wrapper.emitted('update:open')).toEqual([[false]]);
  });

  it('keeps dialog open and exposes sanitized failure message on unsuccessful create', async () => {
    mockSubmitConnection.mockResolvedValueOnce({
      lastConnectionStatus: 'FAILED',
      lastConnectionMessage: '<p>Invalid credentials</p>',
    });

    const wrapper = mountDialog();
    await openDialog();
    await wrapper.find('.seed-create-form').trigger('click');
    await wrapper.find('.submit-create-form').trigger('click');
    await nextTick();

    expect(wrapper.emitted('created')).toBeFalsy();
    expect(wrapper.emitted('update:open')).toBeFalsy();
    expect(wrapper.find('.test-state').text()).toContain('true|false|Invalid credentials');
  });

  it('keeps dialog open and exposes sanitized error message when create throws', async () => {
    mockSubmitConnection.mockRejectedValueOnce({
      body: { message: '<strong>Network failure</strong>' },
    });

    const wrapper = mountDialog();
    await openDialog();
    await wrapper.find('.seed-create-form').trigger('click');
    await wrapper.find('.submit-create-form').trigger('click');
    await nextTick();

    expect(wrapper.emitted('created')).toBeFalsy();
    expect(wrapper.emitted('update:open')).toBeFalsy();
    expect(wrapper.find('.test-state').text()).toContain('true|false|Network failure');
  });
});
