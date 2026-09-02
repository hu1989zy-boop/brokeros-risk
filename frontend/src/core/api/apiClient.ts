import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios';

import { asRecord, parseApiResponse, type ApiResponse } from './contracts';
import { ApiError, AuthenticationRequiredError, AuthorizationError } from './errors';

export interface AuthSession {
  getAccessToken(): string | null;
  refreshAccessToken(): Promise<string | null>;
  authenticationRequired(): void;
}

type RetriableRequest = InternalAxiosRequestConfig & { q016Retried?: boolean };

export class ApiClient {
  private readonly axios: AxiosInstance;
  private refreshInFlight: Promise<string | null> | null = null;

  constructor(baseUrl: string, private readonly auth: AuthSession, instance?: AxiosInstance) {
    this.axios = instance ?? axios.create({ baseURL: baseUrl, timeout: 15_000 });
    this.axios.interceptors.request.use((request) => {
      const token = this.auth.getAccessToken();
      if (token && !request.headers.has('Authorization')) {
        request.headers.Authorization = `Bearer ${token}`;
      }
      return request;
    });
    this.axios.interceptors.response.use(
      (response) => response,
      async (error: unknown) => this.handleTransportError(error),
    );
  }

  async get<T>(path: string, parseData: (data: unknown) => T, config?: AxiosRequestConfig): Promise<T> {
    return this.unwrap(await this.axios.get<unknown>(path, config), parseData);
  }

  async post<T>(path: string, body: unknown, parseData: (data: unknown) => T): Promise<T> {
    return this.unwrap(await this.axios.post<unknown>(path, body), parseData);
  }

  private unwrap<T>(response: { data: unknown; status: number }, parseData: (data: unknown) => T): T {
    let envelope: ApiResponse<T>;
    try {
      envelope = parseApiResponse(response.data, parseData);
    } catch {
      throw new ApiError(
        'INVALID_API_RESPONSE',
        'The BrokerOS Risk API returned an invalid response.',
        response.status,
      );
    }
    if (envelope.code !== 'SUCCESS' || envelope.data === null) {
      throw new ApiError(envelope.code, envelope.message, response.status);
    }
    return envelope.data;
  }

  private async handleTransportError(error: unknown): Promise<never> {
    if (!(error instanceof AxiosError) || !error.config) {
      throw new ApiError('API_UNAVAILABLE', 'The BrokerOS Risk API is unavailable.', null);
    }

    const request = error.config as RetriableRequest;
    const status = error.response?.status ?? null;
    if (status === 401 && !request.q016Retried) {
      request.q016Retried = true;
      const refreshed = await this.refreshOnce();
      if (refreshed) {
        request.headers.Authorization = `Bearer ${refreshed}`;
        return this.axios.request(request);
      }
      this.auth.authenticationRequired();
      throw new AuthenticationRequiredError(this.failureMessage(error) ?? undefined);
    }

    const failure = this.failureEnvelope(error);
    if (status === 403) {
      throw new AuthorizationError(failure?.code, failure?.message);
    }
    throw new ApiError(
      failure?.code ?? 'API_UNAVAILABLE',
      failure?.message ?? 'The BrokerOS Risk API is unavailable.',
      status,
    );
  }

  private refreshOnce(): Promise<string | null> {
    if (!this.refreshInFlight) {
      this.refreshInFlight = this.auth.refreshAccessToken().finally(() => {
        this.refreshInFlight = null;
      });
    }
    return this.refreshInFlight;
  }

  private failureEnvelope(error: AxiosError): { code: string; message: string } | null {
    try {
      const record = asRecord(error.response?.data, 'ApiResponse');
      return {
        code: typeof record.code === 'string' ? record.code : 'API_ERROR',
        message:
          typeof record.message === 'string' && record.message.trim()
            ? record.message
            : 'The request could not be completed.',
      };
    } catch {
      return null;
    }
  }

  private failureMessage(error: AxiosError): string | null {
    return this.failureEnvelope(error)?.message ?? null;
  }
}
