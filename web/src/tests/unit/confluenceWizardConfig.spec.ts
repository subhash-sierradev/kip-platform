import { describe, expect, it } from 'vitest';

import {
  createDefaultFormData,
  createSteps,
} from '@/components/outbound/confluenceintegration/wizard/confluenceWizardConfig';

describe('confluenceWizardConfig', () => {
  it('creates expected step titles for each mode', () => {
    expect(createSteps('create').map(s => s.title)).toEqual([
      'Integration Details',
      'Schedule Configuration',
      'Confluence Connection',
      'Confluence Configuration',
      'Review & Create',
    ]);

    expect(createSteps('edit').at(-1)?.title).toBe('Review & Update');
    expect(createSteps('clone').at(-1)?.title).toBe('Review & Clone');
  });

  it('creates default form data with stable initial values', () => {
    const form = createDefaultFormData();

    expect(form.name).toBe('');
    expect(form.itemType).toBe('DOCUMENT');
    expect(form.languageCodes).toEqual(['en']);
    expect(form.reportNameTemplate).toContain('{date}');
    expect(form.includeTableOfContents).toBe(true);
    expect(form.frequencyPattern).toBe('DAILY');
    expect(form.executionTime).toBe('02:00');
    expect(form.connectionMethod).toBe('existing');
    expect(form.existingConnectionId).toBe('');
    expect(form.createdConnectionId).toBe('');
    expect(form.username).toBe('');
    expect(form.password).toBe('');
  });
});
