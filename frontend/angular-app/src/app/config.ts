export interface WorkRhRuntimeConfig {
  apiBaseUrl?: string;
  frontendBaseUrl?: string;
  defaultTenantId?: string;
  showDemoHints?: boolean;
  defaultLoginEmail?: string;
  defaultLoginPassword?: string;
}

declare global {
  interface Window {
    __WORKRH_CONFIG__?: WorkRhRuntimeConfig;
  }
}

const runtimeConfig: WorkRhRuntimeConfig = typeof window !== 'undefined'
  ? window.__WORKRH_CONFIG__ ?? {}
  : {};

function normalizeUrl(value?: string): string | null {
  const trimmedValue = value?.trim();
  if (!trimmedValue) {
    return null;
  }
  return trimmedValue.replace(/\/+$/, '');
}

function readString(value?: string): string {
  return typeof value === 'string' ? value.trim() : '';
}

const browserOrigin = typeof window !== 'undefined'
  ? window.location.origin.replace(/\/+$/, '')
  : null;

export const API_BASE_URL = normalizeUrl(runtimeConfig.apiBaseUrl)
  ?? browserOrigin
  ?? 'http://localhost:9080';

export const FRONTEND_BASE_URL = normalizeUrl(runtimeConfig.frontendBaseUrl)
  ?? browserOrigin
  ?? 'http://localhost:4200';

export const DEFAULT_TENANT_ID = readString(runtimeConfig.defaultTenantId);
export const SHOW_DEMO_HINTS = runtimeConfig.showDemoHints === true;
export const DEFAULT_LOGIN_EMAIL = readString(runtimeConfig.defaultLoginEmail);
export const DEFAULT_LOGIN_PASSWORD = typeof runtimeConfig.defaultLoginPassword === 'string'
  ? runtimeConfig.defaultLoginPassword
  : '';
