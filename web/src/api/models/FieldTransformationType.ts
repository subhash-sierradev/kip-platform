/**
 * Field transformation types for field mapping
 * Matches backend com.integration.model.enums.FieldTransformationType
 */
export enum FieldTransformationType {
  PASSTHROUGH = 'PASSTHROUGH',
  UPPERCASE = 'UPPERCASE',
  LOWERCASE = 'LOWERCASE',
  STRING_TITLE_CASE = 'STRING_TITLE_CASE',
  TRIM = 'TRIM',
  TO_STRING = 'TO_STRING',
  TO_INTEGER = 'TO_INTEGER',
  TO_DOUBLE = 'TO_DOUBLE',
  TO_BOOLEAN = 'TO_BOOLEAN',
  TO_DATE = 'TO_DATE',
}
