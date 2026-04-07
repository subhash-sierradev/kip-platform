<template>
  <CommonScheduleStep
    :model-value="modelValue"
    mode="full"
    @update:model-value="emit('update:modelValue', $event)"
    @validation-change="emit('validation-change', $event)"
  />
</template>

<script setup lang="ts">
import CommonScheduleStep from '@/components/common/schedulestep/ScheduleConfigurationStep.vue';
import type { ScheduleConfigurationData } from '@/types/ArcGISFormData';

defineOptions({ name: 'ScheduleConfigurationStep' });

defineProps<{ modelValue: ScheduleConfigurationData }>();
const emit = defineEmits<{
  (e: 'update:modelValue', v: ScheduleConfigurationData): void;
  (e: 'validation-change', v: boolean): void;
}>();
</script>
