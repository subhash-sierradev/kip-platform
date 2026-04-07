import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';

import { roleGuard } from './guards';

// Centralized lazy-loaded components with chunk names for consistency
const IntegrationPage = () =>
  import(/* webpackChunkName: "inbound" */ '@/components/inbound/IntegrationsPage.vue');
const AuditLogPage = () =>
  import(/* webpackChunkName: "admin" */ '@/components/admin/audit/AuditLogPage.vue');
const ClearCachePage = () =>
  import(/* webpackChunkName: "admin" */ '@/components/admin/cache/ClearCachePage.vue');
const SiteConfigPage = () =>
  import(/* webpackChunkName: "admin" */ '@/components/admin/siteconfig/SiteConfigPage.vue');
const JiraConnectionPage = () =>
  import(
    /* webpackChunkName: "admin" */ '@/components/admin/jiraconnections/JiraConnectionPage.vue'
  );
const ArcGISConnectionPage = () =>
  import(
    /* webpackChunkName: "admin" */ '@/components/admin/arcgisconnections/ArcGISConnectionPage.vue'
  );
const NotificationsPage = () =>
  import(/* webpackChunkName: "admin" */ '@/components/admin/notifications/NotificationsPage.vue');
const NotificationsAllPage = () =>
  import(
    /* webpackChunkName: "notifications" */ '@/components/notifications/NotificationsAllPage.vue'
  );
const JiraWebhookDetailsPage = () =>
  import(
    /* webpackChunkName: "jira-webhooks" */ '@/components/outbound/jirawebhooks/details/JiraWebhookDetailsPage.vue'
  );
const CacheStatisticsPage = () =>
  import(/* webpackChunkName: "admin" */ '@/components/admin/cache/CacheStatisticsPage.vue');
const UnauthorizedPage = () =>
  import(/* webpackChunkName: "common" */ '@/components/common/UnauthorizedPage.vue');
const JiraWebhookPage = () =>
  import(
    /* webpackChunkName: "jira-webhooks" */ '@/components/outbound/jirawebhooks/JiraWebhookDashboard.vue'
  );
const ArcGISIntegrationPage = () =>
  import(
    /* webpackChunkName: "outbound" */ '@/components/outbound/arcgisintegration/ArcGISIntegrationPage.vue'
  );
const ConfluenceIntegrationPage = () =>
  import(
    /* webpackChunkName: "confluence" */ '@/components/outbound/confluenceintegration/ConfluenceIntegrationPage.vue'
  );
const ConfluenceConnectionPage = () =>
  import(
    /* webpackChunkName: "admin" */ '@/components/admin/confluenceconnections/ConfluenceConnectionPage.vue'
  );

const routes: RouteRecordRaw[] = [
  // Set HomePage as the startup (root) component; provide '/home' as an alias for backward compatibility
  {
    path: '/',
    component: () => import(/* webpackChunkName: "home" */ '@/components/home/HomePage.vue'),
    alias: '/home',
  },

  // Inbound routes
  {
    path: '/inbound/integrations',
    component: IntegrationPage,
    beforeEnter: roleGuard(['feature_integration']),
  },

  {
    path: '/outbound/webhook/jira',
    component: JiraWebhookPage,
    meta: { disableShellPadding: true },
    beforeEnter: roleGuard(['feature_jira_webhook']),
  },
  {
    path: '/outbound/webhook/jira/:id',
    component: JiraWebhookDetailsPage,
    beforeEnter: roleGuard(['feature_jira_webhook']),
  },
  {
    path: '/outbound/integration/arcgis',
    component: ArcGISIntegrationPage,
    meta: { disableShellPadding: true },
    beforeEnter: roleGuard(['feature_arcgis_integration']),
  },
  {
    path: '/outbound/integration/arcgis/:id',
    component: () =>
      import('@/components/outbound/arcgisintegration/details/ArcGISIntegrationDetailsPage.vue'),
    beforeEnter: roleGuard(['feature_arcgis_integration']),
  },
  {
    path: '/outbound/jira-webhook/wizard',
    name: 'jira-webhook-wizard',
    component: () => import('@/components/outbound/jirawebhooks/wizard/JiraWebhookWizard.vue'),
    beforeEnter: roleGuard(['feature_jira_webhook']),
  },
  {
    path: '/outbound/integration/confluence',
    component: ConfluenceIntegrationPage,
    meta: { disableShellPadding: true },
    beforeEnter: roleGuard(['feature_confluence_integration']),
  },
  {
    path: '/outbound/integration/confluence/:id',
    component: () =>
      import('@/components/outbound/confluenceintegration/details/ConfluenceIntegrationDetailsPage.vue'),
    beforeEnter: roleGuard(['feature_confluence_integration']),
  },

  // Admin routes
  { path: '/admin/audit-log', component: AuditLogPage, beforeEnter: roleGuard(['tenant_admin']) },
  { path: '/admin/clear-cache', component: ClearCachePage, beforeEnter: roleGuard(['app_admin']) },
  {
    path: '/admin/cache-statistics',
    component: CacheStatisticsPage,
    beforeEnter: roleGuard(['app_admin']),
  },
  {
    path: '/admin/site-config',
    component: SiteConfigPage,
    beforeEnter: roleGuard(['tenant_admin', 'app_admin']),
  },
  {
    path: '/admin/connections/jira',
    component: JiraConnectionPage,
    beforeEnter: roleGuard(['tenant_admin']),
  },
  {
    path: '/admin/connections/arcgis',
    component: ArcGISConnectionPage,
    beforeEnter: roleGuard(['tenant_admin']),
  },
  {
    path: '/admin/connections/confluence',
    component: ConfluenceConnectionPage,
    beforeEnter: roleGuard(['tenant_admin', 'feature_confluence_integration']),
  },
  {
    path: '/admin/notifications',
    component: NotificationsPage,
    beforeEnter: roleGuard(['tenant_admin', 'app_admin']),
  },
  { path: '/notifications', component: NotificationsAllPage },
  { path: '/unauthorized', component: UnauthorizedPage },

  // Catch all route
  { path: '/:pathMatch(.*)*', redirect: '/' },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.VITE_BASE_URL || '/'),
  routes,
});

export default router;
