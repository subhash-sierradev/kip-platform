import { beforeEach, describe, expect, it, vi } from 'vitest';

const hoisted = vi.hoisted(() => ({
  getEventsMock: vi.fn(),
  createEventMock: vi.fn(),
  deactivateEventMock: vi.fn(),
  successMock: vi.fn(),
  errorMock: vi.fn(),
}));

vi.mock('@/api/services/NotificationAdminService', () => ({
  NotificationAdminService: {
    getEvents: hoisted.getEventsMock,
    createEvent: hoisted.createEventMock,
    deactivateEvent: hoisted.deactivateEventMock,
  },
}));

vi.mock('@/utils/notificationUtils', () => ({
  default: {
    success: hoisted.successMock,
    error: hoisted.errorMock,
  },
}));

import { useNotificationEvents } from '@/composables/useNotificationEvents';

describe('useNotificationEvents', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches events successfully and updates active event keys', async () => {
    hoisted.getEventsMock.mockResolvedValueOnce([
      { id: 'e1', eventKey: 'A', isActive: true },
      { id: 'e2', eventKey: null, isActive: true },
    ]);
    const state = useNotificationEvents();

    await state.fetchEvents();

    expect(state.events.value).toHaveLength(2);
    expect(state.loadingEvents.value).toBe(false);
    expect(Array.from(state.activeEventKeys.value)).toEqual(['A']);
  });

  it('handles fetch errors', async () => {
    hoisted.getEventsMock.mockRejectedValueOnce(new Error('boom'));
    const state = useNotificationEvents();

    await state.fetchEvents();

    expect(hoisted.errorMock).toHaveBeenCalledWith('Failed to load event catalog');
    expect(state.loadingEvents.value).toBe(false);
  });

  it('creates an event on success and returns false on failure', async () => {
    hoisted.createEventMock.mockResolvedValueOnce({ id: 'e3', eventKey: 'NEW', isActive: true });
    const state = useNotificationEvents();
    state.events.value = [{ id: 'e1', eventKey: 'OLD', isActive: true } as any];

    await expect(state.createEvent({} as any)).resolves.toBe(true);
    expect(state.events.value[0]).toEqual({ id: 'e3', eventKey: 'NEW', isActive: true });
    expect(hoisted.successMock).toHaveBeenCalledWith('Event created successfully');

    hoisted.createEventMock.mockRejectedValueOnce(new Error('bad'));
    await expect(state.createEvent({} as any)).resolves.toBe(false);
    expect(hoisted.errorMock).toHaveBeenCalledWith('Failed to create event');
    expect(state.saving.value).toBe(false);
  });

  it('deactivates an event and handles failure', async () => {
    const state = useNotificationEvents();
    state.events.value = [
      { id: 'e1', eventKey: 'OLD', isActive: true },
      { id: 'e2', eventKey: 'KEEP', isActive: true },
    ] as any;

    await state.deactivateEvent('e1');
    expect(state.events.value[0]).toMatchObject({ id: 'e1', isActive: false });
    expect(hoisted.successMock).toHaveBeenCalledWith('Event deactivated');

    hoisted.deactivateEventMock.mockRejectedValueOnce(new Error('bad'));
    await state.deactivateEvent('e2');
    expect(hoisted.errorMock).toHaveBeenCalledWith('Failed to deactivate event');
  });
});
