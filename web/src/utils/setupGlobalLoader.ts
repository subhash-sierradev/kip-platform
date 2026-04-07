import axios, { type AxiosRequestConfig } from 'axios';

import { useGlobalLoading } from '../composables/useGlobalLoading';

// Extend Axios config to support custom loading messages
declare module 'axios' {
  export interface AxiosRequestConfig {
    loadingMessage?: string;
    skipGlobalLoader?: boolean;
  }
}

/**
 * Interface for managing global loader interceptors
 */
export interface GlobalLoaderCleanup {
  /** Cleanup function to remove interceptors */
  cleanup: () => void;
  /** Whether the loader is currently active */
  isSetup: boolean;
}

// Track interceptor IDs to prevent multiple registrations
let requestInterceptorId: number | null = null;
let responseInterceptorId: number | null = null;
let isLoaderSetup = false;

/**
 * Clean up resource name for better readability
 */
function cleanResourceName(resource: string): string {
  return resource.replace(/-/g, ' ').replace(/_/g, ' ').toLowerCase();
}

/**
 * Check if segment is likely an ID (numeric or UUID pattern)
 * Returns true if segment is 8+ characters of digits/letters/hyphens/underscores
 * but excludes strings that are only lowercase letters and hyphens
 */
function isIdSegment(segment: string): boolean {
  return /^[\d\w-]{8,}$/.test(segment) && !/^[a-z-]+$/.test(segment);
}

/**
 * Extract resource name from URL segments
 */
function extractResource(urlSegments: string[]): string {
  const lastSegment = urlSegments[urlSegments.length - 1] || 'data';
  const secondLastSegment = urlSegments[urlSegments.length - 2];

  const isId = isIdSegment(lastSegment);
  const resource = isId ? secondLastSegment || 'item' : lastSegment;

  return cleanResourceName(resource);
}

/**
 * Check for special endpoint patterns
 */
function getSpecialEndpointMessage(url: string): string | null {
  const specialPatterns = {
    '/test-connection': 'Testing connection...',
    '/validate': 'Validating...',
    '/search': 'Searching...',
    '/export': 'Exporting...',
    '/import': 'Importing...',
    '/sync': 'Synchronizing...',
  };

  for (const [pattern, message] of Object.entries(specialPatterns)) {
    if (url.includes(pattern)) return message;
  }

  return null;
}

/**
 * Generate action-based message for HTTP method
 */
function getActionMessage(method: string, resource: string, url: string, isId: boolean): string {
  // Check special endpoints first
  const specialMessage = getSpecialEndpointMessage(url);
  if (specialMessage) return specialMessage;

  const methodUpper = method.toUpperCase();
  switch (methodUpper) {
    case 'POST':
      return isId ? `Updating ${resource}...` : `Creating ${resource}...`;
    case 'PUT':
    case 'PATCH':
      return `Updating ${resource}...`;
    case 'DELETE':
      return `Deleting ${resource}...`;
    case 'GET':
    default:
      return `Loading ${resource}...`;
  }
}

/**
 * Generate intelligent loading message based on HTTP method and URL pattern
 */
function generateLoadingMessage(method: string = 'get', url: string = ''): string {
  const urlSegments = url.split('/').filter(segment => segment && segment !== 'api');
  const resource = extractResource(urlSegments);
  const lastSegment = urlSegments[urlSegments.length - 1] || '';
  const isId = isIdSegment(lastSegment);

  return getActionMessage(method, resource, url, isId);
}

/**
 * Simple axios interceptor setup - replaces complex intercepted axios system
 * Automatically shows/hides loader during API requests with intelligent message generation
 *
 * @returns GlobalLoaderCleanup interface with cleanup function and setup status
 *
 * @example
 * const loaderCleanup = setupGlobalLoader();
 * // Later, when you need to clean up (e.g., in tests or during hot reload)
 * loaderCleanup.cleanup();
 */
export function setupGlobalLoader(): GlobalLoaderCleanup {
  // Prevent multiple registrations
  if (isLoaderSetup) {
    console.warn(
      'setupGlobalLoader: Interceptors already registered. Use cleanup() first if you need to re-register.'
    );
    return {
      cleanup: cleanupGlobalLoader,
      isSetup: true,
    };
  }

  const { setLoading } = useGlobalLoading();

  // Request interceptor - show loader with intelligent message
  requestInterceptorId = axios.interceptors.request.use(
    config => {
      // Skip loader if explicitly requested
      if (config.skipGlobalLoader) {
        return config;
      }

      // Use custom message if provided, otherwise generate intelligent message
      const message = config.loadingMessage || generateLoadingMessage(config.method, config.url);

      setLoading(true, message);
      return config;
    },
    error => {
      setLoading(false);
      return Promise.reject(error);
    }
  );

  // Response interceptor - hide loader
  responseInterceptorId = axios.interceptors.response.use(
    response => {
      setLoading(false);
      return response;
    },
    error => {
      setLoading(false);
      return Promise.reject(error);
    }
  );

  isLoaderSetup = true;

  return {
    cleanup: cleanupGlobalLoader,
    isSetup: true,
  };
}

/**
 * Cleans up global loader interceptors
 * Removes all registered interceptors and resets state
 */
export function cleanupGlobalLoader(): void {
  if (requestInterceptorId !== null) {
    axios.interceptors.request.eject(requestInterceptorId);
    requestInterceptorId = null;
  }

  if (responseInterceptorId !== null) {
    axios.interceptors.response.eject(responseInterceptorId);
    responseInterceptorId = null;
  }

  isLoaderSetup = false;

  // Reset loading state to ensure clean state
  const { resetLoading } = useGlobalLoading();
  resetLoading();
}

/**
 * Manual loading control for specific operations
 */
export async function withLoading<T>(
  operation: () => Promise<T>,
  message = 'Processing...'
): Promise<T> {
  const { setLoading } = useGlobalLoading();

  setLoading(true, message);
  try {
    const result = await operation();
    return result;
  } finally {
    setLoading(false);
  }
}

/**
 * Utility to create axios config with custom loading message
 * @param config - Base axios config
 * @param loadingMessage - Custom loading message to display
 * @returns Enhanced config with loading message
 *
 * @example
 * // Custom loading message
 * const config = withLoadingMessage({ method: 'post' }, 'Synchronizing data...');
 *
 * // Skip global loader entirely
 * const config = withLoadingMessage({ method: 'get' }, null);
 */
export function withLoadingMessage(
  config: AxiosRequestConfig = {},
  loadingMessage: string | null
): AxiosRequestConfig {
  return {
    ...config,
    ...(loadingMessage === null ? { skipGlobalLoader: true } : { loadingMessage }),
  };
}
