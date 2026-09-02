export interface AppConfig {
  apiBaseUrl: string;
  oidcAuthority: string;
  oidcClientId: string;
  oidcRedirectUri: string;
}

const defaults = {
  apiBaseUrl: 'http://localhost:8080',
  oidcAuthority: 'http://localhost:8180/realms/brokeros',
  oidcClientId: 'brokeros-risk-console',
} as const;

function requiredUrl(value: string, field: string): string {
  try {
    const url = new URL(value);
    if (url.protocol !== 'http:' && url.protocol !== 'https:') {
      throw new Error('unsupported protocol');
    }
    return url.toString().replace(/\/$/, '');
  } catch {
    throw new Error(`${field} must be an absolute HTTP(S) URL`);
  }
}

export function loadAppConfig(): AppConfig {
  const runtime = window.__BROKEROS_CONFIG__ ?? {};
  const apiBaseUrl = runtime.apiBaseUrl ?? import.meta.env.VITE_API_BASE_URL ?? defaults.apiBaseUrl;
  const oidcAuthority =
    runtime.oidcAuthority ?? import.meta.env.VITE_OIDC_AUTHORITY ?? defaults.oidcAuthority;
  const oidcClientId =
    runtime.oidcClientId ?? import.meta.env.VITE_OIDC_CLIENT_ID ?? defaults.oidcClientId;
  const oidcRedirectUri =
    runtime.oidcRedirectUri ?? import.meta.env.VITE_OIDC_REDIRECT_URI ?? window.location.origin;

  if (oidcClientId.trim().length === 0) {
    throw new Error('oidcClientId must not be blank');
  }

  return {
    apiBaseUrl: requiredUrl(apiBaseUrl, 'apiBaseUrl'),
    oidcAuthority: requiredUrl(oidcAuthority, 'oidcAuthority'),
    oidcClientId: oidcClientId.trim(),
    oidcRedirectUri: requiredUrl(oidcRedirectUri, 'oidcRedirectUri'),
  };
}
