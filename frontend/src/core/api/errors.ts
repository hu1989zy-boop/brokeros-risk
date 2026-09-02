export class ApiError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly httpStatus: number | null,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export class AuthenticationRequiredError extends ApiError {
  constructor(message = 'Your session expired. Sign in again to continue.') {
    super('AUTHENTICATION_REQUIRED', message, 401);
    this.name = 'AuthenticationRequiredError';
  }
}

export class AuthorizationError extends ApiError {
  constructor(code = 'AUTHORIZATION_DENIED', message = 'You are not authorized to do that.') {
    super(code, message, 403);
    this.name = 'AuthorizationError';
  }
}

export function userFacingError(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  return fallback;
}
