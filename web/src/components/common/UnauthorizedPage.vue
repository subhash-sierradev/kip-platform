<template>
  <section class="unauthorized-page" aria-labelledby="unauthorized-title">
    <div class="card" role="alert">
      <div class="icon-circle" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path
            d="M17 9H7a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6a2 2 0 0 0-2-2Z"
            stroke="currentColor"
            stroke-width="1.5"
          />
          <path d="M8 9V7a4 4 0 1 1 8 0v2" stroke="currentColor" stroke-width="1.5" />
          <circle cx="12" cy="14" r="1.5" fill="currentColor" />
        </svg>
      </div>
      <h1 id="unauthorized-title" class="title">403 • Access Denied</h1>
      <p class="subtitle">
        You do not have access to this page. If you believe this is an error, contact your
        administrator.
      </p>

      <div class="actions">
        <button class="btn btn-primary" @click="goHome">Go to Dashboard</button>
        <button class="btn btn-outline" @click="goBack">Go Back</button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();
function goHome() {
  router.push('/');
}
function goBack() {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push('/');
  }
}
</script>

<style scoped>
.unauthorized-page {
  display: grid;
  place-items: center;
  padding: 40px 16px;
}

.card {
  width: 100%;
  max-width: 720px;
  background: var(--kw-panel-bg, #ffffff);
  border: 1px solid var(--kw-panel-border, #e5e7eb);
  border-radius: 16px;
  box-shadow: 0 12px 30px var(--kw-shadow, rgba(0, 0, 0, 0.12));
  text-align: center;
  padding: 40px 48px;
}

.icon-circle {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  border-radius: 50%;
  color: var(--kw-accent-orange, #e67e22);
  background: radial-gradient(
    120% 120% at 30% 20%,
    rgba(230, 126, 34, 0.15) 0%,
    rgba(230, 126, 34, 0.05) 100%
  );
  display: grid;
  place-items: center;
}
.icon-circle svg {
  width: 28px;
  height: 28px;
}

.title {
  margin: 6px 0 8px;
  font-size: 26px;
  line-height: 1.25;
  color: #1f2937;
  font-weight: 700;
}

.subtitle {
  margin: 0 auto 28px;
  color: #4b5563;
  max-width: 52ch;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.btn {
  appearance: none;
  border-radius: 10px;
  padding: 10px 16px;
  font-weight: 600;
  cursor: pointer;
  transition:
    transform 0.06s ease,
    box-shadow 0.2s ease,
    background 0.2s ease,
    color 0.2s ease;
}
.btn:active {
  transform: translateY(1px);
}

/* Keyboard accessibility: visible focus indicator */
.btn:focus-visible {
  outline: 2px solid var(--kw-focus-ring, #3b82f6);
  outline-offset: 2px;
}

.btn-primary {
  color: #fff;
  background: var(--kw-accent-orange, #e67e22);
  border: 1px solid var(--kw-accent-orange, #e67e22);
  box-shadow: 0 4px 12px rgba(230, 126, 34, 0.25);
}
.btn-primary:hover {
  background: var(--kw-accent-orange-dark, #d35400);
  border-color: var(--kw-accent-orange-dark, #d35400);
}

.btn-outline {
  background: #fff;
  color: #374151;
  border: 1px solid #e5e7eb;
}
.btn-outline:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

@media (max-width: 480px) {
  .card {
    padding: 28px 20px;
  }
  .title {
    font-size: 22px;
  }
}
</style>
