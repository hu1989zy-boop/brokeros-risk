import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Alert, Button, Card, ConfigProvider, Flex, Layout, Space, Spin, Typography } from 'antd';
import {
  lazy,
  Suspense,
  useEffect,
  useMemo,
  useRef,
  useState,
  type PropsWithChildren,
} from 'react';
import { hasAuthParams, useAuth } from 'react-oidc-context';
import {
  BrowserRouter,
  Navigate,
  Outlet,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from 'react-router-dom';

import { ApiClient, type AuthSession } from '../core/api/apiClient';
import type { AppConfig } from '../core/config/appConfig';
import { HttpReferenceListRepository } from '../features/riskcase/api/referenceList';
import { HttpReferencePreviewRepository } from '../features/riskcase/api/referencePreview';
import { HttpRiskCaseRepository } from '../features/riskcase/api/riskCaseRepository';
import { ReferenceListRepositoryProvider } from '../features/riskcase/model/referenceListContext';
import { ReferencePreviewRepositoryProvider } from '../features/riskcase/model/referencePreviewContext';
import { RiskCaseRepositoryProvider } from '../features/riskcase/model/riskCaseContext';

const RiskCaseListPage = lazy(async () => {
  const module = await import('../features/riskcase/ui/RiskCaseListPage');
  return { default: module.RiskCaseListPage };
});
const RiskCaseDetailPage = lazy(async () => {
  const module = await import('../features/riskcase/ui/RiskCaseDetailPage');
  return { default: module.RiskCaseDetailPage };
});

export function App({ config }: { config: AppConfig }) {
  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#3158d4',
          borderRadius: 8,
          fontFamily: 'Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, sans-serif',
        },
      }}
    >
      <RuntimeProviders config={config}>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route element={<ProtectedRoute />}>
              <Route element={<AppShell />}>
                <Route path="/cases" element={<LazyPage><RiskCaseListPage /></LazyPage>} />
                <Route path="/cases/:caseNumber" element={<LazyPage><RiskCaseDetailPage /></LazyPage>} />
              </Route>
            </Route>
            <Route path="*" element={<Navigate to="/cases" replace />} />
          </Routes>
        </BrowserRouter>
      </RuntimeProviders>
    </ConfigProvider>
  );
}

function LazyPage({ children }: PropsWithChildren) {
  return <Suspense fallback={<FullPageSpinner label="Loading console module" />}>{children}</Suspense>;
}

function RuntimeProviders({ config, children }: PropsWithChildren<{ config: AppConfig }>) {
  const auth = useAuth();
  const authRef = useRef(auth);
  authRef.current = auth;
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { staleTime: 10_000, retry: false },
          mutations: { retry: false },
        },
      }),
  );
  const authenticatedSubject = auth.user?.profile.sub ?? null;
  const priorSubject = useRef(authenticatedSubject);
  useEffect(() => {
    if (priorSubject.current !== authenticatedSubject) {
      queryClient.clear();
      priorSubject.current = authenticatedSubject;
    }
  }, [authenticatedSubject, queryClient]);
  const repositories = useMemo(() => {
    const session: AuthSession = {
      getAccessToken: () => authRef.current.user?.access_token ?? null,
      refreshAccessToken: async () => {
        try {
          const user = await authRef.current.signinSilent();
          return user?.access_token ?? null;
        } catch {
          return null;
        }
      },
      authenticationRequired: () => {
        queryClient.clear();
        void authRef.current.removeUser();
      },
    };
    const client = new ApiClient(config.apiBaseUrl, session);
    return {
      riskCases: new HttpRiskCaseRepository(client),
      referenceLists: new HttpReferenceListRepository(client),
      referencePreviews: new HttpReferencePreviewRepository(client),
    };
  }, [config.apiBaseUrl, queryClient]);

  // While the OIDC redirect callback is being processed (auth params still in the
  // URL) or the session is loading, do NOT mount the router. Otherwise the
  // catch-all route redirect strips the `?code`/`?state` from the URL before
  // react-oidc-context can exchange the authorization code, and login silently
  // fails back to the sign-in page.
  if (hasAuthParams() || auth.isLoading || auth.activeNavigator) {
    return <FullPageSpinner label="Completing sign-in" />;
  }

  return (
    <QueryClientProvider client={queryClient}>
      <RiskCaseRepositoryProvider repository={repositories.riskCases}>
        <ReferenceListRepositoryProvider repository={repositories.referenceLists}>
          <ReferencePreviewRepositoryProvider repository={repositories.referencePreviews}>
            {children}
          </ReferencePreviewRepositoryProvider>
        </ReferenceListRepositoryProvider>
      </RiskCaseRepositoryProvider>
    </QueryClientProvider>
  );
}

function ProtectedRoute() {
  const auth = useAuth();
  const location = useLocation();
  if (auth.isLoading || auth.activeNavigator) {
    return <FullPageSpinner label="Restoring your secure session" />;
  }
  if (auth.error) {
    return (
      <FullPageCard>
        <Alert
          type="error"
          showIcon
          message="Authentication failed"
          description="The identity provider could not restore your session. Sign in again."
          action={<Button onClick={() => void auth.signinRedirect()}>Sign in</Button>}
        />
      </FullPageCard>
    );
  }
  if (!auth.isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <Outlet />;
}

function LoginPage() {
  const auth = useAuth();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/cases';
  if (auth.isAuthenticated) {
    return <Navigate to={from} replace />;
  }
  return (
    <FullPageCard>
      <Card className="login-card">
        <Space direction="vertical" size="large">
          <div>
            <Typography.Text className="eyebrow">BROKEROS</Typography.Text>
            <Typography.Title level={1}>Risk Console</Typography.Title>
            <Typography.Paragraph type="secondary">
              Authenticate with Keycloak to access the bounded Risk Case operator workflow.
            </Typography.Paragraph>
          </div>
          {auth.error ? (
            <Alert type="error" showIcon message="Sign-in failed" description="Try signing in again." />
          ) : null}
          <Button
            type="primary"
            size="large"
            block
            loading={auth.isLoading || Boolean(auth.activeNavigator)}
            onClick={() => void auth.signinRedirect({ state: from })}
          >
            Sign in with Keycloak
          </Button>
          <Typography.Text type="secondary">
            Your password is entered only at the identity provider. This application does not receive it.
          </Typography.Text>
        </Space>
      </Card>
    </FullPageCard>
  );
}

function AppShell() {
  const auth = useAuth();
  const navigate = useNavigate();
  return (
    <Layout className="app-layout">
      <Layout.Header className="app-header">
        <Flex align="center" justify="space-between" gap="middle">
          <button className="brand-button" type="button" onClick={() => navigate('/cases')}>
            <span className="brand-mark">B</span>
            <span>
              <strong>BrokerOS</strong>
              <small>Risk Console</small>
            </span>
          </button>
          <Space>
            <Typography.Text className="header-identity">
              {auth.user?.profile.preferred_username ?? auth.user?.profile.name ?? 'Operator'}
            </Typography.Text>
            <Button
              ghost
              onClick={() => {
                void auth.signoutRedirect();
              }}
            >
              Sign out
            </Button>
          </Space>
        </Flex>
      </Layout.Header>
      <Layout.Content className="app-content">
        <Outlet />
      </Layout.Content>
    </Layout>
  );
}

function FullPageCard({ children }: PropsWithChildren) {
  return <main className="full-page-card">{children}</main>;
}

function FullPageSpinner({ label }: { label: string }) {
  return (
    <FullPageCard>
      <Space direction="vertical" align="center">
        <Spin size="large" />
        <Typography.Text type="secondary">{label}</Typography.Text>
      </Space>
    </FullPageCard>
  );
}
