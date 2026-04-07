<template>
  <div class="panel">
    <h3>Jira Connections</h3>
    <LoadingSpinner v-if="loading" />
    <ErrorPanel v-else-if="error" :message="error" />
    <table v-else class="dx-table">
      <thead>
        <tr>
          <th>Connection Key</th>
          <th>Base URL</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="c in connections" :key="c.id">
          <td>{{ (c as any).secretName || c.name }}</td>
          <td>{{ c.jiraBaseUrl }}</td>
          <td>
            <StatusBadge :status="c.lastConnectionStatus" />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue';
import { useJiraConnections } from '@/composables/useJiraConnections';
import StatusBadge from '@/components/common/StatusBadge.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import ErrorPanel from '@/components/common/ErrorPanel.vue';

const props = defineProps<{ autofetch?: boolean }>();
const { data, loading, error, fetch } = useJiraConnections();

onMounted(() => {
  if (props.autofetch !== false) {
    void fetch({});
  }
});

const connections = computed(() => data.value ?? []);
</script>

<style scoped>
.dx-table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 0.5rem 0.65rem;
  font-size: 0.85rem;
  border-bottom: 1px solid #eceef0;
}

thead th {
  background: #f7f8fa;
  font-weight: 600;
}
</style>
