import type { AuthProviderNoUserManagerProps } from 'react-oidc-context';
import { WebStorageStateStore } from 'oidc-client-ts';

import type { AppConfig } from '../config/appConfig';

class MemoryStorage implements Storage {
  private readonly values = new Map<string, string>();

  get length(): number {
    return this.values.size;
  }

  clear(): void {
    this.values.clear();
  }

  getItem(key: string): string | null {
    return this.values.get(key) ?? null;
  }

  key(index: number): string | null {
    return [...this.values.keys()][index] ?? null;
  }

  removeItem(key: string): void {
    this.values.delete(key);
  }

  setItem(key: string, value: string): void {
    this.values.set(key, value);
  }
}

export function createOidcConfig(config: AppConfig): AuthProviderNoUserManagerProps {
  return {
    authority: config.oidcAuthority,
    client_id: config.oidcClientId,
    redirect_uri: config.oidcRedirectUri,
    post_logout_redirect_uri: config.oidcRedirectUri,
    response_type: 'code',
    scope: 'openid profile',
    automaticSilentRenew: true,
    loadUserInfo: false,
    monitorSession: false,
    userStore: new WebStorageStateStore({ store: new MemoryStorage() }),
    stateStore: new WebStorageStateStore({ store: window.sessionStorage }),
    onSigninCallback: () => {
      window.history.replaceState({}, document.title, window.location.pathname);
    },
  };
}
