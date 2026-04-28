import type { NavigationGuardNext, RouteLocationNormalized } from 'vue-router';

import { useAuthStore } from '@/store/auth';

/**
 * Authentication guard for Vue Router
 * Ensures users are authenticated before accessing protected routes
 */
export async function authGuard(
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  next: NavigationGuardNext
) {
  const authStore = useAuthStore();

  try {
    // Initialize authentication if not already done
    if (!authStore.initialized) {
      await authStore.init();
    }

    // Check if user is authenticated
    if (authStore.isAuthenticated) {
      next();
    } else {
      // Redirect to login if not authenticated
      next('/login');
    }
  } catch {
    next('/login');
  }
}

/**
 * Role-based authorization guard
 * Ensures users have required roles to access specific routes
 */
export function roleGuard(requiredRoles: string[]) {
  return async (
    to: RouteLocationNormalized,
    from: RouteLocationNormalized,
    next: NavigationGuardNext
  ) => {
    const authStore = useAuthStore();

    try {
      // Ensure authentication first
      if (!authStore.initialized) {
        await authStore.init();
      }

      if (!authStore.isAuthenticated) {
        next('/login');
        return;
      }

      // Check role authorization
      if (authStore.hasAnyRole(requiredRoles)) {
        next();
      } else {
        next('/unauthorized');
      }
    } catch {
      next('/login');
    }
  };
}

/**
 * All-roles authorization guard
 * Ensures users have ALL required roles to access specific routes
 */
export function allRolesGuard(requiredRoles: string[]) {
  return async (
    to: RouteLocationNormalized,
    from: RouteLocationNormalized,
    next: NavigationGuardNext
  ) => {
    const authStore = useAuthStore();

    try {
      // Ensure authentication first
      if (!authStore.initialized) {
        await authStore.init();
      }

      if (!authStore.isAuthenticated) {
        next('/login');
        return;
      }

      // Check that the user holds every required role
      if (authStore.hasAllRoles(requiredRoles)) {
        next();
      } else {
        next('/unauthorized');
      }
    } catch {
      next('/login');
    }
  };
}
