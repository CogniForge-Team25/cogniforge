const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export const ROLES = [
  'SECURITY_MANAGER',
  'SECURITY_OFFICER',
  'IT_ADMINISTRATOR',
  'COMPLIANCE_OFFICER',
  'INTERNAL_ASSESSOR',
  'EXECUTIVE_REVIEWER',
] as const;

export type Role = (typeof ROLES)[number];

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  role: Role;
}

export interface Framework {
  code: string;
  name: string;
  version: string;
  clauseCount: number;
}

export interface Requirement {
  clauseRef: string;
  title: string;
}

export interface CreateUserResponse {
  email: string;
  invitationToken: string;
  expiresAt: string;
}

// The token lives only in this variable (memory). It is lost on page refresh.
let accessToken: string | null = null;
let onUnauthorized: (() => void) | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function setOnUnauthorized(handler: (() => void) | null): void {
  onUnauthorized = handler;
}

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST';
  body?: unknown;
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {};
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  const sentToken = accessToken;
  if (sentToken) {
    headers['Authorization'] = `Bearer ${sentToken}`;
  }

  let response: Response;
  try {
    response = await fetch(`${API_URL}${path}`, {
      method: options.method ?? 'GET',
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Is the backend running?');
  }

  if (!response.ok) {
    // A 401 on a request that carried a token means the session is no longer valid.
    if (response.status === 401 && sentToken && onUnauthorized) {
      onUnauthorized();
    }
    let message = `Request failed (${response.status})`;
    try {
      const data = await response.json();
      if (data && typeof data.message === 'string' && data.message) {
        message = data.message;
      }
    } catch {
      // The error response had no JSON body. Keep the default message.
    }
    throw new ApiError(response.status, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}
