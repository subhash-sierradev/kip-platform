import type { ConnectionStatus } from './IntegrationConnection';

export interface ServiceConnectionResponse {
  id: string;
  statusCode: number;
  success: boolean;
  message: string;
  lastConnectionStatus?: ConnectionStatus;
  lastConnectionTest?: string; // ISO 8601 formatted date-time string representation of the last connection test timestamp
}
