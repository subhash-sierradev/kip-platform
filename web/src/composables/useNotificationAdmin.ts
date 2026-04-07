import { computed, type ComputedRef, onMounted, type Ref, ref, watch } from 'vue';

import type {
  NotificationEventCatalogResponse,
  NotificationSeverity,
  UserProfileResponse,
} from '@/api';
import { NotificationAdminService } from '@/api/services/NotificationAdminService';
import { UserService } from '@/api/services/UserService';
import { useAuthStore } from '@/store/auth';
import Alert from '@/utils/notificationUtils';

import { useActiveIntegrationTypes } from './useActiveIntegrationTypes';
import { useNotificationEvents } from './useNotificationEvents';
import { useNotificationPoliciesManager } from './useNotificationPoliciesManager';
import { useNotificationRulesManager } from './useNotificationRulesManager';
import { useNotificationTemplatesManager } from './useNotificationTemplatesManager';

interface EventKeyScopedItem {
  eventKey?: string | null;
}

interface RemainingRuleRow {
  event: NotificationEventCatalogResponse;
  severity: string;
}

function createEventKeyFilter<T extends EventKeyScopedItem>(
  items: Ref<T[]>,
  activeEventKeys: ComputedRef<Set<string>>
) {
  return computed(() =>
    items.value.filter(item => !item.eventKey || activeEventKeys.value.has(item.eventKey))
  );
}

function watchAuthentication(
  isAuthenticated: Ref<boolean> | ComputedRef<boolean>,
  fetchEvents: () => void | Promise<void>
): void {
  if (isAuthenticated.value) {
    fetchEvents();
    return;
  }

  watch(
    isAuthenticated,
    isAuth => {
      if (isAuth) {
        fetchEvents();
      }
    },
    { once: true }
  );
}

function buildBatchRuleRequest(rows: RemainingRuleRow[], recipientType: string, userIds: string[]) {
  return {
    rules: rows.map(row => ({
      eventId: row.event.id!,
      severity: row.severity as NotificationSeverity,
      isEnabled: true,
    })),
    recipientType,
    ...(recipientType === 'SELECTED_USERS' && userIds.length > 0 ? { userIds } : {}),
  };
}

export function useNotificationAdmin() {
  const authStore = useAuthStore();
  const { activeEntityTypes } = useActiveIntegrationTypes();
  const ev = useNotificationEvents();
  const rl = useNotificationRulesManager();
  const tl = useNotificationTemplatesManager();
  const pl = useNotificationPoliciesManager();

  // Events are already server-filtered by entity type (backend uses the same role logic).
  // Templates, rules and policies are filtered client-side by cross-referencing event keys.
  const activeEventKeys = computed(
    () => new Set(ev.events.value.map(e => e.eventKey).filter((k): k is string => !!k))
  );
  const filteredTemplates = createEventKeyFilter(tl.templates, activeEventKeys);
  const filteredRules = createEventKeyFilter(rl.rules, activeEventKeys);
  const filteredPolicies = createEventKeyFilter(pl.policies, activeEventKeys);

  const users = ref<UserProfileResponse[]>([]);
  const loadingUsers = ref(false);
  const bulkSaving = ref(false);
  const saving = computed(
    () =>
      ev.saving.value || rl.saving.value || tl.saving.value || pl.saving.value || bulkSaving.value
  );

  async function fetchUsers(): Promise<void> {
    loadingUsers.value = true;
    try {
      users.value = await UserService.getUsers();
    } catch {
      Alert.error('Failed to load users');
    } finally {
      loadingUsers.value = false;
    }
  }

  async function createAllRemainingRules(
    rows: RemainingRuleRow[],
    recipientType: string,
    userIds: string[] = []
  ): Promise<void> {
    bulkSaving.value = true;
    try {
      await NotificationAdminService.createRulesBatch({
        requestBody: buildBatchRuleRequest(rows, recipientType, userIds),
      });
      Alert.success(`${rows.length} rule${rows.length !== 1 ? 's' : ''} created successfully`);
    } catch {
      Alert.error('Failed to create notification rules');
    } finally {
      bulkSaving.value = false;
      await Promise.all([rl.fetchRules(), pl.fetchPolicies()]);
    }
  }

  onMounted(() => {
    watchAuthentication(
      computed(() => authStore.isAuthenticated),
      ev.fetchEvents
    );
  });

  return {
    events: ev.events,
    rules: rl.rules,
    templates: tl.templates,
    policies: pl.policies,
    filteredRules,
    filteredTemplates,
    filteredPolicies,
    activeEntityTypes,
    users,
    saving,
    loadingEvents: ev.loadingEvents,
    loadingRules: rl.loadingRules,
    loadingTemplates: tl.loadingTemplates,
    loadingPolicies: pl.loadingPolicies,
    loadingUsers,
    fetchEvents: ev.fetchEvents,
    fetchRules: rl.fetchRules,
    fetchTemplates: tl.fetchTemplates,
    fetchPolicies: pl.fetchPolicies,
    fetchUsers,
    createEvent: ev.createEvent,
    deactivateEvent: ev.deactivateEvent,
    createRule: rl.createRule,
    updateRule: rl.updateRule,
    deleteRule: rl.deleteRule,
    toggleRule: rl.toggleRule,
    createPolicy: pl.createPolicy,
    upsertPolicy: pl.upsertPolicy,
    deletePolicy: pl.deletePolicy,
    createAllRemainingRules,
  };
}
