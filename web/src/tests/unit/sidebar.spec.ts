import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';

import { useSidebarStore } from '@/store/sidebar';

describe('sidebar store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  describe('initial state', () => {
    it('starts with sidebar expanded (not collapsed)', () => {
      const store = useSidebarStore();

      expect(store.isCollapsed).toBe(false);
      expect(store.isHoverExpanded).toBe(false);
    });

    it('has effectiveWidth of 200px when expanded', () => {
      const store = useSidebarStore();

      expect(store.effectiveWidth).toBe(200);
    });
  });

  describe('toggleCollapse', () => {
    it('collapses sidebar when expanded', () => {
      const store = useSidebarStore();

      store.toggleCollapse();

      expect(store.isCollapsed).toBe(true);
      expect(store.effectiveWidth).toBe(90);
    });

    it('expands sidebar when collapsed', () => {
      const store = useSidebarStore();

      store.toggleCollapse(); // collapse first
      expect(store.isCollapsed).toBe(true);

      store.toggleCollapse(); // expand again
      expect(store.isCollapsed).toBe(false);
      expect(store.effectiveWidth).toBe(200);
    });

    it('can toggle multiple times', () => {
      const store = useSidebarStore();

      store.toggleCollapse();
      expect(store.isCollapsed).toBe(true);

      store.toggleCollapse();
      expect(store.isCollapsed).toBe(false);

      store.toggleCollapse();
      expect(store.isCollapsed).toBe(true);
    });
  });

  describe('setHoverExpanded', () => {
    it('sets hover expanded to true', () => {
      const store = useSidebarStore();

      store.setHoverExpanded(true);

      expect(store.isHoverExpanded).toBe(true);
    });

    it('sets hover expanded to false', () => {
      const store = useSidebarStore();
      store.setHoverExpanded(true);

      store.setHoverExpanded(false);

      expect(store.isHoverExpanded).toBe(false);
    });

    it('expands sidebar to 200px when hover is active on collapsed sidebar', () => {
      const store = useSidebarStore();
      store.toggleCollapse(); // collapse sidebar

      store.setHoverExpanded(true);

      expect(store.effectiveWidth).toBe(200);
    });

    it('returns to 90px when hover is removed from collapsed sidebar', () => {
      const store = useSidebarStore();
      store.toggleCollapse(); // collapse
      store.setHoverExpanded(true); // hover expand

      store.setHoverExpanded(false); // remove hover

      expect(store.effectiveWidth).toBe(90);
    });
  });

  describe('effectiveWidth computed', () => {
    it('returns 200 when sidebar is expanded', () => {
      const store = useSidebarStore();

      expect(store.effectiveWidth).toBe(200);
    });

    it('returns 90 when sidebar is collapsed and not hover expanded', () => {
      const store = useSidebarStore();
      store.toggleCollapse();

      expect(store.effectiveWidth).toBe(90);
    });

    it('returns 200 when sidebar is collapsed but hover expanded', () => {
      const store = useSidebarStore();
      store.toggleCollapse();
      store.setHoverExpanded(true);

      expect(store.effectiveWidth).toBe(200);
    });

    it('returns 200 when sidebar is expanded regardless of hover state', () => {
      const store = useSidebarStore();
      store.setHoverExpanded(true);

      expect(store.effectiveWidth).toBe(200);

      store.setHoverExpanded(false);

      expect(store.effectiveWidth).toBe(200);
    });
  });

  describe('complex interactions', () => {
    it('handles collapse -> hover -> unhover -> expand flow', () => {
      const store = useSidebarStore();

      // Start expanded
      expect(store.effectiveWidth).toBe(200);

      // Collapse
      store.toggleCollapse();
      expect(store.effectiveWidth).toBe(90);

      // Hover
      store.setHoverExpanded(true);
      expect(store.effectiveWidth).toBe(200);

      // Unhover
      store.setHoverExpanded(false);
      expect(store.effectiveWidth).toBe(90);

      // Expand permanently
      store.toggleCollapse();
      expect(store.effectiveWidth).toBe(200);
    });
  });
});
