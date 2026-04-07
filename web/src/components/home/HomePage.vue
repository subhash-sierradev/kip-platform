<template>
  <div class="home-page">
    <div class="container">
      <h1>Welcome to Kaseware Integration Platform</h1>
      <p>
        Streamline your data integration workflows with our powerful, enterprise-grade platform.
      </p>

      <div class="features">
        <div class="feature">
          <h3>Real-time Integration</h3>
          <p>Connect your systems seamlessly with real-time data synchronization.</p>
        </div>
        <div class="feature">
          <h3>Monitoring & Analytics</h3>
          <p>Track integration performance and monitor data flows.</p>
        </div>
        <div class="feature">
          <h3>Enterprise Security</h3>
          <p>Multi-tenant architecture with OAuth2/JWT authentication.</p>
        </div>
        <div class="feature">
          <h3>Webhook Management</h3>
          <p>Configure and manage Jira webhooks with field mapping.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// Simple minimal home page
</script>

<script lang="ts">
export default {
  name: 'HomePage',
};
</script>

<style scoped>
.home-page {
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
  .home-page {
    padding: 1rem;
  }

  .features {
    grid-template-columns: 1fr;
  }
}
</style>
