/**
 * Reusable composable for character counter functionality
 * Provides character counting, validation, and styling logic
 */
import { computed, type Ref } from 'vue';

export interface CharacterCounterConfig {
  maxLength?: number;
  warningThreshold?: number;
  dangerThreshold?: number;
}

export interface CharacterCounterReturn {
  length: Ref<number>;
  counterClass: Ref<string>;
  counterText: Ref<string>;
  isValid: Ref<boolean>;
  truncateValue: (value: string) => string;
}

/**
 * Character counter composable
 * @param text - Reactive reference to the text value
 * @param config - Configuration options
 * @returns Object with counter properties and utilities
 */
export function useCharacterCounter(
  text: Ref<string>,
  config: CharacterCounterConfig = {}
): CharacterCounterReturn {
  const { maxLength = 500, warningThreshold = 450, dangerThreshold = 475 } = config;

  // Computed properties
  const length = computed(() => text.value?.length || 0);

  const counterClass = computed(() => {
    if (length.value >= dangerThreshold) return 'counter-red';
    if (length.value >= warningThreshold) return 'counter-orange';
    return 'counter-normal';
  });

  const counterText = computed(() => `${length.value}/${maxLength}`);

  const isValid = computed(() => length.value <= maxLength);

  // Utility function to truncate text to max length
  const truncateValue = (value: string): string => {
    return value?.slice(0, maxLength) || '';
  };

  return {
    length,
    counterClass,
    counterText,
    isValid,
    truncateValue,
  };
}

/**
 * Character counter CSS classes for consistent styling
 * Add these classes to your component's CSS
 */
export const characterCounterStyles = `
/* Character counter base styles */
.counter-base {
  font-size: 12px;
  font-weight: 500;
  transition: color 0.2s;
  margin-left: auto;
}

.counter-normal {
  color: #6b7280;
}

.counter-orange {
  color: #f59e0b;
}

.counter-red {
  color: #dc2626;
}
`;
