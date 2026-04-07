import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  createDefaultDialogConfig,
  useConfirmationDialog,
} from '@/composables/useConfirmationDialog';

describe('state', () => {
  beforeEach(() => vi.clearAllMocks());

  it('initial state is closed with no pending action and not loading', () => {
    const { dialogOpen, actionLoading, pendingAction } = useConfirmationDialog(
      createDefaultDialogConfig()
    );
    expect(dialogOpen.value).toBe(false);
    expect(actionLoading.value).toBe(false);
    expect(pendingAction.value).toBeNull();
  });

  it('openDialog sets pending action and opens the dialog', () => {
    const { dialogOpen, pendingAction, openDialog } = useConfirmationDialog(
      createDefaultDialogConfig()
    );
    openDialog('enable');
    expect(dialogOpen.value).toBe(true);
    expect(pendingAction.value).toBe('enable');
  });

  it('closeDialog closes and clears pending action', () => {
    const { dialogOpen, pendingAction, openDialog, closeDialog } = useConfirmationDialog(
      createDefaultDialogConfig()
    );
    openDialog('disable');
    expect(dialogOpen.value).toBe(true);
    expect(pendingAction.value).toBe('disable');
    closeDialog();
    expect(dialogOpen.value).toBe(false);
    expect(pendingAction.value).toBeNull();
  });
});

describe('labels', () => {
  it('computed labels reflect config for enable/disable/delete and custom', () => {
    const { dialogTitle, dialogDescription, dialogConfirmLabel, openDialog } =
      useConfirmationDialog(createDefaultDialogConfig());

    openDialog('enable');
    expect(dialogTitle.value).toBe('Enable Webhook');
    expect(dialogDescription.value).toContain('Enabling this webhook');
    expect(dialogConfirmLabel.value).toBe('Enable');

    openDialog('disable');
    expect(dialogTitle.value).toBe('Disable Webhook');
    expect(dialogDescription.value).toContain('Disabling this webhook');
    expect(dialogConfirmLabel.value).toBe('Disable');

    openDialog('delete');
    expect(dialogTitle.value).toBe('Delete Webhook');
    expect(dialogDescription.value).toContain('Deleting this webhook');
    expect(dialogConfirmLabel.value).toBe('Delete');

    openDialog('custom');
    expect(dialogTitle.value).toBe('Confirm Action');
    expect(dialogDescription.value).toBe('');
    expect(dialogConfirmLabel.value).toBe('Confirm');
  });
});

describe('confirmWithHandlers', () => {
  it('delete closes on success=true and stays open on false', async () => {
    const { openDialog, dialogOpen, confirmWithHandlers } = useConfirmationDialog(
      createDefaultDialogConfig()
    );
    openDialog('delete');
    const deleteTrue = vi.fn().mockResolvedValue(true);
    await confirmWithHandlers({ delete: deleteTrue });
    expect(deleteTrue).toHaveBeenCalledTimes(1);
    expect(dialogOpen.value).toBe(false);

    openDialog('delete');
    const deleteFalse = vi.fn().mockResolvedValue(false);
    await confirmWithHandlers({ delete: deleteFalse });
    expect(deleteFalse).toHaveBeenCalledTimes(1);
    expect(dialogOpen.value).toBe(true);
  });

  it('enable/disable/custom close dialog after running', async () => {
    const { openDialog, dialogOpen, confirmWithHandlers } = useConfirmationDialog(
      createDefaultDialogConfig()
    );

    const enableHandler = vi.fn().mockResolvedValue(undefined);
    openDialog('enable');
    await confirmWithHandlers({ enable: enableHandler });
    expect(enableHandler).toHaveBeenCalledTimes(1);
    expect(dialogOpen.value).toBe(false);

    const disableHandler = vi.fn().mockResolvedValue(undefined);
    openDialog('disable');
    await confirmWithHandlers({ disable: disableHandler });
    expect(disableHandler).toHaveBeenCalledTimes(1);
    expect(dialogOpen.value).toBe(false);

    const customHandler = vi.fn().mockResolvedValue(undefined);
    openDialog('custom');
    await confirmWithHandlers({ custom: customHandler });
    expect(customHandler).toHaveBeenCalledTimes(1);
    expect(dialogOpen.value).toBe(false);
  });

  it('actionLoading toggles true during execution and false after', async () => {
    const { openDialog, actionLoading, confirmWithHandlers } = useConfirmationDialog(
      createDefaultDialogConfig()
    );
    openDialog('custom');
    const customHandler = vi.fn().mockImplementation(async () => {
      expect(actionLoading.value).toBe(true);
    });
    await confirmWithHandlers({ custom: customHandler });
    expect(actionLoading.value).toBe(false);
  });
});
