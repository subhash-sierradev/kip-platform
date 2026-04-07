<!-- WebhookSuccessDialog.vue -->
<template>
  <div v-if="open" class="wsd-backdrop">
    <div class="wsd-modal">
      <!-- ICON + TITLE IN ONE ROW -->
      <div class="wsd-title-row">
        <svg class="wsd-success-icon" viewBox="0 0 24 24">
          <path
            d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2m-2 15-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8z"
          />
        </svg>

        <h2 class="wsd-title">
          {{ editMode ? 'Webhook Updated Successfully' : 'Webhook Created Successfully' }}
        </h2>
      </div>

      <!-- WEBHOOK URL LABEL -->
      <div class="wsd-url-label">Webhook URL:</div>

      <!-- URL + COPY ICON -->
      <div class="wsd-url-row">
        <p class="wsd-url-text" :title="webhook?.webhookUrl">
          {{ webhook?.webhookUrl }}
        </p>

        <button class="wsd-copy-icon" aria-label="Copy webhook URL" @click="handleCopy">
          <svg class="wsd-copy-svg" viewBox="0 0 24 24">
            <path
              d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12zm3 4H8c-1.1 0-2 .9-2 2v14c0
              1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2m0
              16H8V7h11z"
            />
          </svg>
        </button>
      </div>

      <!-- INFO SECTION -->
      <div class="wsd-info-card">
        <h6 class="wsd-info-heading">📋 How to Use This Webhook</h6>

        <div class="wsd-info-text">
          <ol class="wsd-steps-list">
            <li class="wsd-step-item">
              <strong>Copy the webhook URL above</strong> and configure it in your Kaseware
              Application.
            </li>

            <li class="wsd-step-item">
              <strong>Configure your Kaseware Application</strong> to send HTTP POST requests to
              this URL when events occur.
            </li>

            <li class="wsd-step-item">
              <strong>The webhook will automatically create Jira tickets</strong>
              based on your configured field mapping settings.
            </li>

            <li class="wsd-step-item">
              <strong>Monitor webhook activity</strong>
              in the main dashboard to track successful ticket creation.
            </li>
          </ol>
        </div>
      </div>

      <!-- BUTTON -->
      <div class="wsd-actions">
        <button class="wsd-primary-btn" @click="$emit('close')">OK, I Understand</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const props = defineProps<{
  open: boolean;
  webhook?: { webhookUrl: string };
  editMode?: boolean;
}>();

const emit = defineEmits(['close', 'copy-url']);

const copied = ref(false);

async function handleCopy() {
  if (!props.webhook?.webhookUrl) return;

  try {
    await navigator.clipboard.writeText(props.webhook.webhookUrl);
    copied.value = true;
    emit('copy-url');

    setTimeout(() => {
      copied.value = false;
    }, 1500);
  } catch (err) {
    console.error('Copy failed', err);
  }
}
</script>

<style scoped>
/* BACKDROP */
.wsd-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

/* MODAL */
.wsd-modal {
  width: 700px;
  max-width: 95vw;
  background: white;
  border-radius: 10px;
  padding: 32px;
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.25);
  animation: popup 0.2s ease-out;
}

@keyframes popup {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* TITLE ROW */
.wsd-title-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 20px;
}

/* SUCCESS ICON (MUI EXACT) */
/* SUCCESS ICON (MUI exact match from css-gu9sq2) */
.wsd-success-icon {
  user-select: none;
  width: 1em;
  height: 1em;
  display: inline-block;
  flex-shrink: 0;
  fill: currentColor;
  color: rgb(22, 163, 74); /* MUI green-600 */
  font-size: 24px; /* reduced from 32px */
  margin-right: 12px;
  transition: fill 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

/* TITLE */
.wsd-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #166534;
  font-family:
    'Segoe UI',
    system-ui,
    -apple-system,
    sans-serif;
  letter-spacing: -0.025em;
}

/* URL LABEL */
.wsd-url-label {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 6px;
  color: #374151;
  font-family:
    'Segoe UI',
    system-ui,
    -apple-system,
    sans-serif;
}

/* URL ROW */
.wsd-url-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* URL TEXT BOX (css-jjuxh9) */
.wsd-url-text {
  margin: 0;
  font-weight: 400;
  color: rgb(55, 65, 81);
  font-size: 14px;
  font-family: monospace;
  background: rgb(248, 249, 250);
  padding: 4px 8px;
  border-radius: 4px;
  word-break: break-all;
  line-height: 1.5;
  flex: 1 1 0%;
}

/* COPY ICON BUTTON (css-1wee14y) */
.wsd-copy-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  cursor: pointer;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  color: rgb(107, 114, 128);
  padding: 5px;
  transition: background-color 150ms;
}

.wsd-copy-icon:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

/* COPY SVG */
.wsd-copy-svg {
  width: 20px;
  height: 20px;
  fill: rgb(107, 114, 128);
}

/* INFO CARD */
.wsd-info-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 20px;
  border-radius: 10px;
  margin-top: 16px;
}

/* INFO TITLE (css-1b32qtp) */
.wsd-info-heading {
  margin: 0 0 16px;
  font-family:
    'Segoe UI',
    system-ui,
    -apple-system,
    sans-serif;
  font-size: 16px;
  font-weight: 600;
  color: rgb(66, 66, 66);
  display: flex;
  align-items: center;
  gap: 8px;
}

/* INFO BODY TEXT */
.wsd-info-text {
  font-family:
    'Segoe UI',
    system-ui,
    -apple-system,
    sans-serif;
  font-size: 14px;
  font-weight: 400;
  color: rgb(75, 85, 99);
  line-height: 1.5;
}

/* LIST STYLES */
.wsd-steps-list {
  padding-left: 18px;
  margin: 0;
}

.wsd-step-item {
  margin-bottom: 10px;
  font-size: 14px;
  line-height: 1.6;
}

.wsd-step-item strong {
  font-weight: 600;
  color: rgb(55, 65, 81);
}

/* ACTION BUTTON */
.wsd-actions {
  text-align: center;
  margin-top: 24px;
}

.wsd-primary-btn {
  background: #f59e0b;
  padding: 12px 32px;
  border-radius: 8px;
  color: white;
  border: none;
  font-size: 14px;
  font-weight: 500;
  font-family:
    'Segoe UI',
    system-ui,
    -apple-system,
    sans-serif;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.wsd-primary-btn:hover {
  background: #d97706;
}
</style>
