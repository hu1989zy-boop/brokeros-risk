import { afterEach, describe, expect, it } from 'vitest';

import { loadAppConfig } from '../src/core/config/appConfig';

afterEach(() => {
  delete window.__BROKEROS_CONFIG__;
});

describe('React runtime configuration', () => {
  it('uses the delivered local endpoints by default', () => {
    expect(loadAppConfig()).toMatchObject({
      apiBaseUrl: 'http://localhost:8080',
      oidcAuthority: 'http://localhost:8180/realms/brokeros',
      oidcClientId: 'brokeros-risk-console',
    });
  });

  it('accepts a non-secret deployment runtime override', () => {
    window.__BROKEROS_CONFIG__ = {
      apiBaseUrl: 'https://risk-api.example.test/',
      oidcAuthority: 'https://identity.example.test/realms/brokeros/',
      oidcClientId: 'deployment-console',
      oidcRedirectUri: 'https://risk-console.example.test/',
    };

    expect(loadAppConfig()).toEqual({
      apiBaseUrl: 'https://risk-api.example.test',
      oidcAuthority: 'https://identity.example.test/realms/brokeros',
      oidcClientId: 'deployment-console',
      oidcRedirectUri: 'https://risk-console.example.test',
    });
  });

  it('fails startup on an unsafe or malformed URL', () => {
    window.__BROKEROS_CONFIG__ = { apiBaseUrl: 'javascript:alert(1)' };
    expect(() => loadAppConfig()).toThrow('apiBaseUrl must be an absolute HTTP(S) URL');
  });
});
