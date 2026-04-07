import { ref } from 'vue';

export function useActionTooltip(rowId: string) {
  const actionTipVisible = ref(false);
  const actionTipX = ref(0);
  const actionTipY = ref(0);
  const actionTipText = ref('');
  const actionTipId = `row-tooltip-${rowId}`;

  function onActionEnter(payload: { text: string; event: MouseEvent }) {
    actionTipText.value = payload.text;
    actionTipVisible.value = true;
    actionTipX.value = payload.event.clientX + 12;
    actionTipY.value = payload.event.clientY + 12;
  }

  function onActionMove(event: MouseEvent) {
    actionTipX.value = event.clientX + 12;
    actionTipY.value = event.clientY + 12;
  }

  function onActionLeave() {
    actionTipVisible.value = false;
  }

  return {
    actionTipVisible,
    actionTipX,
    actionTipY,
    actionTipText,
    actionTipId,
    onActionEnter,
    onActionMove,
    onActionLeave,
  };
}
