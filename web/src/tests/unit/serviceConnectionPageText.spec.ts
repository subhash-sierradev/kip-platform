import { describe, expect, it } from 'vitest';

import { ServiceType } from '@/api/models/enums';
import {
  getConfirmDialogDescription,
  getDependentsNameCaption,
} from '@/components/common/serviceConnectionPageText';

describe('serviceConnectionPageText', () => {
  it('returns an empty confirm description when no action is selected', () => {
    expect(getConfirmDialogDescription(null, ServiceType.JIRA)).toBe('');
  });

  it('falls back to the raw service type label when metadata is unavailable', () => {
    expect(getConfirmDialogDescription('test', 'UNKNOWN' as ServiceType)).toContain('UNKNOWN');
  });

  it('falls back to the generic dependents label when no service type is provided', () => {
    expect(getDependentsNameCaption()).toBe('Integration Name');
  });

  it('falls back to the generic dependents label for unmapped service types', () => {
    expect(getDependentsNameCaption('UNKNOWN' as ServiceType)).toBe('Integration Name');
  });
});
