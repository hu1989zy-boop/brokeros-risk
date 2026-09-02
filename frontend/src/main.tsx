import 'antd/dist/reset.css';
import './app/styles.css';

import React from 'react';
import ReactDOM from 'react-dom/client';
import { AuthProvider } from 'react-oidc-context';

import { App } from './app/App';
import { createOidcConfig } from './core/auth/authConfig';
import { loadAppConfig } from './core/config/appConfig';

const config = loadAppConfig();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AuthProvider {...createOidcConfig(config)}>
      <App config={config} />
    </AuthProvider>
  </React.StrictMode>,
);
