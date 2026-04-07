/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi } from 'vitest';
import WebhookSuccessDialog from '@/components/outbound/jirawebhooks/wizard/WebhookSuccessDialog.vue';

describe('WebhookSuccessDialog', () => {
  it('emits copy-url and close events', async () => {
    // Mock clipboard API
    // @ts-expect-error augment navigator for test
    globalThis.navigator.clipboard = { writeText: vi.fn().mockResolvedValue(undefined) } as any;
    const wrapper = mount(WebhookSuccessDialog, {
      props: { open: true, webhook: { webhookUrl: 'https://example.com/hook' } },
    });

    // Copy button emits copy-url
    const copyBtn = wrapper.find('.wsd-copy-icon');
    await copyBtn.trigger('click');
    expect(wrapper.emitted('copy-url')).toBeTruthy();

    // Close button emits close
    const okBtn = wrapper.find('.wsd-primary-btn');
    await okBtn.trigger('click');
    expect(wrapper.emitted('close')).toBeTruthy();
  });
});
