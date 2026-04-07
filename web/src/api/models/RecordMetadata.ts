export interface RecordMetadata {
  documentId: string;
  title: string;
  locationId: string;
  documentCreatedAt: number | null;
  documentUpdatedAt: number | null;
  locationCreatedAt: number | null;
  locationUpdatedAt: number | null;
}

export interface FailedRecordMetadata extends RecordMetadata {
  errorMessage: string;
}
