import { ref } from 'vue';
// Fix for MouseEvent type in Vue composable
type MouseEvent = globalThis.MouseEvent;

export interface TooltipState {
  visible: boolean;
  text: string;
  x: number;
  y: number;
}

export function useTooltip() {
  const tooltip = ref<TooltipState>({
    visible: false,
    text: '',
    x: 0,
    y: 0,
  });

  function showTooltip(event: MouseEvent, text: string) {
    tooltip.value.visible = true;
    tooltip.value.text = text;
    tooltip.value.x = event.clientX + 12;
    tooltip.value.y = event.clientY + 12;
  }

  function moveTooltip(event: MouseEvent) {
    tooltip.value.x = event.clientX + 12;
    tooltip.value.y = event.clientY + 12;
  }

  function hideTooltip() {
    tooltip.value.visible = false;
  }

  return {
    tooltip,
    showTooltip,
    moveTooltip,
    hideTooltip,
  };
}
