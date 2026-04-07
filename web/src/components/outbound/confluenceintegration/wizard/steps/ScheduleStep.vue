<template>
  <ScheduleConfigurationStep
    :model-value="modelValue"
    mode="full"
    @update:model-value="emit('update:modelValue', $event)"
    @validation-change="emit('validation-change', $event)"
  />
</template>

<script setup lang="ts">
import ScheduleConfigurationStep from '@/components/common/schedulestep/ScheduleConfigurationStep.vue';
import type { ScheduleConfigurationData } from '@/types/ArcGISFormData';

defineOptions({ name: 'ScheduleStep' });

interface Props {
  modelValue: ScheduleConfigurationData;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'update:modelValue', value: ScheduleConfigurationData): void;
  (e: 'validation-change', isValid: boolean): void;
}>();

// ensure props is referenced to avoid TypeScript unused-variable warning
void props;
</script>
