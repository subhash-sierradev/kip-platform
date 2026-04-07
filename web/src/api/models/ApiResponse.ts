/**
 * Standard API response for operations like connection testing
 */
export interface ApiResponse {
  /**
   * HTTP status code
   */
  statusCode: number;

  /**
   * Whether the operation was successful
   */
  success: boolean;

  /**
   * Response message
   */
  message: string;
}
