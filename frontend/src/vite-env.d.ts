/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL: string;
  readonly VITE_CREATOR_STUDIO_DEV_NO_LOGIN?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
