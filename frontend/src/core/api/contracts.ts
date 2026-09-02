export const resultCodes = [
  'SUCCESS',
  'VALIDATION_ERROR',
  'MALFORMED_REQUEST',
  'AUTHENTICATION_REQUIRED',
  'AUTHENTICATION_INVALID',
  'ACTOR_ACCESS_DENIED',
  'AUTHORIZATION_DENIED',
  'SECURITY_DEPENDENCY_UNAVAILABLE',
  'RISK_CASE_NOT_FOUND',
  'RISK_CASE_INVALID_TRANSITION',
  'RISK_CASE_INVARIANT_VIOLATION',
  'RISK_CASE_VERSION_CONFLICT',
  'RISK_CASE_REFERENCE_NOT_FOUND',
  'RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE',
  'RISK_CASE_SUBJECT_NOT_ELIGIBLE',
  'INTERNAL_ERROR',
] as const;

export type ResultCode = (typeof resultCodes)[number];

export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T | null;
  timestamp: string;
}

export type JsonRecord = Record<string, unknown>;

export function asRecord(value: unknown, label: string): JsonRecord {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new ContractError(`${label} must be an object`);
  }
  return value as JsonRecord;
}

export function asString(value: unknown, label: string): string {
  if (typeof value !== 'string') {
    throw new ContractError(`${label} must be a string`);
  }
  return value;
}

export function asNullableString(value: unknown, label: string): string | null {
  return value === null ? null : asString(value, label);
}

export function asInteger(value: unknown, label: string): number {
  if (typeof value !== 'number' || !Number.isSafeInteger(value)) {
    throw new ContractError(`${label} must be a safe integer`);
  }
  return value;
}

export function asBoolean(value: unknown, label: string): boolean {
  if (typeof value !== 'boolean') {
    throw new ContractError(`${label} must be a boolean`);
  }
  return value;
}

export function asInstant(value: unknown, label: string): string {
  const instant = asString(value, label);
  if (Number.isNaN(Date.parse(instant))) {
    throw new ContractError(`${label} must be an ISO-8601 timestamp`);
  }
  return instant;
}

export function asArray<T>(
  value: unknown,
  label: string,
  parseItem: (item: unknown, index: number) => T,
): T[] {
  if (!Array.isArray(value)) {
    throw new ContractError(`${label} must be an array`);
  }
  return value.map(parseItem);
}

export function parseApiResponse<T>(
  value: unknown,
  parseData: (data: unknown) => T,
): ApiResponse<T> {
  const record = asRecord(value, 'ApiResponse');
  const timestamp = asInstant(record.timestamp, 'ApiResponse.timestamp');
  return {
    code: asString(record.code, 'ApiResponse.code'),
    message: asString(record.message, 'ApiResponse.message'),
    data: record.data === null ? null : parseData(record.data),
    timestamp,
  };
}

export class ContractError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ContractError';
  }
}
