<template>
  <header class="jw-header">
    <div class="jw-header-inner">
      <div class="jw-stepper">
        <template v-for="(label, index) in steps" :key="label">
          <div class="jw-step">
            <div
              class="jw-step-indicator"
              :class="{
                'jw-active': index === activeStep,
                'jw-completed': index < activeStep,
              }"
            >
              <span v-if="index >= activeStep">{{ index + 1 }}</span>
              <span v-else class="jw-check">✔</span>
            </div>
            <div class="jw-step-label">{{ label }}</div>
          </div>

          <div
            v-if="index < steps.length - 1"
            class="jw-step-line"
            :class="{
              'jw-line-active': index < activeStep,
              'jw-line-inactive': index >= activeStep,
            }"
          ></div>
        </template>
      </div>

      <button type="button" class="jw-icon-button" @click="emit('close')">✕</button>
    </div>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  steps: string[];
  activeStep: number;
}>();

const emit = defineEmits<{ (e: 'close'): void }>();
</script>
