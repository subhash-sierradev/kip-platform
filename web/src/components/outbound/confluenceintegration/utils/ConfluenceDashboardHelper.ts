import type { ConfluenceIntegrationSummaryResponse } from '@/api/models/ConfluenceIntegrationSummaryResponse';
import type { ActionMenuItem } from '@/components/common/ActionMenu.vue';

export function getIntegrationMenuItems(
  integration: ConfluenceIntegrationSummaryResponse
): ActionMenuItem[] {
  const items: ActionMenuItem[] = [
    {
      id: 'edit',
      label: 'Edit',
      iconType: 'svg',
      svgPath:
        'M3 17.25V21h3.75l11-11-3.75-3.75-11 11zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34a.996.996 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z',
    },
    {
      id: 'clone',
      label: 'Clone',
      iconType: 'svg',
      svgPath:
        'M16 1H4c-1.1 0-2 .9-2 2v12h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z',
    },
    {
      id: integration.isEnabled ? 'disable' : 'enable',
      label: integration.isEnabled ? 'Disable' : 'Enable',
      iconType: 'devextreme',
      icon: integration.isEnabled ? 'dx-icon-cursorprohibition' : 'dx-icon-video',
    },
    {
      id: 'delete',
      label: 'Delete',
      iconType: 'svg',
      svgPath: 'M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z',
    },
  ];

  return items;
}

export function createConfluenceDialogConfig() {
  return {
    enable: {
      title: 'Enable Confluence Integration',
      desc: 'Enabling this integration will allow scheduled or manual runs.',
      label: 'Enable',
    },
    disable: {
      title: 'Disable Confluence Integration',
      desc: 'Disabling this integration will prevent runs until re-enabled.',
      label: 'Disable',
    },
    delete: {
      title: 'Delete Confluence Integration',
      desc: 'Deleting this integration will remove it permanently. This action cannot be undone.',
      label: 'Delete',
    },
  };
}
