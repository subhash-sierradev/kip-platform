import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  convertToCSV,
  type DataSourceLike,
  downloadFile,
  exportGridData,
  getUSTimestamp,
  type GridLikeInstance,
  resolveGridItems,
} from '@/components/common/ExportGridData';

// Helper to validate timestamp format MM-DD-YYYY_HH-MM-SS
const TIMESTAMP_REGEX = /^\d{2}-\d{2}-\d{4}_\d{2}-\d{2}-\d{2}$/;

beforeEach(() => {
  vi.restoreAllMocks();
});

describe('getUSTimestamp', () => {
  it('returns a string in MM-DD-YYYY_HH-MM-SS format', () => {
    const ts = getUSTimestamp();
    expect(typeof ts).toBe('string');
    expect(TIMESTAMP_REGEX.test(ts)).toBe(true);
  });
});

describe('convertToCSV', () => {
  it('returns empty string when rows empty', () => {
    const csv = convertToCSV([], ['a', 'b'], () => []);
    expect(csv).toBe('');
  });

  it('creates CSV with headers and escaped values', () => {
    interface Row {
      a?: string;
      b?: number;
      c?: string;
    }
    const rows: Row[] = [
      { a: 'hello', b: 42, c: 'x"y' },
      { a: undefined, b: 0, c: '' },
    ];
    const headers = ['A', 'B', 'C'];
    const csv = convertToCSV<Row>(rows, headers, r => [r.a, r.b, r.c]);
    const lines = csv.split('\n');
    expect(lines[0]).toBe('A,B,C');
    // Values quoted, quotes doubled
    expect(lines[1]).toBe('"hello","42","x""y"');
    // undefined -> empty string, still quoted
    expect(lines[2]).toBe('"","0",""');
  });

  it('handles Date objects and null values', () => {
    interface Row {
      date?: Date;
      value?: number | null;
    }
    const testDate = new Date('2024-01-15T10:30:00Z');
    const rows: Row[] = [
      { date: testDate, value: 100 },
      { date: undefined, value: null },
    ];
    const csv = convertToCSV<Row>(rows, ['Date', 'Value'], r => [r.date, r.value ?? '']);
    const lines = csv.split('\n');
    expect(lines[0]).toBe('Date,Value');
    // Date converts to string, null converts to empty
    expect(lines[1]).toContain('"');
    expect(lines[2]).toBe('"",""');
  });
});

describe('downloadFile', () => {
  const originalCreateObjectURL = URL.createObjectURL;
  const originalRevokeObjectURL = URL.revokeObjectURL;

  beforeEach(() => {
    // Mock URL API
    URL.createObjectURL = vi.fn(() => 'blob:mock-url');
    URL.revokeObjectURL = vi.fn();
  });

  afterEach(() => {
    URL.createObjectURL = originalCreateObjectURL;
    URL.revokeObjectURL = originalRevokeObjectURL;
    document.body.innerHTML = '';
  });

  it('creates a link, triggers click, and cleans up', () => {
    const clickSpy = vi.fn();
    // Spy on document.createElement to inject click spy
    const createElSpy = vi.spyOn(document, 'createElement');
    createElSpy.mockImplementation((tagName: any) => {
      const el = document.createElementNS('http://www.w3.org/1999/xhtml', tagName);
      // attach click spy
      Object.defineProperty(el, 'click', { value: clickSpy });
      return el as unknown as HTMLElement;
    });

    downloadFile('a,b\n1,2', 'test.csv');

    expect(URL.createObjectURL).toHaveBeenCalledTimes(1);
    expect(clickSpy).toHaveBeenCalledTimes(1);
    expect(document.querySelector('a[download="test.csv"]')).toBeNull();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-url');
  });

  it('uses custom mimeType when provided', () => {
    const clickSpy = vi.fn();
    const createElSpy = vi.spyOn(document, 'createElement');
    createElSpy.mockImplementation((tagName: any) => {
      const el = document.createElementNS('http://www.w3.org/1999/xhtml', tagName);
      Object.defineProperty(el, 'click', { value: clickSpy });
      return el as unknown as HTMLElement;
    });

    downloadFile('{"key":"value"}', 'data.json', 'application/json');

    // Verify blob was created (can't directly inspect mimeType without more mocking)
    expect(URL.createObjectURL).toHaveBeenCalledTimes(1);
    expect(clickSpy).toHaveBeenCalledTimes(1);
  });
});

describe('resolveGridItems', () => {
  interface Row {
    id: number;
  }

  const makeDataSource = (opts: {
    items?: Row[];
    filter?: unknown;
    loadResult?: { data?: Row[] } | Row[];
    loadThrows?: boolean;
  }): DataSourceLike<Row> => {
    return {
      items: opts.items ? () => opts.items! : undefined,
      filter:
        opts.filter !== undefined
          ? (
              () => () =>
                opts.filter
            )()
          : (undefined as any),
      load: (_args?: any) => {
        if (opts.loadThrows) return Promise.reject(new Error('load error'));
        return Promise.resolve(opts.loadResult ?? { data: opts.items ?? [] });
      },
    };
  };

  const makeGridInst = (): GridLikeInstance<Row> => ({
    option: vi.fn(),
    getDataSource: vi.fn(),
  });

  it('returns fallback when no grid instance', async () => {
    const res = await resolveGridItems<Row>(undefined, [{ id: 9 }]);
    expect(res).toEqual([{ id: 9 }]);
  });

  it('prefers filtered loadAll when filter is set', async () => {
    const ds = makeDataSource({ filter: { x: 1 }, loadResult: { data: [{ id: 1 }] } });
    const gridInst = makeGridInst();
    gridInst.getDataSource = vi.fn(() => ds);
    const res = await resolveGridItems<Row>({ instance: gridInst });
    expect(res).toEqual([{ id: 1 }]);
  });

  it('falls back to current items when filtered load fails', async () => {
    const ds = makeDataSource({ filter: { x: 1 }, loadThrows: true, items: [{ id: 2 }] });
    const gridInst = makeGridInst();
    gridInst.getDataSource = vi.fn(() => ds);
    const res = await resolveGridItems<Row>({ instance: gridInst });
    expect(res).toEqual([{ id: 2 }]);
  });

  it('returns empty array when filtered load fails and no items', async () => {
    const ds = makeDataSource({ filter: { x: 1 }, loadThrows: true, items: [] });
    const gridInst = makeGridInst();
    gridInst.getDataSource = vi.fn(() => ds);
    const res = await resolveGridItems<Row>({ instance: gridInst });
    expect(res).toEqual([]);
  });

  it('with no filter, tries loadAll, paging expand, fallbackData, then items', async () => {
    const ds = makeDataSource({ items: [{ id: 5 }] });
    const gridInst = makeGridInst();
    gridInst.getDataSource = vi.fn(() => ds);

    // First, loadAll returns empty -> try expand -> fallback -> items
    // Simulate option changing without throwing
    (gridInst.option as any) = vi.fn();

    const res = await resolveGridItems<Row>({ instance: gridInst }, [{ id: 7 }]);
    // loadAll default uses items -> returns items via { data: items }
    expect(res).toEqual([{ id: 5 }]);
  });

  it('uses fallbackData when loadAll and items are empty', async () => {
    const ds = makeDataSource({ items: [], loadResult: { data: [] } });
    const gridInst = makeGridInst();
    gridInst.getDataSource = vi.fn(() => ds);
    gridInst.option = vi.fn(); // Prevent errors during expand

    const fallback = [{ id: 99 }];
    const res = await resolveGridItems<Row>({ instance: gridInst }, fallback);
    expect(res).toEqual(fallback);
  });

  it('handles expandPageSizeAndGetItems when option throws', async () => {
    const ds = makeDataSource({ items: [], loadResult: { data: [] } });
    const gridInst = makeGridInst();
    gridInst.getDataSource = vi.fn(() => ds);
    // Make option throw to test error handling
    gridInst.option = vi.fn(() => {
      throw new Error('option error');
    });

    const fallback = [{ id: 88 }];
    const res = await resolveGridItems<Row>({ instance: gridInst }, fallback);
    // Should fall back to fallbackData when expand fails
    expect(res).toEqual(fallback);
  });

  it('handles load returning array directly instead of object', async () => {
    const ds = makeDataSource({ loadResult: [{ id: 42 }] });
    const gridInst = makeGridInst();
    gridInst.getDataSource = vi.fn(() => ds);

    const res = await resolveGridItems<Row>({ instance: gridInst });
    expect(res).toEqual([{ id: 42 }]);
  });

  it('returns empty when dataSource has no load function', async () => {
    const ds: DataSourceLike<Row> = {
      items: () => [],
      // No load function
    };
    const gridInst = makeGridInst();
    gridInst.getDataSource = vi.fn(() => ds);

    const res = await resolveGridItems<Row>({ instance: gridInst });
    expect(res).toEqual([]);
  });
});

describe('exportGridData', () => {
  it('warns and does nothing when no items are available', async () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation((_msg?: unknown) => {
      return undefined;
    });
    const gridInst: GridLikeInstance = {
      getDataSource: vi.fn(() => ({
        items: () => [],
        load: () => Promise.resolve({ data: [] }),
      })),
      option: vi.fn(),
    };
    await exportGridData({
      gridRef: { instance: gridInst },
      headers: ['H'],
      pickFields: () => ['x'],
      filenamePrefix: 'test',
    });
    expect(warnSpy).toHaveBeenCalledWith('No data available for export');
  });

  it('builds CSV and triggers download when items exist', async () => {
    const downloadSpy = vi.spyOn(document, 'createElement');
    downloadSpy.mockImplementation((tagName: any) => {
      const el = document.createElementNS('http://www.w3.org/1999/xhtml', tagName);
      Object.defineProperty(el, 'click', { value: vi.fn() });
      return el as unknown as HTMLElement;
    });
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:mock');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation((_url: string) => {
      return undefined;
    });

    const gridInst: GridLikeInstance<{ a: string; b: number }> = {
      getDataSource: vi.fn(() => ({
        items: () => [{ a: 'foo', b: 1 }],
      })),
      option: vi.fn(),
    };

    await exportGridData<{ a: string; b: number }>({
      gridRef: { instance: gridInst },
      headers: ['A', 'B'],
      pickFields: (r: { a: string; b: number }) => [r.a, r.b],
      filenamePrefix: 'data',
    });

    const link = document.querySelector('a');
    expect(link).toBeNull();
    // Verify a blob URL was created
    expect(URL.createObjectURL).toHaveBeenCalledTimes(1);
    // Very basic CSV content check via blob creation argument captured indirectly
    // We can't read back the Blob easily here, but headers were added
  });
});
