import { computed, ref } from 'vue';

import { JiraWebhookService, type WebhookExecution } from '@/api/services/JiraWebhookService';
import { formatDate, parseDateOnlyBoundary } from '@/utils/dateUtils';

export interface WebhookHistoryFilters {
  status: string;
  dateFrom: string;
  dateTo: string;
}

export interface WebhookHistorySorting {
  field: string;
  order: 'asc' | 'desc';
}

// Filtering functions
function applyStatusFilter(executions: WebhookExecution[], status: string): WebhookExecution[] {
  return status ? executions.filter(exec => exec.status === status) : executions;
}

function applyDateFromFilter(executions: WebhookExecution[], dateFrom: string): WebhookExecution[] {
  if (!dateFrom) return executions;
  const fromDate = parseDateOnlyBoundary(dateFrom, false);
  if (!fromDate) return executions;
  return executions.filter(exec => new Date(exec.triggeredAt) >= fromDate);
}

function applyDateToFilter(executions: WebhookExecution[], dateTo: string): WebhookExecution[] {
  if (!dateTo) return executions;
  const toDate = parseDateOnlyBoundary(dateTo, true);
  if (!toDate) return executions;
  return executions.filter(exec => new Date(exec.triggeredAt) <= toDate);
}

// Sorting functions
function compareValues(aValue: unknown, bValue: unknown, field: string): number {
  if (aValue === undefined && bValue === undefined) return 0;
  if (aValue === undefined) return 1;
  if (bValue === undefined) return -1;

  if (typeof aValue === 'string' && typeof bValue === 'string') {
    return compareDateOrString(aValue, bValue, field);
  } else if (typeof aValue === 'number' && typeof bValue === 'number') {
    return aValue - bValue;
  }

  return String(aValue).localeCompare(String(bValue));
}

function compareDateOrString(aValue: string, bValue: string, field: string): number {
  if (field === 'triggeredAt' || field === 'completedAt') {
    return new Date(aValue).getTime() - new Date(bValue).getTime();
  }
  return aValue.localeCompare(bValue);
}

function sortExecutions(
  executions: WebhookExecution[],
  field: string,
  order: 'asc' | 'desc'
): WebhookExecution[] {
  return [...executions].sort((a, b) => {
    const aValue = a[field as keyof WebhookExecution];
    const bValue = b[field as keyof WebhookExecution];

    const comparison = compareValues(aValue, bValue, field);
    return order === 'desc' ? -comparison : comparison;
  });
}

// Webhook history filters composable
export function useWebhookHistoryFilters() {
  const statusFilter = ref('');
  const dateFromFilter = ref('');
  const dateToFilter = ref('');

  const resetFilters = () => {
    statusFilter.value = '';
    dateFromFilter.value = '';
    dateToFilter.value = '';
  };

  return {
    statusFilter,
    dateFromFilter,
    dateToFilter,
    resetFilters,
  };
}

// Webhook history sorting composable
export function useWebhookHistorySorting() {
  const sortField = ref('triggeredAt');
  const sortOrder = ref<'asc' | 'desc'>('desc');

  const updateSort = (field: string, order: 'asc' | 'desc') => {
    sortField.value = field;
    sortOrder.value = order;
  };

  return {
    sortField,
    sortOrder,
    updateSort,
  };
}

// Webhook history pagination composable
export function useWebhookHistoryPagination() {
  const currentPage = ref(1);
  const recordsPerPage = ref(10);

  const resetPagination = () => {
    currentPage.value = 1;
  };

  return {
    currentPage,
    recordsPerPage,
    resetPagination,
  };
}

// Webhook history data fetching composable
export function useWebhookHistoryData(webhookId: string) {
  const loading = ref(false);
  const executionHistory = ref<WebhookExecution[]>([]);

  const fetchHistory = async () => {
    if (!webhookId) return;

    try {
      loading.value = true;
      const response = await JiraWebhookService.getWebhookExecutions(webhookId);
      executionHistory.value = response || [];
    } catch (error) {
      console.error('Failed to fetch webhook history:', error);
      executionHistory.value = [];
    } finally {
      loading.value = false;
    }
  };

  const refreshHistory = () => {
    return fetchHistory();
  };

  return {
    loading,
    executionHistory,
    fetchHistory,
    refreshHistory,
  };
}

export function useWebhookHistory(webhookId: string) {
  const data = useWebhookHistoryData(webhookId);
  const filters = useWebhookHistoryFilters();
  const sorting = useWebhookHistorySorting();
  const pagination = useWebhookHistoryPagination();

  const filteredHistory = computed(() => {
    let filtered = data.executionHistory.value;

    filtered = applyStatusFilter(filtered, filters.statusFilter.value);
    filtered = applyDateFromFilter(filtered, filters.dateFromFilter.value);
    filtered = applyDateToFilter(filtered, filters.dateToFilter.value);

    return sortExecutions(filtered, sorting.sortField.value, sorting.sortOrder.value);
  });

  const paginatedHistory = computed(() => {
    const startIndex = (pagination.currentPage.value - 1) * pagination.recordsPerPage.value;
    const endIndex = startIndex + pagination.recordsPerPage.value;
    return filteredHistory.value.slice(startIndex, endIndex);
  });

  const totalPages = computed(() => {
    return Math.ceil(filteredHistory.value.length / pagination.recordsPerPage.value);
  });

  const totalRecords = computed(() => {
    return filteredHistory.value.length;
  });

  const resetAll = () => {
    filters.resetFilters();
    pagination.resetPagination();
  };

  const formatExecutionDate = (dateString: string) => {
    return formatDate(dateString, { includeTime: true, includeSeconds: true });
  };

  return {
    ...data,
    ...filters,
    ...sorting,
    ...pagination,
    filteredHistory,
    paginatedHistory,
    totalPages,
    totalRecords,
    resetAll,
    formatExecutionDate,
  };
}
