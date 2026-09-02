import { describe, expect, it } from 'vitest';

import { createOidcConfig } from '../src/core/auth/authConfig';
import type { AppConfig } from '../src/core/config/appConfig';

const config: AppConfig = {
  apiBaseUrl: 'http://localhost:8080',
  oidcAuthority: 'http://localhost:8180/realms/brokeros',
  oidcClientId: 'brokeros-risk-console',
  oidcRedirectUri: 'http://localhost:4173',
};

describe('OIDC configuration', () => {
  it('uses Authorization Code, PKCE-capable OIDC settings and no password grant', () => {
    const oidc = createOidcConfig(config);
    expect(oidc.response_type).toBe('code');
    expect(oidc.authority).toBe(config.oidcAuthority);
    expect(oidc.client_id).toBe(config.oidcClientId);
    expect(oidc.automaticSilentRenew).toBe(true);
    expect(oidc).not.toHaveProperty('client_secret');
  });

  it('keeps the signed-in user and access token out of session storage', async () => {
    const oidc = createOidcConfig(config);
    sessionStorage.clear();
    await oidc.userStore?.set('user', 'opaque-signed-in-user-state');

    expect(await oidc.userStore?.get('user')).toBe('opaque-signed-in-user-state');
    expect(sessionStorage.length).toBe(0);
  });
});
