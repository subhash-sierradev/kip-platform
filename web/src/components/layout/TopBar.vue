<template>
  <header class="modern-topbar" role="banner">
    <div class="topbar-content">
      <!-- Left Section: Menu Toggle + Logo -->
      <div class="topbar-left">
        <DxButton
          icon="menu"
          styling-mode="text"
          :class="['menu-toggle', { collapsed }]"
          :width="40"
          :height="40"
          @click="toggleSidebar"
        />
        <div class="logo-section" @click="navigateToHome">
          <div class="logo">
            <img src="/kaseware.png" alt="Kaseware Logo" class="logo-image" />
            <div class="logo-text">
              <span class="brand-name">Kaseware</span>
              <span class="brand-subtitle">Integration Platform</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Section: Organization + Notifications + User -->
      <div class="topbar-right">
        <!-- Organization Badge -->
        <div class="organization-badge">
          <div class="org-icon">
            <i class="dx-icon dx-icon-group"></i>
          </div>
          <span class="org-label">{{ formattedTenantId }}</span>
        </div>

        <!-- Notifications -->
        <NotificationBell />

        <!-- Help Guide -->
        <div class="help-container">
          <div class="help-item" @click="toggleHelpGuide" title="Help & Documentation">
            <i class="dx-icon dx-icon-help help-icon"></i>
          </div>
        </div>

        <!-- User Profile -->
        <div ref="userMenuRef" class="user-menu-container">
          <div class="user-profile" @click="toggleUserMenu">
            <div class="user-icon">
              <i class="dx-icon dx-icon-user"></i>
            </div>
            <div class="user-info">
              <span class="user-name">{{ formattedUserName }}</span>
            </div>
            <div class="dropdown-arrow">
              <i
                class="dx-icon dx-icon-chevrondown arrow-icon"
                :class="{ rotated: showUserMenu }"
              ></i>
            </div>
          </div>

          <!-- User Dropdown Menu -->
          <div v-if="showUserMenu" class="user-dropdown" @click.stop>
            <div class="user-dropdown-header">
              <div class="user-icon">
                <i class="dx-icon dx-icon-mention"></i>
              </div>
              <span class="dropdown-user-name">{{ formattedUserId }}</span>
            </div>
            <div class="user-dropdown-header">
              <div class="user-icon">
                <i class="dx-icon dx-icon-email"></i>
              </div>
              <span class="dropdown-user-name">{{ formattedUserEmail }}</span>
            </div>

            <div class="user-dropdown-item" @click="handleLogout">
              <div class="logout-icon">
                <i class="dx-icon dx-icon-export"></i>
              </div>
              <span>Logout</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { DxButton } from 'devextreme-vue/button';
import { useAuthStore } from '../../store/auth';
import NotificationBell from './NotificationBell.vue';
import './layout.css';

defineOptions({ name: 'TopBar' });

const props = defineProps<{ collapsed?: boolean }>();
const emit = defineEmits<{
  toggle: [];
}>();

const router = useRouter();

// Use auth store as single source of truth
const authStore = useAuthStore();

const collapsed = computed(() => !!props.collapsed);
const showUserMenu = ref(false);

// Get formatted user and tenant data from auth store
const formattedUserName = computed(() => {
  return authStore.currentUser?.userName || 'Unknown User';
});

const formattedTenantId = computed(() => {
  return authStore.currentUser?.tenantId || 'No Tenant';
});

const formattedUserEmail = computed(() => {
  return authStore.currentUser?.userMail || 'No Email';
});

const formattedUserId = computed(() => {
  return authStore.currentUser?.userId || 'No User ID';
});

// Notification state is now handled by NotificationBell component

// Navigation function for logo click
function navigateToHome(): void {
  router.push('/');
}

// expose a wrapper for emitting, safer to use in template
const toggleSidebar = () => {
  emit('toggle');
};

function toggleUserMenu() {
  showUserMenu.value = !showUserMenu.value;
}

function toggleHelpGuide(): void {
  // TODO: Add help guide functionality
  // This could open a help modal, documentation panel, or external help site
  console.warn('Help guide clicked - functionality to be implemented');
}

function handleLogout(): void {
  showUserMenu.value = false;
  // Use auth store logout method
  authStore.logout().catch(() => {
    // Handle logout error silently in production
  });
}

// Use a template ref for the user menu container for robust click-outside detection
const userMenuRef = ref<HTMLElement | null>(null);

// Close dropdown when clicking outside
const handleClickOutside = (event: Event) => {
  const target = event.target as Node;
  if (!userMenuRef.value) return;
  if (!userMenuRef.value.contains(target)) {
    showUserMenu.value = false;
  }
};

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<style scoped>
/* TopBar styles are imported from layout.css */
</style>
