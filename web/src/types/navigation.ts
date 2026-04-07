// Navigation types for the application

export interface NavSection {
  label: string;
  route?: string;
  icon?: string;
  children?: NavItem[];
}

export interface NavItem {
  label: string;
  route: string;
  icon?: string;
  badge?: string | number;
  children?: NavItem[];
}

// Utility type for breadcrumb navigation
export interface BreadcrumbItem {
  text: string;
  url?: string;
}

// Additional types used in AppShell
export interface NavLeaf {
  label: string;
  route?: string;
  icon?: string;
}
