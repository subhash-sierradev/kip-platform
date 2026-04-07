/**
 * Base interface for authentication credentials
 */
export interface AuthCredential {
  /**
   * Authentication type
   */
  authType: string;
}

/**
 * Basic authentication credentials
 */
export interface BasicAuthCredential extends AuthCredential {
  username: string;
  password: string;
}

/**
 * OAuth2 client credentials
 */
export interface OAuthClientCredential extends AuthCredential {
  clientId: string;
  clientSecret: string;
  tokenUrl: string;
  scope?: string;
}
