import { describe, expect, it } from 'vitest';
import { computed, ref } from 'vue';

import { useCharacterCounter } from '@/composables/useCharacterCounter';

describe('useCharacterCounter', () => {
  describe('with default config', () => {
    it('initializes with empty text', () => {
      const text = ref('');
      const { length, counterClass, counterText, isValid } = useCharacterCounter(text);

      expect(length.value).toBe(0);
      expect(counterClass.value).toBe('counter-normal');
      expect(counterText.value).toBe('0/500');
      expect(isValid.value).toBe(true);
    });

    it('counts characters correctly', () => {
      const text = ref('Hello World');
      const { length } = useCharacterCounter(text);

      expect(length.value).toBe(11);
    });

    it('updates length when text changes', () => {
      const text = ref('Initial');
      const { length } = useCharacterCounter(text);

      expect(length.value).toBe(7);

      text.value = 'Updated text';
      expect(length.value).toBe(12);
    });

    it('shows counter-normal class when below warning threshold', () => {
      const text = ref('A'.repeat(449));
      const { counterClass } = useCharacterCounter(text);

      expect(counterClass.value).toBe('counter-normal');
    });

    it('shows counter-orange class at warning threshold', () => {
      const text = ref('A'.repeat(450));
      const { counterClass } = useCharacterCounter(text);

      expect(counterClass.value).toBe('counter-orange');
    });

    it('shows counter-orange class between warning and danger thresholds', () => {
      const text = ref('A'.repeat(460));
      const { counterClass } = useCharacterCounter(text);

      expect(counterClass.value).toBe('counter-orange');
    });

    it('shows counter-red class at danger threshold', () => {
      const text = ref('A'.repeat(475));
      const { counterClass } = useCharacterCounter(text);

      expect(counterClass.value).toBe('counter-red');
    });

    it('shows counter-red class above danger threshold', () => {
      const text = ref('A'.repeat(490));
      const { counterClass } = useCharacterCounter(text);

      expect(counterClass.value).toBe('counter-red');
    });

    it('displays correct counter text', () => {
      const text = ref('Test');
      const { counterText } = useCharacterCounter(text);

      expect(counterText.value).toBe('4/500');
    });

    it('updates counter text when text changes', () => {
      const text = ref('Start');
      const { counterText } = useCharacterCounter(text);

      text.value = 'End';
      expect(counterText.value).toBe('3/500');
    });

    it('isValid returns true when within limit', () => {
      const text = ref('A'.repeat(500));
      const { isValid } = useCharacterCounter(text);

      expect(isValid.value).toBe(true);
    });

    it('isValid returns false when exceeding limit', () => {
      const text = ref('A'.repeat(501));
      const { isValid } = useCharacterCounter(text);

      expect(isValid.value).toBe(false);
    });

    it('truncates text to max length', () => {
      const text = ref('Hello');
      const { truncateValue } = useCharacterCounter(text);

      const truncated = truncateValue('A'.repeat(600));
      expect(truncated.length).toBe(500);
      expect(truncated).toBe('A'.repeat(500));
    });

    it('does not truncate text below max length', () => {
      const text = ref('Hello');
      const { truncateValue } = useCharacterCounter(text);

      const truncated = truncateValue('Short text');
      expect(truncated).toBe('Short text');
    });

    it('handles null/undefined in truncateValue', () => {
      const text = ref('');
      const { truncateValue } = useCharacterCounter(text);

      expect(truncateValue(null as any)).toBe('');
      expect(truncateValue(undefined as any)).toBe('');
    });

    it('handles null/undefined text value', () => {
      const text = ref(null as any);
      const { length } = useCharacterCounter(text);

      expect(length.value).toBe(0);
    });
  });

  describe('with custom config', () => {
    it('uses custom maxLength', () => {
      const text = ref('Hello');
      const { counterText, isValid, truncateValue } = useCharacterCounter(text, { maxLength: 10 });

      expect(counterText.value).toBe('5/10');
      expect(isValid.value).toBe(true);
      expect(truncateValue('123456789012').length).toBe(10);
    });

    it('uses custom warning threshold', () => {
      const text = ref('A'.repeat(8));
      const { counterClass } = useCharacterCounter(text, {
        maxLength: 10,
        warningThreshold: 8,
      });

      expect(counterClass.value).toBe('counter-orange');
    });

    it('uses custom danger threshold', () => {
      const text = ref('A'.repeat(9));
      const { counterClass } = useCharacterCounter(text, {
        maxLength: 10,
        warningThreshold: 7,
        dangerThreshold: 9,
      });

      expect(counterClass.value).toBe('counter-red');
    });

    it('applies custom thresholds correctly', () => {
      const config = {
        maxLength: 100,
        warningThreshold: 75,
        dangerThreshold: 90,
      };

      const text1 = ref('A'.repeat(74));
      expect(useCharacterCounter(text1, config).counterClass.value).toBe('counter-normal');

      const text2 = ref('A'.repeat(75));
      expect(useCharacterCounter(text2, config).counterClass.value).toBe('counter-orange');

      const text3 = ref('A'.repeat(90));
      expect(useCharacterCounter(text3, config).counterClass.value).toBe('counter-red');
    });
  });

  describe('edge cases', () => {
    it('handles exactly at maxLength', () => {
      const text = ref('A'.repeat(500));
      const { isValid, counterText } = useCharacterCounter(text);

      expect(isValid.value).toBe(true);
      expect(counterText.value).toBe('500/500');
    });

    it('handles one character over maxLength', () => {
      const text = ref('A'.repeat(501));
      const { isValid } = useCharacterCounter(text);

      expect(isValid.value).toBe(false);
    });

    it('handles empty string', () => {
      const text = ref('');
      const { length, isValid, counterClass } = useCharacterCounter(text);

      expect(length.value).toBe(0);
      expect(isValid.value).toBe(true);
      expect(counterClass.value).toBe('counter-normal');
    });

    it('handles unicode characters', () => {
      const text = ref('Hello 世界 🌍');
      const { length } = useCharacterCounter(text);

      expect(length.value).toBe(11); // JavaScript counts code units (emoji is 2 units)
    });

    it('handles newlines and special characters', () => {
      const text = ref('Line 1\nLine 2\tTabbed');
      const { length } = useCharacterCounter(text);

      expect(length.value).toBe(20); // 'Line 1' (6) + \n (1) + 'Line 2' (6) + \t (1) + 'Tabbed' (6) = 20
    });
  });

  describe('reactivity', () => {
    it('reacts to text changes', () => {
      const text = ref('Initial');
      const counter = useCharacterCounter(text);

      expect(counter.length.value).toBe(7);
      expect(counter.counterText.value).toBe('7/500');

      text.value = 'Updated';
      expect(counter.length.value).toBe(7);
      expect(counter.counterText.value).toBe('7/500');

      text.value = '';
      expect(counter.length.value).toBe(0);
      expect(counter.counterText.value).toBe('0/500');
    });

    it('reacts to text causing class changes', () => {
      const text = ref('A'.repeat(400));
      const { counterClass } = useCharacterCounter(text);

      expect(counterClass.value).toBe('counter-normal');

      text.value = 'A'.repeat(455);
      expect(counterClass.value).toBe('counter-orange');

      text.value = 'A'.repeat(480);
      expect(counterClass.value).toBe('counter-red');

      text.value = 'A'.repeat(100);
      expect(counterClass.value).toBe('counter-normal');
    });

    it('works with computed refs', () => {
      const baseText = ref('Hello');
      const text = computed(() => `${baseText.value} World`);
      const { length } = useCharacterCounter(text);

      expect(length.value).toBe(11);

      baseText.value = 'Hi';
      expect(length.value).toBe(8);
    });
  });
});
