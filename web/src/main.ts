// Import DevExtreme theme CSS to enable built-in icons
import 'devextreme/dist/css/dx.light.css';
import './theme/index.css';

import { computed, createApp } from 'vue';

import App from './App.vue';
import { useAutoLogout } from './composables/useAutoLogout';
import { useConfigureOpenAPI } from './composables/useConfigureOpenAPI';
import { activateDevExtremeLicense } from './config/devextremeLicense';
import router from './router/index';
import { configureStore, useAuthStore } from './store/index';
import { setupGlobalLoader } from './utils/setupGlobalLoader';

// DevExtreme license activation from environment (synchronous)
activateDevExtremeLicense();

async function bootstrap() {
  const app = createApp(App);

  // Configure Pinia store first
  const pinia = configureStore();
  app.use(pinia);

  // Initialize loading interceptors AFTER Pinia is configured
  const globalLoaderCleanup = setupGlobalLoader();

  // Expose cleanup for development/HMR
  if (import.meta.hot) {
    import.meta.hot.dispose(() => {
      globalLoaderCleanup.cleanup();
    });
  }

  // Initialize authentication before anything else
  try {
    const authStore = useAuthStore();
    await authStore.init();

    // Configure OpenAPI client with reactive token management
    const tokenRef = computed(() => authStore.token || undefined);
    useConfigureOpenAPI(tokenRef);

    // Initialize auto-logout functionality after authentication is ready
    useAutoLogout();
  } catch {
    // Show error message to user
    document.body.innerHTML = `
      <div style="
        display: flex; 
        justify-content: center; 
        align-items: center; 
        height: 100vh; 
        font-family: Arial, sans-serif;
        background: #f8f9fa;
        color: #dc3545;
        text-align: center;
      ">
        <div>
          <h2>Authentication Error</h2>
          <p>Please check your connection and try refreshing the page.</p>
          <button onclick="window.location.reload()" style="
            background: #007bff;
            color: white;
            padding: 10px 20px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
          ">Retry</button>
        </div>
      </div>
    `;
    return;
  }

  // Configure router with authentication guard
  router.beforeEach(async (to, from, next) => {
    const authStore = useAuthStore();

    // Ensure authentication is initialized
    if (!authStore.initialized) {
      try {
        await authStore.init();
      } catch {
        return;
      }
    }

    // Check if route requires authentication (all routes in this app require auth)
    if (!authStore.isAuthenticated) {
      return; // Keycloak will handle the redirect
    }

    // OpenAPI client now handles token automatically via getToken()
    next();
  });

  // Configure router
  app.use(router);

  // OpenAPI client is pre-configured with Keycloak authentication
  // Auto-logout is already initialized after authentication is ready
  // Mount the appw
  app.mount('#app');
}

void bootstrap();
