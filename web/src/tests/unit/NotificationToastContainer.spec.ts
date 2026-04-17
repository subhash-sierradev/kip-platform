/* eslint-disable simple-import-sort/imports */
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { reactive } from 'vue';

const removeNotificationToast = vi.fn();
const push = vi.fn();

const storeState = reactive({
  notificationToasts: [] as any[],
  removeNotificationToast,
});

vi.mock('@/store/notification', () => ({
  useNotificationStore: () => storeState,
}));

vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
}));

import NotificationToastContainer from '@/components/common/NotificationToastContainer.vue';

describe('NotificationToastContainer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    storeState.notificationToasts = [];
  });

  it('renders no cards when there are no toasts', () => {
    const wrapper = mount(NotificationToastContainer);
    expect(wrapper.findAll('.ntf-card')).toHaveLength(0);
  });

  it('renders toasts and supports dismiss action', async () => {
    storeState.notificationToasts = [
      {
        id: 't1',
        severity: 'INFO',
        title: 'Info title',
        message: 'Info message',
      },
    ] as any;

    const wrapper = mount(NotificationToastContainer, { attachTo: document.body });

    expect(document.body.querySelectorAll('.ntf-card')).toHaveLength(1);
    expect(document.body.textContent).toContain('Info title');

    (document.body.querySelector('.ntf-close') as HTMLButtonElement).click();
    expect(removeNotificationToast).toHaveBeenCalledWith('t1');
    wrapper.unmount();
  });

  it('renders primary action link and routes internally', async () => {
    storeState.notificationToasts = [
      {
        id: 't2',
        severity: 'SUCCESS',
        title: 'ArcGIS Job Completed',
        message: 'completed',
        metadata: {
          actionLabel: 'View Integration',
          route: '/outbound/integration/arcgis/123',
        },
      },
    ] as any;

    const wrapper = mount(NotificationToastContainer, { attachTo: document.body });
    const actionLink = document.body.querySelector('.ntf-action-link') as HTMLAnchorElement;

    expect(actionLink).toBeTruthy();
    expect(actionLink.textContent).toContain('View Integration');
    actionLink.click();

    expect(push).toHaveBeenCalledWith('/outbound/integration/arcgis/123');
    wrapper.unmount();
  });

  it('routes to Jira webhook details from toast link', async () => {
    storeState.notificationToasts = [
      {
        id: 't4',
        severity: 'SUCCESS',
        title: 'Jira Webhook Triggered',
        message: 'done',
        metadata: {
          actionLabel: 'View Webhook',
          route: '/outbound/webhook/jira/123',
        },
      },
    ] as any;

    const wrapper = mount(NotificationToastContainer, { attachTo: document.body });
    const actionLink = document.body.querySelector('.ntf-action-link') as HTMLAnchorElement;

    expect(actionLink).toBeTruthy();
    actionLink.click();

    expect(push).toHaveBeenCalledWith('/outbound/webhook/jira/123');
    wrapper.unmount();
  });

  it('opens external primary action in a new window', async () => {
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);
    storeState.notificationToasts = [
      {
        id: 't3',
        severity: 'INFO',
        title: 'External',
        message: 'open external',
        metadata: {
          actionLabel: 'Open Link',
          actionUrl: 'https://example.com',
        },
      },
    ] as any;

    const wrapper = mount(NotificationToastContainer, { attachTo: document.body });
    const actionLink = document.body.querySelector('.ntf-action-link') as HTMLAnchorElement;

    expect(actionLink).toBeTruthy();
    actionLink.click();

    expect(openSpy).toHaveBeenCalledWith('https://example.com', '_blank', 'noopener,noreferrer');
    expect(push).not.toHaveBeenCalled();
    openSpy.mockRestore();
    wrapper.unmount();
  });

  it('does not render a duplicate message body when the message matches the title', () => {
    storeState.notificationToasts = [
      {
        id: 't5',
        severity: 'INFO',
        title: 'Same text',
        message: 'Same text',
      },
    ] as any;

    const wrapper = mount(NotificationToastContainer, { attachTo: document.body });

    expect(document.body.querySelector('.ntf-message')).toBeNull();
    wrapper.unmount();
  });

  it('does not render an action link when the toast has no primary action metadata', () => {
    storeState.notificationToasts = [
      {
        id: 't6',
        severity: 'WARNING',
        title: 'No action',
        message: 'Just informational',
        metadata: {},
      },
    ] as any;

    const wrapper = mount(NotificationToastContainer, { attachTo: document.body });

    expect(document.body.querySelector('.ntf-action-link')).toBeNull();
    wrapper.unmount();
  });
});
