// Sidebar menu descriptions for tooltips
export interface SidebarMenuDescription {
  title: string;
  description: string;
}

export const sidebarMenuDescriptions: Record<string, SidebarMenuDescription> = {
  inbound: {
    title: 'Inbound',
    description: 'Manage inbound integrations, data sources, and imports',
  },
  outbound: {
    title: 'Outbound',
    description: 'Configure outbound connectors, webhooks, and exports',
  },
  admin: {
    title: 'Admin',
    description: 'Platform administration, audit logs, and advanced settings',
  },
  graph: {
    title: 'Graph',
    description: 'Perform graph analytics, explore OSINT integrations, and connect the dots',
  },
  // Add more as needed
};
