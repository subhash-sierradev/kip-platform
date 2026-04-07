/* eslint-disable simple-import-sort/imports */
import { describe, it, expect } from 'vitest';

import { useTroubleshootDialog } from '@/composables/useTroubleshootDialog';

describe('useTroubleshootDialog', () => {
  it('initializes with hidden dialog and null selection', () => {
    const { troubleshootDialogVisible, selectedExecution, currentExecution } =
      useTroubleshootDialog();

    expect(troubleshootDialogVisible.value).toBe(false);
    expect(selectedExecution.value).toBeNull();
    expect(currentExecution.value).toBeNull();
  });

  it('shows dialog with provided execution and closes/reset correctly', () => {
    const {
      troubleshootDialogVisible,
      selectedExecution,
      currentExecution,
      showTroubleshootDialog,
      closeTroubleshootDialog,
    } = useTroubleshootDialog();

    const exec = { id: '1', status: 'OK' } as any;
    showTroubleshootDialog(exec);

    expect(troubleshootDialogVisible.value).toBe(true);
    expect(selectedExecution.value).toStrictEqual(exec);
    expect(currentExecution.value).toStrictEqual(exec);

    closeTroubleshootDialog();

    expect(troubleshootDialogVisible.value).toBe(false);
    expect(selectedExecution.value).toBeNull();
    expect(currentExecution.value).toBeNull();
  });
});
