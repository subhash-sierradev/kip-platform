import { computed, onMounted, ref, watch } from 'vue';

import { type AuditLogResponse, EntityType, SettingsService } from '@/api';
import { useAuthStore } from '@/store/auth';

// Helper function for fetching logs
const createLogFetcher = (authStore: ReturnType<typeof useAuthStore>) => {
  return async (
    auditLogs: { value: AuditLogResponse[] },
    loading: { value: boolean },
    error: { value: string | null }
  ) => {
    if (!authStore.isAuthenticated) {
      return;
    }

    loading.value = true;
    error.value = null;

    try {
      const isAppAdmin = authStore.hasRole('app_admin');
      let logs: AuditLogResponse[];

      if (isAppAdmin) {
        logs = await SettingsService.listAuditLogs();
      } else {
        logs = await SettingsService.listAuditLogs();
      }

      auditLogs.value = logs;
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to fetch audit logs';
      error.value = errorMessage;
    } finally {
      loading.value = false;
    }
  };
};

// Helper function for fetching logs by tenant
const createTenantLogFetcher = (authStore: ReturnType<typeof useAuthStore>) => {
  return async (
    tenantId: string,
    auditLogs: { value: AuditLogResponse[] },
    loading: { value: boolean },
    error: { value: string | null }
  ) => {
    if (!authStore.isAuthenticated || !authStore.hasRole('app_admin')) {
      return;
    }

    loading.value = true;
    error.value = null;

    try {
      const logs = await SettingsService.listAuditLogsByTenant({ tenantId });
      auditLogs.value = logs;
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to fetch audit logs';
      error.value = errorMessage;
    } finally {
      loading.value = false;
    }
  };
};

// Helper function to extract webhook name from details string
function extractWebhookName(details: string): string | null {
  if (typeof details === 'string' && details.includes('webhookName')) {
    const match = details.match(/webhookName[=:]([^,}]+)/);
    return match ? match[1].trim() : null;
  }
  return null;
}

// Helper function to extract clean display name
function getDisplayName(log: AuditLogResponse): string {
  // For JIRA_WEBHOOK_EVENT, always extract webhookName from details/metadata
  if (log?.entityType === 'JIRA_WEBHOOK_EVENT') {
    const webhookName = extractWebhookName(log.details || '');
    if (webhookName) {
      return webhookName;
    }
    // fallback to entityId if webhookName not found
    return log.entityId || '-';
  }
  // For other types, use details if present
  if (log?.details) {
    return log.details;
  }
  return `${log?.entityType || '-'} [${log?.entityId || ''}]`;
}

// Helper function to setup filters and computed values
const setupFilters = (auditLogs: { value: AuditLogResponse[] }) => {
  const selectedEntityType = ref<EntityType | null>(null);
  const selectedUserId = ref<string | null>(null);
  const selectedActivity = ref<string | null>(null);

  const entityTypes = computed(() => [
    { value: EntityType.JIRA_WEBHOOK, label: 'JIRA_WEBHOOK' },
    { value: EntityType.INTEGRATION, label: 'INTEGRATION' },
    { value: EntityType.JIRA_WEBHOOK_EVENT, label: 'JIRA_WEBHOOK_EVENT' },
    { value: EntityType.SITE_CONFIG, label: 'SITE_CONFIG' },
    { value: EntityType.CACHE, label: 'CACHE' },
  ]);

  const userIds = computed(() => {
    const uniqueUsers = [...new Set(auditLogs.value.map(log => log.performedBy).filter(Boolean))];
    return uniqueUsers.map(user => ({ value: user!, label: user! }));
  });

  const activities = computed(() => {
    // Provide a complete static list of all possible audit activities
    // This ensures all filter options are always available regardless of current data
    const allPossibleActivities = [
      'CREATE',
      'UPDATE',
      'UPSERT',
      'DELETE',
      'ENABLED',
      'DISABLED',
      'RETRY',
      'TEST',
      'EXECUTE',
      'LOGIN',
      'LOGOUT',
      'CLEAR_CACHE',
    ];

    // Filter to only show activities that actually exist in the current dataset
    const existingActivities = [...new Set(auditLogs.value.map(log => log.action).filter(Boolean))];

    return allPossibleActivities
      .filter(activity => existingActivities.includes(activity))
      .map(activity => ({ value: activity, label: activity }));
  });

  // Helper to safely convert timestamp to ISO string
  function toIsoTimestamp(ts: string | number | Date | undefined): string {
    if (!ts) return '';
    const d = new Date(ts);
    return isNaN(d.getTime()) ? '' : d.toISOString();
  }

  const filteredLogs = computed(() => {
    let filtered = [...auditLogs.value];

    if (selectedEntityType.value) {
      filtered = filtered.filter(log => log.entityType === selectedEntityType.value);
    }

    if (selectedUserId.value) {
      filtered = filtered.filter(log => log.performedBy === selectedUserId.value);
    }

    if (selectedActivity.value) {
      filtered = filtered.filter(log => log.action === selectedActivity.value);
    }

    // Ensure timestamp is always an ISO string for DevExtreme grouping/filtering
    // Filter out records with missing or invalid timestamp
    return filtered
      .filter(log => {
        if (!log.timestamp) return false;
        return toIsoTimestamp(log.timestamp) !== '';
      })
      .map(log => ({
        ...log,
        timestamp: toIsoTimestamp(log.timestamp),
        displayName: getDisplayName(log),
      }));
  });

  const clearFilters = () => {
    selectedEntityType.value = null;
    selectedUserId.value = null;
    selectedActivity.value = null;
  };

  return {
    selectedEntityType,
    selectedUserId,
    selectedActivity,
    entityTypes,
    userIds,
    activities,
    filteredLogs,
    clearFilters,
  };
};

// Helper function to setup authentication watcher
const setupAuthWatcher = (authStore: ReturnType<typeof useAuthStore>, fetchLogs: () => void) => {
  if (authStore.isAuthenticated) {
    fetchLogs();
  } else {
    const stopWatcher = watch(
      () => authStore.isAuthenticated,
      (isAuth: boolean) => {
        if (isAuth) {
          fetchLogs();
          stopWatcher();
        }
      }
    );
  }
};

export function useAuditLogs() {
  const authStore = useAuthStore();

  const auditLogs = ref<AuditLogResponse[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const filters = setupFilters(auditLogs);

  const fetchAuditLogs = createLogFetcher(authStore);
  const fetchAuditLogsByTenant = createTenantLogFetcher(authStore);

  const fetchLogs = () => fetchAuditLogs(auditLogs, loading, error);
  const fetchLogsByTenant = (tenantId: string) =>
    fetchAuditLogsByTenant(tenantId, auditLogs, loading, error);

  const exportLogs = () => {
    // TODO: Implement CSV export functionality
  };

  const formatEntityType = (entityType?: EntityType): string => entityType || '';
  const formatActivity = (activity?: string): string => activity || '';

  onMounted(() => {
    setupAuthWatcher(authStore, fetchLogs);
  });

  return {
    auditLogs: computed(() => auditLogs.value),
    loading: computed(() => loading.value),
    error: computed(() => error.value),
    ...filters,
    fetchAuditLogs: fetchLogs,
    fetchAuditLogsByTenant: fetchLogsByTenant,
    exportLogs,
    formatEntityType,
    formatActivity,
  };
}
