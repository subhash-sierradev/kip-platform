/**
 * Common dashboard types for toolbar and pagination components
 */

/** Sort option configuration for dashboard toolbars */
export interface DashboardSortOption {
  /** Unique value for the sort field */
  value: string;
  /** Display label for the sort option */
  label: string;
}

/** View mode options for data display */
export type DashboardViewMode = 'grid' | 'list';

/** Base props interface for DashboardToolbar component */
export interface DashboardToolbarProps {
  /** Current search term */
  search: string;
  /** Current sort field value */
  sortBy: string;
  /** Available sort options for the dropdown */
  sortOptions: DashboardSortOption[];
  /** Current view mode (grid or list) */
  viewMode: DashboardViewMode;
  /** Current page size */
  pageSize: number;
  /** Current page number (1-based) */
  currentPage: number;
  /** Total number of items */
  totalCount: number;
  /** Available page size options */
  pageSizeOptions: number[];
  /** Placeholder text for search input */
  searchPlaceholder?: string;
  /** Text for create button */
  createButtonText?: string;
  /** Icon for create button */
  createButtonIcon?: string;
}

/** Events emitted by DashboardToolbar component */
export interface DashboardToolbarEmits {
  /** Search term updated */
  (e: 'update:search', value: string): void;
  /** Sort field updated */
  (e: 'update:sortBy', value: string): void;
  /** Page size updated */
  (e: 'update:pageSize', value: number): void;
  /** View mode changed */
  (e: 'setViewMode', mode: DashboardViewMode): void;
  /** Navigate to previous page */
  (e: 'prevPage'): void;
  /** Navigate to next page */
  (e: 'nextPage'): void;
  /** Create new item button clicked */
  (e: 'create'): void;
}

/** Pagination state for consistent tracking across components */
export interface PaginationState {
  /** Current page (1-based) */
  currentPage: number;
  /** Items per page */
  pageSize: number;
  /** Total number of items */
  totalCount: number;
  /** Available page size options */
  pageSizeOptions: number[];
}

/** Computed pagination properties */
export interface PaginationComputed {
  /** Total number of pages */
  totalPages: number;
  /** Display current page (0 if no items) */
  displayCurrentPage: number;
  /** Starting item number for current page */
  pageStart: number;
  /** Ending item number for current page */
  pageEnd: number;
}
