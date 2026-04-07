import { describe, expect, it } from 'vitest';

import {
  eventColumns,
  notificationsPageTabs,
  recipientColumns,
  ruleColumns,
  templateColumns,
} from '@/components/admin/notifications/notificationsPageColumns';

describe('notificationsPageColumns', () => {
  it('notificationsPageTabs has 4 tabs with id and label', () => {
    expect(notificationsPageTabs).toHaveLength(4);
    const ids = notificationsPageTabs.map(t => t.id);
    expect(ids).toContain('events');
    expect(ids).toContain('templates');
    expect(ids).toContain('rules');
    expect(ids).toContain('recipients');
    notificationsPageTabs.forEach(tab => {
      expect(tab).toHaveProperty('id');
      expect(tab).toHaveProperty('label');
    });
  });

  it('eventColumns has 5 columns with expected dataFields', () => {
    expect(eventColumns).toHaveLength(5);
    const fields = eventColumns.map(c => c.dataField);
    expect(fields).toContain('eventKey');
    expect(fields).toContain('entityType');
  });

  it('ruleColumns has 7 columns and includes an Actions column', () => {
    expect(ruleColumns).toHaveLength(7);
    expect(ruleColumns.some(c => c.caption === 'Actions')).toBe(true);
  });

  it('recipientColumns has 5 columns with caption on each', () => {
    expect(recipientColumns).toHaveLength(5);
    recipientColumns.forEach(col => {
      expect(col).toHaveProperty('caption');
    });
    // 4 data columns + 1 Actions column (no dataField)
    const dataFields = recipientColumns.filter(col => 'dataField' in col);
    expect(dataFields).toHaveLength(4);
  });

  it('templateColumns has 3 columns with correct dataFields', () => {
    expect(templateColumns).toHaveLength(3);
    const fields = templateColumns.map(c => c.dataField);
    expect(fields).toContain('eventKey');
    expect(fields).toContain('messageTemplate');
  });
});
