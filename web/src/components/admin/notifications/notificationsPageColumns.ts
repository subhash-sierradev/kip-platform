export const notificationsPageTabs = [
  { id: 'events', label: 'Events', iconClass: 'dx-icon dx-icon-description' },
  { id: 'templates', label: 'Message Templates', iconClass: 'dx-icon dx-icon-file' },
  { id: 'rules', label: 'Notification Rules', iconClass: 'dx-icon dx-icon-bell' },
  { id: 'recipients', label: 'Recipients', iconClass: 'dx-icon dx-icon-group' },
];

export const eventColumns = [
  { dataField: 'eventKey', caption: 'Event Key', width: 242, template: 'eventKeyTemplate' },
  { dataField: 'entityType', caption: 'Entity Type', width: 180 },
  { dataField: 'displayName', caption: 'Display Name', width: 198 },
  { dataField: 'description', caption: 'Description', template: 'descriptionTemplate' },
  {
    dataField: 'notifyInitiator',
    caption: 'Notify Actor',
    width: 140,
    template: 'notifyInitiatorTemplate',
  },
];

export const ruleColumns = [
  { dataField: 'eventKey', caption: 'Event Key' },
  { dataField: 'entityType', caption: 'Entity Type', width: 200, allowFiltering: true },
  { dataField: 'severity', caption: 'Alert Type', width: 120, template: 'severityTemplate' },
  { dataField: 'enabled', caption: 'Status', width: 110, template: 'enabledTemplate' },
  {
    dataField: 'lastModifiedBy',
    caption: 'Updated By',
    width: 200,
    template: 'ruleUpdatedByTemplate',
  },
  { dataField: 'lastModifiedDate', caption: 'Last Updated', width: 200, template: 'dateTemplate' },
  {
    caption: 'Actions',
    width: 130,
    template: 'actionTemplate',
    allowSorting: false,
    allowFiltering: false,
  },
];

export const recipientColumns = [
  { dataField: 'eventKey', caption: 'Event Key', width: 280 },
  { dataField: 'recipientType', caption: 'Recipient Type', template: 'recipientTypeTemplate' },
  {
    dataField: 'lastModifiedBy',
    caption: 'Updated By',
    width: 160,
    template: 'recipientUpdatedByTemplate',
  },
  {
    dataField: 'lastModifiedDate',
    caption: 'Last Updated',
    width: 200,
    template: 'recipientDateTemplate',
  },
  {
    caption: 'Actions',
    width: 100,
    template: 'recipientActionTemplate',
    allowSorting: false,
    allowFiltering: false,
  },
];

export const templateColumns = [
  { dataField: 'eventKey', caption: 'Event Key', width: 200 },
  { dataField: 'titleTemplate', caption: 'Title Template', width: 250 },
  { dataField: 'messageTemplate', caption: 'Message Template' },
];
