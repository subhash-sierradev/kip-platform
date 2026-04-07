/**
 * Response DTO for source field mapping data
 * Represents field metadata from source system
 */
export interface KwDocField {
  /** Sequential ID starting at 1 */
  id: number;

  /** Field name from source system */
  fieldName: string;

  /** Field type (data type) */
  fieldType: string;

  /** Whether the field is mandatory/required */
  isMandatory?: boolean;
}
