import { computed, ref } from 'vue';

export type ConfirmAction = 'enable' | 'disable' | 'delete' | 'runNow' | 'custom';

export interface DialogConfigItem {
  title: string;
  desc: string;
  label: string;
}

export type DialogConfigMap = Partial<Record<ConfirmAction, DialogConfigItem>>;

export function createDefaultDialogConfig(): Required<
  Pick<DialogConfigMap, 'enable' | 'disable' | 'delete'>
> {
  return {
    enable: {
      title: 'Enable Webhook',
      desc: 'Enabling this webhook will allow it to trigger when matching events occur.',
      label: 'Enable',
    },
    disable: {
      title: 'Disable Webhook',
      desc: 'Disabling this webhook will prevent it from triggering until it is enabled again.',
      label: 'Disable',
    },
    delete: {
      title: 'Delete Webhook',
      desc: 'Deleting this webhook will remove it permanently and stop any future triggers. This action cannot be undone.',
      label: 'Delete',
    },
  };
}

export function useConfirmationDialog(dialogConfig: DialogConfigMap = {}) {
  const dialogOpen = ref(false);
  const actionLoading = ref(false);
  const pendingAction = ref<ConfirmAction | null>(null);

  // Deep merge with defaults to ensure all required properties exist
  const defaults = createDefaultDialogConfig();
  const fullConfig: DialogConfigMap = {
    enable: { ...defaults.enable, ...dialogConfig.enable },
    disable: { ...defaults.disable, ...dialogConfig.disable },
    delete: { ...defaults.delete, ...dialogConfig.delete },
    ...dialogConfig,
  };

  function openDialog(type: ConfirmAction) {
    pendingAction.value = type;
    dialogOpen.value = true;
  }

  function closeDialog() {
    dialogOpen.value = false;
    pendingAction.value = null;
  }

  const dialogTitle = computed(() => {
    const action = pendingAction.value;
    if (!action || action === 'custom') return 'Confirm Action';
    return fullConfig[action]?.title || 'Confirm Action';
  });

  const dialogDescription = computed(() => {
    const action = pendingAction.value;
    return action && action !== 'custom' ? fullConfig[action]?.desc || '' : '';
  });

  const dialogConfirmLabel = computed(() => {
    const action = pendingAction.value;
    return action && action !== 'custom' ? fullConfig[action]?.label || 'Confirm' : 'Confirm';
  });

  async function confirmWithHandlers(handlers: {
    delete?: () => Promise<boolean> | boolean;
    enable?: () => Promise<void> | void;
    disable?: () => Promise<void> | void;
    runNow?: () => Promise<void> | void;
    custom?: () => Promise<void> | void;
  }) {
    actionLoading.value = true;
    try {
      let success = true;
      if (pendingAction.value === 'delete' && handlers.delete) {
        const result = await handlers.delete();
        success = !!result;
      } else if (pendingAction.value === 'enable' && handlers.enable) {
        await handlers.enable();
      } else if (pendingAction.value === 'disable' && handlers.disable) {
        await handlers.disable();
      } else if (pendingAction.value === 'runNow' && handlers.runNow) {
        await handlers.runNow();
      } else if (pendingAction.value === 'custom' && handlers.custom) {
        await handlers.custom();
      }
      if (success) {
        closeDialog();
      }
    } finally {
      actionLoading.value = false;
    }
  }

  return {
    // state
    dialogOpen,
    actionLoading,
    pendingAction,
    // derived
    dialogTitle,
    dialogDescription,
    dialogConfirmLabel,
    // actions
    openDialog,
    closeDialog,
    confirmWithHandlers,
  };
}
