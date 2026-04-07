export interface ArcGISFeatureField {
  name: string;
  type: string;
  alias?: string | null;
  sqlType?: string | null;
  nullable?: boolean | null;
  editable?: boolean | null;
  domain?: unknown | null;
  defaultValue?: unknown | null;
  length?: number | null;
  description?: string | null;
  precision?: number | null;
}
