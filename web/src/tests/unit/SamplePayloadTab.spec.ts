/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { nextTick } from 'vue';
import SamplePayloadTab from '@/components/outbound/jirawebhooks/details/SamplePayloadTab.vue';

const makeWebhook = (payload: any) => ({
  id: 'w1',
  name: 'X',
  description: '',
  webhookUrl: '',
  connectionId: '',
  jiraFieldMappings: [],
  samplePayload: payload,
  isEnabled: true,
  isDeleted: false,
  normalizedName: 'x',
  createdBy: '',
  createdDate: '',
  lastModifiedBy: '',
  lastModifiedDate: '',
  tenantId: '',
  version: 1,
});

describe('SamplePayloadTab', () => {
  let clipboardWriteTextMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    // Mock clipboard API
    clipboardWriteTextMock = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, {
      clipboard: {
        writeText: clipboardWriteTextMock,
      },
    });
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  describe('Payload Display - Object Payloads', () => {
    it('renders formatted JSON when webhookData has object payload', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ a: 1, b: { c: 2 } }) },
      });
      const text = wrapper.find('.json-content').text();
      expect(text).toContain('"a": 1');
      expect(text).toContain('"c": 2');
    });

    it('formats nested object payload with proper indentation', () => {
      const complexPayload = {
        user: {
          profile: {
            name: 'John Doe',
            email: 'john@example.com',
          },
          settings: {
            theme: 'dark',
          },
        },
      };
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook(complexPayload) },
      });
      const text = wrapper.find('.json-content').text();
      expect(text).toContain('"user"');
      expect(text).toContain('"profile"');
      expect(text).toContain('"John Doe"');
      expect(text).toContain('"email"');
    });

    it('handles array values in payload', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ items: ['a', 'b', 'c'], count: 3 }) },
      });
      const text = wrapper.find('.json-content').text();
      expect(text).toContain('"items"');
      expect(text).toContain('"a"');
      expect(text).toContain('"b"');
      expect(text).toContain('"c"');
    });

    it('handles null and undefined values in object payload', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ key: null, other: 'value' }) },
      });
      const text = wrapper.find('.json-content').text();
      expect(text).toContain('null');
      expect(text).toContain('"other"');
    });

    it('handles empty object payload', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({}) },
      });
      const text = wrapper.find('.json-content').text();
      expect(text).toBe('{}');
    });

    it('handles boolean values in payload', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ active: true, deleted: false }) },
      });
      const text = wrapper.find('.json-content').text();
      expect(text).toContain('true');
      expect(text).toContain('false');
    });

    it('handles numeric values including zero', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ count: 0, total: 100, rate: 3.14 }) },
      });
      const text = wrapper.find('.json-content').text();
      expect(text).toContain('0');
      expect(text).toContain('100');
      expect(text).toContain('3.14');
    });
  });

  describe('Payload Display - String Payloads', () => {
    it('parses and formats valid JSON string payload', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w2', webhookData: makeWebhook('{"x":3}') },
      });
      expect(wrapper.find('.json-content').text()).toContain('"x": 3');
    });

    it('formats complex JSON string payload', () => {
      const jsonString = '{"user":{"name":"Alice","roles":["admin","user"]}}';
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w2', webhookData: makeWebhook(jsonString) },
      });
      const text = wrapper.find('.json-content').text();
      expect(text).toContain('"user"');
      expect(text).toContain('"Alice"');
      expect(text).toContain('"roles"');
    });

    it('displays invalid JSON string as-is without formatting', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w3', webhookData: makeWebhook('not-json') },
      });
      expect(wrapper.find('.json-content').text()).toContain('not-json');
    });

    it('handles empty string payload', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w3', webhookData: makeWebhook('') },
      });
      expect(wrapper.find('.json-content').exists()).toBe(true);
    });

    it('handles plain text string payload', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w3', webhookData: makeWebhook('This is plain text') },
      });
      expect(wrapper.find('.json-content').text()).toBe('This is plain text');
    });

    it('handles JSON string with special characters', () => {
      const jsonString = '{"message":"Hello\\nWorld\\t!","path":"C:\\\\Users\\\\test"}';
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w2', webhookData: makeWebhook(jsonString) },
      });
      const text = wrapper.find('.json-content').text();
      expect(text).toContain('"message"');
      expect(text).toContain('"path"');
    });
  });

  describe('Null/Undefined/Missing Payload Handling', () => {
    it('shows loading message when webhookData is null', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w4', webhookData: null },
      });
      expect(wrapper.find('.json-content').text()).toContain('Loading webhook data');
    });

    it('shows no payload message when samplePayload is undefined', () => {
      const webhook = makeWebhook(undefined);
      webhook.samplePayload = undefined as any;
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w5', webhookData: webhook },
      });
      expect(wrapper.find('.json-content').text()).toContain('No sample payload available');
    });

    it('shows no payload message when samplePayload is null', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w6', webhookData: makeWebhook(null) },
      });
      expect(wrapper.find('.json-content').text()).toContain('No sample payload available');
    });

    it('shows no payload message when samplePayload is explicitly missing', () => {
      const webhook = { ...makeWebhook({}) };
      delete (webhook as any).samplePayload;
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w7', webhookData: webhook },
      });
      expect(wrapper.find('.json-content').text()).toContain('No sample payload available');
    });
  });

  describe('Copy to Clipboard Functionality', () => {
    it('renders copy button', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ test: 'data' }) },
      });
      expect(wrapper.find('.copy-button').exists()).toBe(true);
    });

    it('displays "Copy" text initially', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ test: 'data' }) },
      });
      expect(wrapper.find('.copy-text').text()).toBe('Copy');
    });

    it('copies formatted payload to clipboard when button clicked', async () => {
      const payload = { field: 'value', nested: { data: 123 } };
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook(payload) },
      });

      await wrapper.find('.copy-button').trigger('click');
      await nextTick();

      expect(clipboardWriteTextMock).toHaveBeenCalledTimes(1);
      const copiedText = clipboardWriteTextMock.mock.calls[0][0];
      expect(copiedText).toContain('"field": "value"');
      expect(copiedText).toContain('"nested"');
    });

    it('changes button text to "Copied!" after successful copy', async () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ test: 'data' }) },
      });

      await wrapper.find('.copy-button').trigger('click');
      await nextTick();

      expect(wrapper.find('.copy-text').text()).toBe('Copied!');
    });

    it('disables button while copying', async () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ test: 'data' }) },
      });

      await wrapper.find('.copy-button').trigger('click');
      await nextTick();

      const button = wrapper.find('.copy-button');
      expect(button.attributes('disabled')).toBeDefined();
    });

    it('resets button text after 2 seconds', async () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ test: 'data' }) },
      });

      await wrapper.find('.copy-button').trigger('click');
      await nextTick();
      expect(wrapper.find('.copy-text').text()).toBe('Copied!');

      vi.advanceTimersByTime(2000);
      await nextTick();

      expect(wrapper.find('.copy-text').text()).toBe('Copy');
    });

    it('re-enables button after timeout', async () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ test: 'data' }) },
      });

      await wrapper.find('.copy-button').trigger('click');
      await nextTick();
      expect(wrapper.find('.copy-button').attributes('disabled')).toBeDefined();

      vi.advanceTimersByTime(2000);
      await nextTick();

      expect(wrapper.find('.copy-button').attributes('disabled')).toBeUndefined();
    });

    it('handles clipboard API failure gracefully', async () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      clipboardWriteTextMock.mockRejectedValueOnce(new Error('Clipboard access denied'));

      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ test: 'data' }) },
      });

      await wrapper.find('.copy-button').trigger('click');
      await nextTick();

      expect(consoleErrorSpy).toHaveBeenCalledWith(
        'Failed to copy to clipboard:',
        expect.any(Error)
      );
      consoleErrorSpy.mockRestore();
    });

    it('handles missing clipboard API gracefully', async () => {
      const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
      Object.assign(navigator, { clipboard: null });

      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ test: 'data' }) },
      });

      await wrapper.find('.copy-button').trigger('click');
      await nextTick();

      expect(consoleWarnSpy).toHaveBeenCalledWith('Clipboard API not available');
      consoleWarnSpy.mockRestore();
    });
  });

  describe('UI Elements and Structure', () => {
    it('renders payload container', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ test: 'data' }) },
      });
      expect(wrapper.find('.payload-container').exists()).toBe(true);
    });

    it('renders json-viewer section', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ test: 'data' }) },
      });
      expect(wrapper.find('.json-viewer').exists()).toBe(true);
    });

    it('renders json-content with pre and code tags', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ test: 'data' }) },
      });
      expect(wrapper.find('pre.json-content').exists()).toBe(true);
      expect(wrapper.find('pre.json-content code').exists()).toBe(true);
    });

    it('displays copy icon in button', () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ test: 'data' }) },
      });
      expect(wrapper.find('.dx-icon-copy').exists()).toBe(true);
    });
  });

  describe('Edge Cases and Error Handling', () => {
    it('handles formatting error by converting to string', () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      // Create a circular reference that will cause JSON.stringify to throw
      const circular: any = { a: 1 };
      circular.self = circular;

      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook(circular) },
      });

      // Should fallback to String() conversion
      expect(wrapper.find('.json-content').exists()).toBe(true);
      expect(consoleErrorSpy).toHaveBeenCalledWith('Error formatting payload:', expect.any(Error));
      consoleErrorSpy.mockRestore();
    });

    it('updates display when webhookData prop changes', async () => {
      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook({ initial: 'data' }) },
      });
      expect(wrapper.find('.json-content').text()).toContain('"initial"');

      await wrapper.setProps({ webhookData: makeWebhook({ updated: 'data' }) });
      expect(wrapper.find('.json-content').text()).toContain('"updated"');
      expect(wrapper.find('.json-content').text()).not.toContain('"initial"');
    });

    it('handles very large payload gracefully', () => {
      const largePayload = Array.from({ length: 100 }, (_, i) => ({
        id: i,
        name: `Item ${i}`,
        data: `Data for item ${i}`,
      }));

      const wrapper = mount(SamplePayloadTab, {
        props: { webhookId: 'w1', webhookData: makeWebhook(largePayload) },
      });

      expect(wrapper.find('.json-content').exists()).toBe(true);
      const text = wrapper.find('.json-content').text();
      expect(text).toContain('"id"');
      expect(text).toContain('"name"');
    });
  });
});
