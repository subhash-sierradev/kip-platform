// Frontend mirror of backend FieldDataType enum.
// Defines supported data types for field mapping configurations.

export enum FieldDataType {
  STRING = 'STRING',
  INTEGER = 'INTEGER',
  LONG = 'LONG',
  DOUBLE = 'DOUBLE',
  BOOLEAN = 'BOOLEAN',
  DATE = 'DATE',
  DATETIME = 'DATETIME',
  ARRAY = 'ARRAY',
  OBJECT = 'OBJECT',
  JSON = 'JSON',
}

export interface DataTypeInfo {
  code: FieldDataType;
  description: string;
  validationPattern?: string;
}

export const DATA_TYPES: DataTypeInfo[] = [
  { code: FieldDataType.STRING, description: 'Text/String value' },
  { code: FieldDataType.INTEGER, description: 'Integer number', validationPattern: '^-?\\d+$' },
  { code: FieldDataType.LONG, description: 'Long integer number', validationPattern: '^-?\\d+$' },
  {
    code: FieldDataType.DOUBLE,
    description: 'Decimal number',
    validationPattern: '^-?\\d*\\.?\\d+$',
  },
  { code: FieldDataType.BOOLEAN, description: 'True/False value' },
  { code: FieldDataType.DATE, description: 'Date value (YYYY-MM-DD)' },
  { code: FieldDataType.DATETIME, description: 'Date and time value' },
  { code: FieldDataType.ARRAY, description: 'Array/List of values' },
  { code: FieldDataType.OBJECT, description: 'Complex object structure' },
  { code: FieldDataType.JSON, description: 'JSON formatted data' },
];

export function getDataTypeInfo(dataType: FieldDataType): DataTypeInfo | undefined {
  return DATA_TYPES.find(type => type.code === dataType);
}

export function getDataTypeDescription(dataType: FieldDataType): string {
  return getDataTypeInfo(dataType)?.description || dataType;
}
