interface ColumnDefinition {
  dataField: string;
  caption: string;
  cellTemplate?: string;
}

export const siteConfigColumns: ColumnDefinition[] = [
  {
    dataField: 'id',
    caption: 'Key',
    cellTemplate: 'keyTemplate',
  },
  {
    dataField: 'value',
    caption: 'Value',
    cellTemplate: 'valueTemplate',
  },
  {
    dataField: 'type',
    caption: 'Type',
    cellTemplate: 'typeTemplate',
  },
  {
    dataField: 'description',
    caption: 'Description',
  },
  {
    dataField: 'lastModifiedDate',
    caption: 'Last Modified',
    cellTemplate: 'dateTemplate',
  },
];
