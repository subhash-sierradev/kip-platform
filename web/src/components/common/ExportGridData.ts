// Common export utilities for DevExtreme DataGrids and pages
// Provides CSV export helpers that can be reused across multiple pages

// Minimal types to avoid `any` while staying flexible
export interface DataSourceLike<T = unknown> {
  items?: () => T[];
  load?: (options?: {
    filter?: unknown;
    requireTotalCount?: boolean;
  }) => Promise<{ data?: T[] } | T[]>;
  filter?: () => unknown;
}

export interface GridLikeInstance<T = unknown> {
  getDataSource?: () => DataSourceLike<T> | undefined;
  option?: (path: string, value?: unknown) => unknown;
}

export interface GridComponentRef {
  instance?: GridLikeInstance;
}

export interface CsvRow {
  [key: string]: string | number | Date | undefined;
}

export const getUSTimestamp = (): string => {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  const year = now.getFullYear();
  const hours = String(now.getHours()).padStart(2, '0');
  const minutes = String(now.getMinutes()).padStart(2, '0');
  const seconds = String(now.getSeconds()).padStart(2, '0');
  return `${month}-${day}-${year}_${hours}-${minutes}-${seconds}`;
};

// Basic CSV converter: accepts array of objects and headers order
export const convertToCSV = <T = unknown>(
  rows: T[],
  headers: string[],
  pickFields: (row: T) => (string | number | Date | undefined)[]
): string => {
  if (!rows || rows.length === 0) return '';
  const csvRows: string[] = [headers.join(',')];
  rows.forEach(row => {
    const values = pickFields(row).map(v => {
      const str = v === undefined || v === null ? '' : String(v);
      return `"${str.replace(/"/g, '""')}"`;
    });
    csvRows.push(values.join(','));
  });
  return csvRows.join('\n');
};

export const downloadFile = (
  content: string,
  filename: string,
  mimeType = 'text/csv;charset=utf-8;'
): void => {
  const blob = new Blob([content], { type: mimeType });
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);
  link.setAttribute('href', url);
  link.setAttribute('download', filename);
  link.style.visibility = 'hidden';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

// Helper to prefer grid's filtered/sorted items when available
// Internal helpers to reduce complexity in resolveGridItems
const hasFilter = <T>(ds: DataSourceLike<T>): boolean =>
  typeof ds?.filter === 'function' && !!ds.filter();

const loadAll = async <T>(ds: DataSourceLike<T>, filter?: unknown): Promise<T[]> => {
  if (typeof ds?.load !== 'function') return [];
  try {
    const args = filter ? { filter, requireTotalCount: false } : { requireTotalCount: false };
    const result = await ds.load(args);
    const data = Array.isArray((result as { data?: T[] })?.data)
      ? (result as { data?: T[] }).data
      : Array.isArray(result)
        ? (result as T[])
        : [];
    return Array.isArray(data) ? data : [];
  } catch {
    return [];
  }
};

const dsItems = <T>(ds: DataSourceLike<T>): T[] => {
  const fn = typeof ds?.items === 'function' ? ds.items.bind(ds) : undefined;
  return fn ? fn() || [] : [];
};

const expandPageSizeAndGetItems = <T>(
  gridInst: GridLikeInstance<T>,
  ds: DataSourceLike<T>
): T[] => {
  try {
    const prevSize = gridInst?.option?.('paging.pageSize');
    if (typeof gridInst?.option === 'function') {
      gridInst.option('paging.pageSize', Number.MAX_SAFE_INTEGER);
    }
    const items = dsItems(ds);
    if (typeof gridInst?.option === 'function') {
      gridInst.option('paging.pageSize', prevSize);
    }
    return items;
  } catch {
    return [];
  }
};

// Resolve items when a filter is applied
const resolveFiltered = async <T>(ds: DataSourceLike<T>): Promise<T[]> => {
  const fromLoad = await loadAll(ds, ds.filter?.());
  if (fromLoad.length) return fromLoad;
  const currentPage = dsItems(ds);
  if (currentPage.length) return currentPage;
  return [];
};

// Resolve items when no filter is applied
const resolveUnfiltered = async <T>(
  gridInst: GridLikeInstance<T>,
  ds: DataSourceLike<T>,
  fallbackData?: T[]
): Promise<T[]> => {
  const all = await loadAll(ds);
  if (all.length) return all;
  const expanded = expandPageSizeAndGetItems(gridInst, ds);
  if (expanded.length) return expanded;
  if (Array.isArray(fallbackData)) return fallbackData;
  const current = dsItems(ds);
  if (current.length) return current;
  return [];
};

export const resolveGridItems = async <T = unknown>(
  gridRef?: GridComponentRef,
  fallbackData?: T[]
): Promise<T[]> => {
  const gridComponent = gridRef;
  const instGetter = gridComponent?.instance?.getDataSource;
  if (typeof instGetter !== 'function') {
    return Array.isArray(fallbackData) ? fallbackData : [];
  }

  const ds = instGetter.call(gridComponent?.instance) as DataSourceLike<T>;
  const filtered = hasFilter(ds);

  const gridInst = (gridComponent?.instance as GridLikeInstance<T>) || ({} as GridLikeInstance<T>);
  const items = filtered
    ? await resolveFiltered(ds)
    : await resolveUnfiltered(gridInst, ds, fallbackData);

  return items.length ? items : Array.isArray(fallbackData) ? fallbackData : [];
};

// High-level CSV export: collects items, builds CSV, and downloads
export const exportGridData = async <T = unknown>(params: {
  gridRef?: GridComponentRef;
  fallbackData?: T[];
  headers: string[];
  pickFields: (row: T) => (string | number | Date | undefined)[];
  filenamePrefix?: string;
}): Promise<void> => {
  const { gridRef, fallbackData, headers, pickFields, filenamePrefix } = params;
  const items = await resolveGridItems(gridRef, fallbackData);
  if (!items.length) {
    console.warn('No data available for export');
    return;
  }
  const csvContent = convertToCSV(items, headers, pickFields);
  const timestamp = getUSTimestamp();
  const filename = `${filenamePrefix || 'grid-export'}_${timestamp}.csv`;
  downloadFile(csvContent, filename);
};
