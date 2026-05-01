import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';

import { allRolesGuard, roleGuard } from './guards';
import { ROUTES } from './routes';

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
    path: ROUTES.home,
    component: () => import(/* webpackChunkName: "home" */ '@/components/home/HomePage.vue'),
    alias: '/home',
  },

  // Inbound routes
  {
    path: ROUTES.inboundIntegrations,
    component: IntegrationPage,
    beforeEnter: roleGuard(['feature_integration']),
  },

  {
    path: ROUTES.jiraWebhook,
    component: JiraWebhookPage,
    meta: { disableShellPadding: true },
    beforeEnter: roleGuard(['feature_jira_webhook']),
  },
  {
    path: `${ROUTES.jiraWebhook}/:id`,
    component: JiraWebhookDetailsPage,
    beforeEnter: roleGuard(['feature_jira_webhook']),
  },
  {
    path: ROUTES.arcgisIntegration,
    component: ArcGISIntegrationPage,
    meta: { disableShellPadding: true },
    beforeEnter: roleGuard(['feature_arcgis_integration']),
  },
  {
    path: `${ROUTES.arcgisIntegration}/:id`,
    component: () =>
      import('@/components/outbound/arcgisintegration/details/ArcGISIntegrationDetailsPage.vue'),
    beforeEnter: roleGuard(['feature_arcgis_integration']),
  },
  {
    path: ROUTES.jiraWebhookWizard,
    name: 'jira-webhook-wizard',
    component: () => import('@/components/outbound/jirawebhooks/wizard/JiraWebhookWizard.vue'),
    beforeEnter: roleGuard(['feature_jira_webhook']),
  },
  {
    path: ROUTES.confluenceIntegration,
    component: ConfluenceIntegrationPage,
    meta: { disableShellPadding: true },
    beforeEnter: roleGuard(['feature_confluence_integration']),
  },
  {
    path: `${ROUTES.confluenceIntegration}/:id`,
    component: () =>
      import('@/components/outbound/confluenceintegration/details/ConfluenceIntegrationDetailsPage.vue'),
    beforeEnter: roleGuard(['feature_confluence_integration']),
  },

  // Admin routes
  { path: ROUTES.adminAuditLog, component: AuditLogPage, beforeEnter: roleGuard(['tenant_admin']) },
  {
    path: ROUTES.adminClearCache,
    component: ClearCachePage,
    beforeEnter: roleGuard(['app_admin']),
  },
  {
    path: ROUTES.adminCacheStatistics,
    component: CacheStatisticsPage,
    beforeEnter: roleGuard(['app_admin']),
  },
  {
    path: ROUTES.adminSiteConfig,
    component: SiteConfigPage,
    beforeEnter: roleGuard(['tenant_admin', 'app_admin']),
  },
  {
    path: ROUTES.jiraConnection,
    component: JiraConnectionPage,
    beforeEnter: roleGuard(['tenant_admin']),
  },
  {
    path: ROUTES.arcgisConnection,
    component: ArcGISConnectionPage,
    beforeEnter: roleGuard(['tenant_admin']),
  },
  {
    path: ROUTES.confluenceConnection,
    component: ConfluenceConnectionPage,
    beforeEnter: roleGuard(['tenant_admin']),
  },
  {
    path: ROUTES.adminNotifications,
    component: NotificationsPage,
    beforeEnter: roleGuard(['tenant_admin', 'app_admin']),
  },
  { path: ROUTES.allNotifications, component: NotificationsAllPage },
  { path: ROUTES.unauthorized, component: UnauthorizedPage },
  // TODO KIP-547 REMOVE — Temporary ArcGIS feature service verification
  {
    path: ROUTES.arcgisVerification,
    component: () =>
      import(
        /* webpackChunkName: "admin" */ '@/components/admin/arcgisverification/ArcGISVerificationPage.vue'
      ),
    beforeEnter: allRolesGuard(['tenant_admin', 'feature_arcgis_integration']),
  },

  // Catch all route
  { path: '/:pathMatch(.*)*', redirect: ROUTES.home },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.VITE_BASE_URL || '/'),
  routes,
});

export default router;
