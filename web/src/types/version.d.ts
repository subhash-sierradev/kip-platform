// Global version constants injected by Vite
declare const __APP_VERSION__: string;
declare const __APP_BUILD_NUMBER__: string;
declare const __APP_BUILD_DATE__: string;

// Environment variables
declare interface ImportMetaEnv {
  readonly VITE_APP_VERSION: string;
  readonly VITE_APP_BUILD_NUMBER: string;
  readonly VITE_APP_BUILD_DATE: string;
}

declare interface ImportMeta {
  readonly env: ImportMetaEnv;
}
