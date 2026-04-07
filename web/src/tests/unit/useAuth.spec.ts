/* eslint-disable simple-import-sort/imports */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { defineComponent, nextTick, ref } from 'vue';
import { mount } from '@vue/test-utils';

let initSpy: ReturnType<typeof vi.fn> | null = null;
let authInfoRef: ReturnType<typeof ref> | null = null;
let loadingRef: ReturnType<typeof ref> | null = null;
let initializedRef: ReturnType<typeof ref> | null = null;

vi.mock('@/composables/useAuthInfo', () => ({
  useAuthInfo: vi.fn(() => {
    authInfoRef = ref({
      isAuth: true,
      token: 'tok-xyz',
      userName: 'Alice',
      userMail: 'alice@example.com',
      tenantId: 'tenant-1',
      userId: 'user-1',
      roles: ['admin'],
    });
    loadingRef = ref(false);
    initializedRef = ref(false);
    initSpy = vi.fn(async () => {
      initializedRef!.value = true;
    });
    return {
      authInfo: authInfoRef,
      loading: loadingRef,
      initialized: initializedRef,
      init: initSpy,
    };
  }),
}));

import { useAuth } from '@/composables/useAuth';

describe('useAuth', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    if (initializedRef) initializedRef.value = false;
    if (loadingRef) loadingRef.value = false;
  });

  it('calls init on mounted when not initialized and exposes computed values', async () => {
    const Comp = defineComponent({
      setup() {
        const auth = useAuth();
        return auth;
      },
      template: '<div></div>',
    });

    const wrapper = mount(Comp);
    await nextTick();

    expect(initSpy).toBeTruthy();
    expect(initSpy!).toHaveBeenCalledTimes(1);

    // Verify exposed computed values
    expect(wrapper.vm.token).toBe('tok-xyz');
    expect(wrapper.vm.isAuthenticated).toBe(true);
    expect(wrapper.vm.userInfo.userName).toBe('Alice');
    expect(wrapper.vm.userInfo.userMail).toBe('alice@example.com');
    expect(wrapper.vm.userInfo.tenantId).toBe('tenant-1');
    expect(wrapper.vm.userInfo.userId).toBe('user-1');
    expect(wrapper.vm.userInfo.roles).toEqual(['admin']);
  });

  it('does not call init if already initialized', async () => {
    // Need to reinitialize the mock for this test
    const mockUseAuthInfo = vi.mocked(await import('@/composables/useAuthInfo'));

    const newInitializedRef = ref(true); // Already initialized
    const newInitSpy = vi.fn();

    mockUseAuthInfo.useAuthInfo.mockReturnValueOnce({
      authInfo: ref({
        isAuth: true,
        token: 'tok-xyz',
        userName: 'Alice',
        userMail: 'alice@example.com',
        tenantId: 'tenant-1',
        userId: 'user-1',
        roles: ['admin'],
      }),
      loading: ref(false),
      initialized: newInitializedRef,
      init: newInitSpy,
    } as any);

    const Comp = defineComponent({
      setup() {
        const auth = useAuth();
        return auth;
      },
      template: '<div></div>',
    });

    mount(Comp);
    await nextTick();

    expect(newInitSpy).not.toHaveBeenCalled();
  });

  it('handles authentication error silently', async () => {
    initSpy = vi.fn().mockRejectedValueOnce(new Error('Auth failed'));

    const Comp = defineComponent({
      setup() {
        const auth = useAuth();
        return auth;
      },
      template: '<div></div>',
    });

    // Should not throw error
    expect(() => mount(Comp)).not.toThrow();
    await nextTick();

    expect(initSpy).toHaveBeenCalledTimes(1);
  });

  it('exposes loading state', async () => {
    const mockUseAuthInfo = vi.mocked(await import('@/composables/useAuthInfo'));

    const newLoadingRef = ref(true);

    mockUseAuthInfo.useAuthInfo.mockReturnValueOnce({
      authInfo: ref({
        isAuth: true,
        token: 'tok-xyz',
        userName: 'Alice',
        userMail: 'alice@example.com',
        tenantId: 'tenant-1',
        userId: 'user-1',
        roles: ['admin'],
      }),
      loading: newLoadingRef,
      initialized: ref(false),
      init: vi.fn(),
    } as any);

    const Comp = defineComponent({
      setup() {
        const auth = useAuth();
        return auth;
      },
      template: '<div></div>',
    });

    const wrapper = mount(Comp);
    await nextTick();

    expect(wrapper.vm.loading).toBe(true);
  });

  it('exposes initialized state', async () => {
    if (initializedRef) initializedRef.value = true;

    const Comp = defineComponent({
      setup() {
        const auth = useAuth();
        return auth;
      },
      template: '<div></div>',
    });

    const wrapper = mount(Comp);
    await nextTick();

    expect(wrapper.vm.initialized).toBe(true);
  });

  it('handles unauthenticated state', async () => {
    const mockUseAuthInfo = vi.mocked(await import('@/composables/useAuthInfo'));

    mockUseAuthInfo.useAuthInfo.mockReturnValueOnce({
      authInfo: ref({
        isAuth: false,
        token: undefined,
        userName: 'Unknown User',
        userMail: '',
        tenantId: '',
        userId: '',
        roles: [],
      }),
      loading: ref(false),
      initialized: ref(false),
      init: vi.fn(),
    } as any);

    const Comp = defineComponent({
      setup() {
        const auth = useAuth();
        return auth;
      },
      template: '<div></div>',
    });

    const wrapper = mount(Comp);
    await nextTick();

    expect(wrapper.vm.isAuthenticated).toBe(false);
    expect(wrapper.vm.token).toBeUndefined();
    expect(wrapper.vm.userInfo.userName).toBe('Unknown User');
    expect(wrapper.vm.userInfo.roles).toEqual([]);
  });

  it('exposes all user info fields correctly', async () => {
    const Comp = defineComponent({
      setup() {
        const auth = useAuth();
        return auth;
      },
      template: '<div></div>',
    });

    const wrapper = mount(Comp);
    await nextTick();

    const userInfo = wrapper.vm.userInfo;
    expect(userInfo).toHaveProperty('userName');
    expect(userInfo).toHaveProperty('userMail');
    expect(userInfo).toHaveProperty('tenantId');
    expect(userInfo).toHaveProperty('userId');
    expect(userInfo).toHaveProperty('roles');
  });
});
