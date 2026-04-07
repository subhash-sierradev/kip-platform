<template>
  <div class="integrations-page">
    <div class="container">
      <h1>Integrations</h1>
      <p>Configure and manage your data integration workflows.</p>

      <div class="features">
        <div class="feature">
          <h3>Data Connections</h3>
          <p>Set up connections between different data sources and destinations.</p>
        </div>
        <div class="feature">
          <h3>Workflow Automation</h3>
          <p>Automate data flows with scheduled and trigger-based processing.</p>
        </div>
        <div class="feature">
          <h3>Real-time Monitoring</h3>
          <p>Track integration performance and monitor data synchronization.</p>
        </div>
        <div class="feature">
          <h3>Error Handling</h3>
          <p>Comprehensive error tracking and retry mechanisms for failed operations.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// Simple minimal integrations page
</script>

<script lang="ts">
export default {
  name: 'IntegrationsPage',
};
</script>

<style scoped>
.integrations-page {
  padding: 2rem;
  max-width: 800px;
  margin: 0 auto;
}

.container {
  text-align: center;
}

h1 {
  font-size: 2rem;
  margin-bottom: 1rem;
  color: #333;
}

p {
  font-size: 1rem;
  color: #666;
  margin-bottom: 2rem;
  line-height: 1.5;
}

.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  text-align: left;
}

.feature {
  padding: 1rem;
  border: 1px solid #eee;
  border-radius: 4px;
}

.feature h3 {
  font-size: 1.1rem;
  margin-bottom: 0.5rem;
  color: #333;
}

.feature p {
  font-size: 0.9rem;
  margin: 0;
  color: #666;
}

@media (max-width: 768px) {
  .integrations-page {
    padding: 1rem;
  }

  .features {
    grid-template-columns: 1fr;
  }
}
</style>
