<template>
  <div class="action-menu" ref="menuContainer">
    <button
      @click.stop="toggleMenu"
      class="action-menu-trigger"
      :aria-label="triggerAriaLabel"
      :aria-expanded="isOpen"
      :disabled="disabled"
    >
      <svg viewBox="0 0 24 24" class="action-menu-icon">
        <circle cx="12" cy="5" r="2" fill="currentColor" />
        <circle cx="12" cy="12" r="2" fill="currentColor" />
        <circle cx="12" cy="19" r="2" fill="currentColor" />
      </svg>
    </button>

    <div v-if="isOpen" ref="dropdownElement" class="action-menu-dropdown" role="menu">
      <template v-for="item in items" :key="item.id">
        <button
          @click.stop="handleItemClick(item.id)"
          class="action-menu-item"
          :class="[item.variant && `variant-${item.variant}`]"
          :disabled="item.disabled"
          :aria-label="item.ariaLabel || item.label"
          role="menuitem"
        >
          <!-- DevExtreme icon -->
          <i
            v-if="item.iconType === 'devextreme' && item.icon"
            :class="[item.icon, 'action-menu-item-icon']"
          ></i>

          <!-- SVG icon -->
          <svg
            v-else-if="item.iconType === 'svg' && item.svgPath"
            viewBox="0 0 24 24"
            class="action-menu-item-icon"
          >
            <path fill="currentColor" :d="item.svgPath" />
          </svg>

          <!-- CSS icon (fallback) -->
          <i v-else-if="item.icon" :class="[item.icon, 'action-menu-item-icon']"></i>

          {{ item.label }}
        </button>

        <div v-if="item.divider" class="action-menu-divider" role="separator"></div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { menuEventBus } from './actionMenuEventBus';

/** Menu item configuration interface */
export interface ActionMenuItem {
  /** Unique identifier for the action */
  id: string;
  /** Display label for the menu item */
  label: string;
  /** Icon identifier (CSS class or DevExtreme class) */
  icon?: string;
  /** Type of icon to render */
  iconType?: 'devextreme' | 'svg' | 'css';
  /** SVG path data for svg icons */
  svgPath?: string;
  /** Visual style variant */
  variant?: 'primary' | 'secondary' | 'danger';
  /** Whether the item is disabled */
  disabled?: boolean;
  /** Add divider after this item */
  divider?: boolean;
  /** Custom aria-label for accessibility */
  ariaLabel?: string;
}

/** Component props interface */
export interface ActionMenuProps {
  /** Array of menu items to display */
  items: ActionMenuItem[];
  /** Whether the entire menu is disabled */
  disabled?: boolean;
  /** Aria-label for the trigger button */
  triggerAriaLabel?: string;
}

/** Component emits interface */
export interface ActionMenuEmits {
  /** Emitted when a menu item is clicked */
  (e: 'action', actionId: string): void;
}

// Props with defaults
const props = withDefaults(defineProps<ActionMenuProps>(), {
  disabled: false,
  triggerAriaLabel: 'More actions',
});

// Emits
const emit = defineEmits<ActionMenuEmits>();

// Component state
const isOpen = ref(false);
const menuContainer = ref<HTMLElement>();
const dropdownElement = ref<HTMLElement>();
const menuId = Symbol('menu-instance'); // Unique identifier for this menu instance

// Menu toggle logic
function toggleMenu(): void {
  if (props.disabled) return;
  isOpen.value = !isOpen.value;

  // When opening, notify other menus to close
  if (isOpen.value) {
    menuEventBus.emit(menuId);
  }

  // Position the fixed dropdown when opened
  if (isOpen.value && menuContainer.value) {
    // Use nextTick to ensure DOM is updated
    nextTick(() => {
      if (dropdownElement.value) {
        const triggerRect = menuContainer.value!.getBoundingClientRect();
        const dropdown = dropdownElement.value;

        // Position dropdown below trigger, aligned to the right edge
        const top = triggerRect.bottom + window.scrollY + 2;
        const left = triggerRect.right + window.scrollX - dropdown.offsetWidth;

        dropdown.style.top = `${top}px`;
        dropdown.style.left = `${left}px`;
      }
    });
  }
}

function closeMenu(): void {
  isOpen.value = false;
}

// Handle menu item clicks
function handleItemClick(actionId: string): void {
  closeMenu();
  emit('action', actionId);
}

// Click outside handling
function handleClickOutside(event: Event): void {
  if (menuContainer.value && !menuContainer.value.contains(event.target as Node)) {
    closeMenu();
  }
}

// Lifecycle hooks
onMounted(() => {
  document.addEventListener('click', handleClickOutside);

  // Subscribe to menu events - close this menu when another opens
  const unsubscribe = menuEventBus.subscribe(openedMenuId => {
    if (openedMenuId !== menuId && isOpen.value) {
      closeMenu();
    }
  });

  // Clean up subscription on unmount
  onUnmounted(() => {
    unsubscribe();
    document.removeEventListener('click', handleClickOutside);
  });
});
</script>

<style scoped>
/* Container */
.action-menu {
  position: relative;
}

/* Trigger button */
.action-menu-trigger {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-menu-trigger:hover:not(:disabled) {
  background-color: #f3f4f6;
}

.action-menu-trigger:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.action-menu-icon {
  width: 18px;
  height: 22px;
  color: #6b7280;
  transition: color 0.2s ease;
}

/* Dropdown menu */
.action-menu-dropdown {
  position: fixed;
  background: white;
  border: 1px solid #ccc;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  z-index: 999999;
  border-radius: 8px;
  padding: 6px 0;
  min-width: 140px;
  max-width: 200px;
  transform-origin: top right;
  /* Fallback positioning */
  top: 0;
  left: 0;
}

/* Menu items */
.action-menu-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  background: none;
  border: none;
  width: 100%;
  color: #333;
  white-space: nowrap;
  font-size: 14px;
  text-align: left;
  transition: background-color 0.2s ease;
}

.action-menu-item:hover:not(:disabled) {
  background-color: #f0f0f0;
}

.action-menu-item:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  color: #9ca3af;
}

.action-menu-item:focus {
  background-color: #f0f0f0;
  outline: none;
}

/* Menu item icons */
.action-menu-item-icon {
  margin-right: 8px;
  width: 20px;
  height: 20px;
  color: rgba(0, 0, 0, 0.6);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* Menu item variants */
.action-menu-item.variant-primary {
  color: #2563eb;
}

.action-menu-item.variant-primary .action-menu-item-icon {
  color: #2563eb;
}

.action-menu-item.variant-danger {
  color: #dc2626;
}

.action-menu-item.variant-danger .action-menu-item-icon {
  color: #dc2626;
}

.action-menu-item.variant-secondary {
  color: #6b7280;
}

.action-menu-item.variant-secondary .action-menu-item-icon {
  color: #6b7280;
}

/* Divider */
.action-menu-divider {
  height: 1px;
  background-color: #e5e7eb;
  margin: 4px 0;
}
</style>
