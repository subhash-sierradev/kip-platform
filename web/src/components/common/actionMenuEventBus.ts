/**
 * Event bus for coordinating ActionMenu state across instances.
 * Ensures only one menu can be open at a time.
 */

export const menuEventBus = {
  listeners: new Set<(menuId: symbol) => void>(),

  emit(menuId: symbol) {
    this.listeners.forEach(fn => fn(menuId));
  },

  subscribe(fn: (menuId: symbol) => void) {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  },
};
