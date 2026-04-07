import { type ComponentPublicInstance, nextTick, type Ref, ref } from 'vue';

import type { NotificationRuleResponse } from '@/api';

import { useConfirmationDialog } from './useConfirmationDialog';

const TOGGLE_DIALOG_CONFIG = {
  enable: {
    title: 'Enable Rule',
    desc: 'Enabling this rule will allow it to generate notifications for matching events.',
    label: 'Enable',
  },
  disable: {
    title: 'Disable Rule',
    desc: 'Disabling this rule will stop notifications from being generated until it is re-enabled.',
    label: 'Disable',
  },
};

export function useNotificationRulesToggle(
  toggleRule: (id: string) => Promise<boolean>,
  rules: Ref<NotificationRuleResponse[]>
) {
  const {
    dialogOpen: toggleDialogOpen,
    actionLoading: toggleActionLoading,
    pendingAction: togglePendingAction,
    dialogTitle: toggleDialogTitle,
    dialogDescription: toggleDialogDescription,
    dialogConfirmLabel: toggleDialogConfirmLabel,
    openDialog: openToggleDialog,
    closeDialog: closeToggleDialog,
    confirmWithHandlers: confirmToggleWithHandlers,
  } = useConfirmationDialog(TOGGLE_DIALOG_CONFIG);

  const rowToggleLoading = ref<Record<string, boolean>>({});
  const pendingToggleRuleId = ref<string | null>(null);
  const rulesGridRef = ref<(ComponentPublicInstance & { instance?: { repaint(): void } }) | null>(
    null
  );

  function getRuleEnabled(id: string): boolean {
    return rules.value.find(r => r.id === id)?.isEnabled ?? false;
  }

  function handleToggleClick(id: string): void {
    if (rowToggleLoading.value[id]) return;
    const currentlyEnabled = getRuleEnabled(id);
    rowToggleLoading.value[id] = true;
    pendingToggleRuleId.value = id;
    openToggleDialog(currentlyEnabled ? 'disable' : 'enable');
  }

  function handleToggleCancel(): void {
    if (pendingToggleRuleId.value) {
      rowToggleLoading.value[pendingToggleRuleId.value] = false;
      pendingToggleRuleId.value = null;
    }
    closeToggleDialog();
  }

  async function executeToggle(): Promise<void> {
    if (!pendingToggleRuleId.value) return;
    const id = pendingToggleRuleId.value;
    const ok = await toggleRule(id);
    rowToggleLoading.value[id] = false;
    pendingToggleRuleId.value = null;
    if (ok) {
      await nextTick();
      rulesGridRef.value?.instance?.repaint();
    }
  }

  async function handleToggleConfirm(): Promise<void> {
    await confirmToggleWithHandlers({ enable: executeToggle, disable: executeToggle });
  }

  return {
    rulesGridRef,
    rowToggleLoading,
    getRuleEnabled,
    handleToggleClick,
    handleToggleCancel,
    handleToggleConfirm,
    toggleDialogOpen,
    toggleActionLoading,
    togglePendingAction,
    toggleDialogTitle,
    toggleDialogDescription,
    toggleDialogConfirmLabel,
  };
}
