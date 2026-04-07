<template>
  <div class="auth-demo">
    <h2>Authentication Demo</h2>

    <div v-if="loading" class="loading">Loading authentication...</div>

    <div v-else-if="isAuthenticated" class="authenticated">
      <h3>Welcome, {{ userInfo.userName }}!</h3>

      <div class="user-details">
        <p><strong>Email:</strong> {{ userInfo.userMail }}</p>
        <p><strong>User ID:</strong> {{ userInfo.userId }}</p>
        <p><strong>Tenant ID:</strong> {{ userInfo.tenantId }}</p>
        <p><strong>Roles:</strong> {{ userInfo.roles.join(', ') }}</p>
      </div>

      <div class="actions">
        <button @click="handleLogout">Logout</button>
      </div>

      <div class="role-demo">
        <h4>Role-based Features:</h4>
        <p v-if="hasAdminRole">✅ You have admin access</p>
        <p v-else>❌ Admin access required</p>
      </div>
    </div>

    <div v-else class="not-authenticated">
      <h3>Not Authenticated</h3>
      <p>Please log in to access this application.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useAuthStore } from '@/store/auth';

const authStore = useAuthStore();

// Computed properties for template
const isAuthenticated = computed(() => authStore.isAuthenticated);
const loading = computed(() => authStore.loading);
const userInfo = computed(() => ({
  userName: authStore.currentUser?.userName || 'Unknown User',
  userMail: authStore.currentUser?.userMail || '',
  userId: authStore.currentUser?.userId || '',
  tenantId: authStore.currentUser?.tenantId || '',
  roles: authStore.currentUser?.roles || [],
}));

// Role-based access example
const hasAdminRole = computed(() => authStore.hasAnyRole(['admin', 'integration-admin']));

// Actions
const handleLogout = async () => {
  try {
    await authStore.logout();
  } catch (error) {
    console.error('Logout failed:', error);
  }
};
</script>

<style scoped>
.auth-demo {
  max-width: 600px;
  margin: 2rem auto;
  padding: 2rem;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.loading {
  text-align: center;
  padding: 2rem;
}

.user-details {
  background: #f5f5f5;
  padding: 1rem;
  border-radius: 4px;
  margin: 1rem 0;
}

.user-details p {
  margin: 0.5rem 0;
}

.actions {
  margin: 1rem 0;
}

.actions button {
  background: #ff4444;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
}

.actions button:hover {
  background: #cc3333;
}

.role-demo {
  margin-top: 2rem;
  padding: 1rem;
  border: 1px solid #eee;
  border-radius: 4px;
}

.not-authenticated {
  text-align: center;
  color: #666;
}
</style>
