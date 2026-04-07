<template>
  <div id="app">
    <div v-if="authStore.loading" class="auth-loading">
      <div class="auth-spinner">
        <div class="spinner"></div>
        <h3>Initializing Authentication...</h3>
        <p>Please wait while we connect to the authentication service.</p>
      </div>
    </div>

    <AppShell v-else-if="authStore.isAuthenticated">
      <RouterView />
    </AppShell>

    <ToastContainer />
    <NotificationToastContainer />
    <SimpleLoader />
  </div>
</template>

<script setup lang="ts">
import AppShell from './components/layout/AppShell.vue';
import ToastContainer from './components/common/ToastContainer.vue';
import NotificationToastContainer from './components/common/NotificationToastContainer.vue';
import SimpleLoader from './components/common/SimpleLoader.vue';
import { useAuthStore } from './store/auth';

const authStore = useAuthStore();
</script>

<style>
html,
body,
#app {
  height: 100%;
}
body {
  margin: 0;
  font-family: Inter, system-ui, sans-serif;
  background: #f4f5f7;
}

.auth-loading,
.auth-required {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  color: #2c3e50;
  text-align: center;
}

.auth-spinner,
.auth-content {
  max-width: 400px;
  padding: 3rem 2rem;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  border: 1px solid #e9ecef;
}

.auth-logo {
  margin-bottom: 2rem;
}

.auth-logo h1 {
  margin: 0;
  font-size: 2rem;
  font-weight: 700;
  color: var(--kw-primary-dark, #2c3e50);
  letter-spacing: -0.5px;
}

.platform-subtitle {
  display: block;
  font-size: 0.875rem;
  font-weight: 400;
  color: var(--kw-text-muted, #6c757d);
  margin-top: 0.25rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.spinner {
  border: 3px solid #e9ecef;
  border-top: 3px solid var(--kw-primary-dark, #2c3e50);
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin: 0 auto 1.5rem;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.auth-spinner h3,
.auth-content h3 {
  margin: 0 0 1rem 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--kw-primary-dark, #2c3e50);
}

.auth-spinner p,
.auth-content p {
  margin: 0 0 1rem 0;
  color: #6c757d;
  line-height: 1.5;
}

.auth-note {
  margin-top: 2rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 3px solid var(--kw-accent-orange, #e67e22);
}

.note-text {
  margin: 0;
  font-size: 0.875rem;
  color: #6c757d;
  font-style: italic;
}
</style>
