<template>
  <div class="sample-payload-tab">
    <!-- Payload Content -->
    <div class="payload-container">
      <div class="json-viewer">
        <!-- Copy Button -->
        <button
          class="copy-button"
          @click="copyToClipboard"
          :disabled="copying"
          :title="copying ? 'Copied!' : 'Copy'"
        >
          <i class="dx-icon-copy"></i>
          <span class="copy-text">{{ copying ? 'Copied!' : 'Copy' }}</span>
        </button>

        <pre class="json-content"><code>{{ formattedPayload }}</code></pre>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, type PropType } from 'vue';
import type { JiraWebhookDetail } from '@/api/services/JiraWebhookService';

export default defineComponent({
  name: 'SamplePayloadTab',
  props: {
    webhookData: {
      type: Object as PropType<JiraWebhookDetail | null>,
      default: null,
    },
    webhookId: {
      type: String,
      required: true,
    },
  },
  setup(props) {
    const copying = ref(false);

    // Display the samplePayload directly from webhook data
    const samplePayload = computed(() => {
      if (!props.webhookData) {
        return {
          message: 'Loading webhook data...',
        };
      }

      // Return the samplePayload field from the webhook response
      return (
        props.webhookData.samplePayload || {
          message: 'No sample payload available for this webhook',
        }
      );
    });

    const formattedPayload = computed(() => {
      try {
        const payload = samplePayload.value;

        // If the payload is already a string, try to parse and re-stringify it
        if (typeof payload === 'string') {
          try {
            const parsed = JSON.parse(payload);
            return JSON.stringify(parsed, null, 2);
          } catch {
            // If it's not valid JSON, return as is
            return payload;
          }
        }

        // If it's an object, stringify it with formatting
        return JSON.stringify(payload, null, 2);
      } catch (error) {
        console.error('Error formatting payload:', error);
        return String(samplePayload.value);
      }
    });

    const copyToClipboard = async () => {
      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(formattedPayload.value);
          copying.value = true;
          setTimeout(() => {
            copying.value = false;
          }, 2000);
        } else {
          console.warn('Clipboard API not available');
        }
      } catch (err) {
        console.error('Failed to copy to clipboard:', err);
      }
    };

    const formatJSON = () => {
      // JSON is already formatted, but this could trigger re-formatting if needed
      // JSON formatting triggered
    };

    return {
      formattedPayload,
      copying,
      copyToClipboard,
      formatJSON,
    };
  },
});
</script>

<style scoped>
.sample-payload-tab {
  padding: 0.5rem;
  background: #f8fafc;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
}

/* Header Section */
.payload-header {
  margin-bottom: 1.5rem;
  padding: 0;
  background: transparent;
  border: none;
  flex-shrink: 0;
}

.payload-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 0.5rem 0;
  line-height: 1.25;
}

.payload-description {
  font-size: 0.875rem;
  color: #6b7280;
  margin: 0;
  line-height: 1.5;
}

/* Payload Container */
.payload-container {
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 0.75rem;
  box-shadow:
    0 1px 3px 0 rgba(0, 0, 0, 0.1),
    0 1px 2px 0 rgba(0, 0, 0, 0.06);
  overflow: hidden;
  flex: 1;
  min-height: 0;
}

.payload-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1rem 0;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  background: white;
  color: #374151;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: #f9fafb;
  border-color: #9ca3af;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.copy-btn:disabled {
  background: #10b981;
  color: white;
  border-color: #10b981;
  cursor: not-allowed;
  transform: none;
}

.btn-icon {
  font-size: 0.875rem;
}

/* JSON Viewer */
.json-viewer {
  flex: 1;
  display: flex;
  background: #ffffff;
  overflow: hidden;
  min-height: 0;
  height: 100%;
  position: relative;
}

/* Copy Button */
.copy-button {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #e2e8f0;
  border-radius: 0.375rem;
  color: #64748b;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  backdrop-filter: blur(4px);
  z-index: 1;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.copy-button:hover {
  background: rgba(248, 250, 252, 0.95);
  border-color: #cbd5e1;
  color: #475569;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.copy-button:disabled {
  background: rgba(55, 65, 81, 0.9);
  color: white;
  border-color: #374151;
  cursor: not-allowed;
  transform: none;
}

.copy-button .dx-icon-copy {
  font-size: 0.875rem;
}

.copy-text {
  font-size: 0.75rem;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .copy-text {
    display: none;
  }

  .copy-button {
    padding: 0.5rem;
    gap: 0;
  }
}

.json-content {
  background: #f8fafc;
  color: #374151;
  padding: 1.5rem;
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Courier New', monospace;
  font-size: 0.702rem;
  line-height: 1.6;
  overflow: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
  word-break: break-word;
  width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 0 0 0.75rem 0.75rem;
  tab-size: 2;
  box-sizing: border-box;
  flex: 1;
  min-height: 0;
  height: 100%;
}

/* JSON Syntax Highlighting (Light Theme) */
.json-content {
  color: #374151;
  background: #f8fafc;
}

/* Responsive Design */
@media (max-width: 768px) {
  .sample-payload-tab {
    padding: 1rem;
  }

  .json-content {
    font-size: 0.602rem;
    padding: 1rem;
  }

  .payload-title {
    font-size: 1.125rem;
  }
}

@media (max-width: 480px) {
  .sample-payload-tab {
    padding: 0.75rem;
  }
}
</style>
