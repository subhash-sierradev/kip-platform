import { describe, expect, it, vi } from 'vitest';

import { useConfirmationDialog } from '@/composables/useConfirmationDialog';

describe('useConfirmationDialog', () => {
  describe('dialogTitle computed', () => {
    it('should return "Confirm Action" when action has no title override and is not in config', () => {
      const dialog = useConfirmationDialog({
        runNow: { title: '', desc: 'Run the job now', label: 'Run' },
      });
      dialog.openDialog('runNow');

      // title is empty string — falsy — so fallback is "Confirm Action"
      expect(dialog.dialogTitle.value).toBe('Confirm Action');
    });

    it('should return configured title when present', () => {
      const dialog = useConfirmationDialog();
      dialog.openDialog('delete');

      expect(dialog.dialogTitle.value).toBe('Delete Webhook');
    });
  });

  describe('dialogDescription computed', () => {
    it('should return empty string when desc is absent for the action', () => {
      const dialog = useConfirmationDialog({
        runNow: { title: 'Run Now', desc: '', label: 'Run' },
      });
      dialog.openDialog('runNow');

      expect(dialog.dialogDescription.value).toBe('');
    });
  });

  describe('dialogConfirmLabel computed', () => {
    it('should return "Confirm" when label is absent for the action', () => {
      const dialog = useConfirmationDialog({
        runNow: { title: 'Run Now', desc: 'desc', label: '' },
      });
      dialog.openDialog('runNow');

      expect(dialog.dialogConfirmLabel.value).toBe('Confirm');
    });
  });

  describe('confirmWithHandlers — runNow action', () => {
    it('should call handlers.runNow when pendingAction is runNow', async () => {
      const runNowHandler = vi.fn().mockResolvedValue(undefined);
      const dialog = useConfirmationDialog();

      dialog.openDialog('runNow');
      await dialog.confirmWithHandlers({ runNow: runNowHandler });

      expect(runNowHandler).toHaveBeenCalledOnce();
      expect(dialog.dialogOpen.value).toBe(false);
    });
  });

  describe('openDialog and closeDialog', () => {
    it('should set pendingAction and open the dialog on openDialog', () => {
      const dialog = useConfirmationDialog();
      dialog.openDialog('enable');

      expect(dialog.dialogOpen.value).toBe(true);
      expect(dialog.pendingAction.value).toBe('enable');
    });

    it('should clear pendingAction and close on closeDialog', () => {
      const dialog = useConfirmationDialog();
      dialog.openDialog('disable');
      dialog.closeDialog();

      expect(dialog.dialogOpen.value).toBe(false);
      expect(dialog.pendingAction.value).toBeNull();
    });
  });
});
