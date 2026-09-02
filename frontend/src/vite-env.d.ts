/// <reference types="vite/client" />

interface BrokerOsRuntimeConfig {
  apiBaseUrl?: string;
  oidcAuthority?: string;
  oidcClientId?: string;
  oidcRedirectUri?: string;
}

interface Window {
  __BROKEROS_CONFIG__?: BrokerOsRuntimeConfig;
}
