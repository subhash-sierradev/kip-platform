/**
 * Field data types for field mapping
 * Matches backend com.integration.model.enums.FieldDataType
 */
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

/**
 * Data Type metadata for display purposes
 */
export const FieldDataTypeMetadata = {
  [FieldDataType.STRING]: {
    displayName: 'String',
    description: 'Text/String value',
    validationPattern: undefined,
  },
  [FieldDataType.INTEGER]: {
    displayName: 'Integer',
    description: 'Integer number',
    validationPattern: '^-?\\d+$',
  },
  [FieldDataType.LONG]: {
    displayName: 'Long',
    description: 'Long integer number',
    validationPattern: '^-?\\d+$',
  },
  [FieldDataType.DOUBLE]: {
    displayName: 'Double',
    description: 'Decimal number',
    validationPattern: '^-?\\d*\\.?\\d+$',
  },
  [FieldDataType.BOOLEAN]: {
    displayName: 'Boolean',
    description: 'True/False value',
    validationPattern: undefined,
  },
  [FieldDataType.DATE]: {
    displayName: 'Date',
    description: 'Date value (YYYY-MM-DD)',
    validationPattern: undefined,
  },
  [FieldDataType.DATETIME]: {
    displayName: 'DateTime',
    description: 'Date and time value',
    validationPattern: undefined,
  },
  [FieldDataType.ARRAY]: {
    displayName: 'Array',
    description: 'Array/List of values',
    validationPattern: undefined,
  },
  [FieldDataType.OBJECT]: {
    displayName: 'Object',
    description: 'Complex object structure',
    validationPattern: undefined,
  },
  [FieldDataType.JSON]: {
    displayName: 'JSON',
    description: 'JSON formatted data',
    validationPattern: undefined,
  },
} as const;

/**
 * Get display name for a field data type
 */
export function getFieldDataTypeDisplayName(dataType: FieldDataType): string {
  return FieldDataTypeMetadata[dataType]?.displayName || dataType;
}

/**
 * Get description for a field data type
 */
export function getFieldDataTypeDescription(dataType: FieldDataType): string {
  return FieldDataTypeMetadata[dataType]?.description || dataType;
}

/**
 * Get validation pattern for a field data type
 */
export function getFieldDataTypeValidationPattern(dataType: FieldDataType): string | undefined {
  return FieldDataTypeMetadata[dataType]?.validationPattern;
}
