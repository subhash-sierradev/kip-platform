/**
 * Centralized shared route path constants for the application.
 *
 * Import from here instead of hardcoding shared path strings elsewhere —
 * navigation utilities, sidebar links, notifications, and tests all share
 * these constants so a path rename only ever touches this file.
 *
 * Router-only aliases and fallback routes may still be defined alongside the
 * Vue Router configuration when they are not consumed outside the router.
 */

// Base paths shared between list and detail routes — kept private so
// consumers always go through ROUTES rather than the raw strings.
const JIRA_WEBHOOK_BASE = '/outbound/webhook/jira';
const ARCGIS_INTEGRATION_BASE = '/outbound/integration/arcgis';
const CONFLUENCE_INTEGRATION_BASE = '/outbound/integration/confluence';

export const ROUTES = {
  // Root
  home: '/',

  // Inbound
  inboundIntegrations: '/inbound/integrations',

  // Outbound — Jira Webhook
  jiraWebhook: JIRA_WEBHOOK_BASE,
  jiraWebhookDetails: (id: string) => `${JIRA_WEBHOOK_BASE}/${id}`,
  jiraWebhookWizard: '/outbound/jira-webhook/wizard',

  // Outbound — ArcGIS Integration
  arcgisIntegration: ARCGIS_INTEGRATION_BASE,
  arcgisIntegrationDetails: (id: string) => `${ARCGIS_INTEGRATION_BASE}/${id}`,

  // Outbound — Confluence Integration
  confluenceIntegration: CONFLUENCE_INTEGRATION_BASE,
  confluenceIntegrationDetails: (id: string) => `${CONFLUENCE_INTEGRATION_BASE}/${id}`,

  // Admin
  adminAuditLog: '/admin/audit-log',
  adminClearCache: '/admin/clear-cache',
  adminCacheStatistics: '/admin/cache-statistics',
  adminSiteConfig: '/admin/site-config',
  adminNotifications: '/admin/notifications',

  // Admin — Connections
  arcgisConnection: '/admin/connections/arcgis',
  jiraConnection: '/admin/connections/jira',
  confluenceConnection: '/admin/connections/confluence',

  // Notifications
  allNotifications: '/notifications',

  // Misc
  unauthorized: '/unauthorized',
} as const;
