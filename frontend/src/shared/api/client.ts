import { ApiErrorResponse, ApiHttpError } from "./types";
import { normalizeAuthToken, readSession } from "../../features/auth/session";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

type ApiRequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  headers?: Record<string, string>;
  body?: unknown;
  skipAuth?: boolean;
  authToken?: string;
  suppressAuthEvents?: boolean;
};

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { method = "GET", headers = {}, body, skipAuth = false, authToken, suppressAuthEvents = false } = options;
  const token = normalizeAuthToken(authToken ?? readSession()?.token ?? "");

  if (!skipAuth && !token) {
    if (!suppressAuthEvents) emitAuthEvents(401);
    throw new ApiHttpError(401, {
      code: "UNAUTHORIZED",
      message: "Session token is missing",
      path,
      timestamp: new Date().toISOString()
    });
  }

  const requestHeaders: Record<string, string> = { ...headers };
  if (!skipAuth) {
    requestHeaders["X-Auth-Token"] = token;
  }
  if (body !== undefined && !requestHeaders["Content-Type"]) {
    requestHeaders["Content-Type"] = "application/json";
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: requestHeaders,
    body: body === undefined ? undefined : JSON.stringify(body)
  });

  if (res.ok) {
    if (res.status === 204) return undefined as T;
    const contentType = res.headers.get("content-type") ?? "";
    const text = await res.text();
    if (!text) return undefined as T;

    if (contentType.toLowerCase().includes("application/json")) {
      return JSON.parse(text) as T;
    }

    try {
      return JSON.parse(text) as T;
    } catch {
      return text as T;
    }
  }

  const payload = await parseApiError(res, path);
  if (!suppressAuthEvents) emitAuthEvents(res.status);
  throw new ApiHttpError(res.status, payload);
}

export function apiGet<T>(path: string, options: Omit<ApiRequestOptions, "method" | "body"> = {}): Promise<T> {
  return apiRequest<T>(path, { ...options, method: "GET" });
}

export function apiPost<T>(path: string, body?: unknown, options: Omit<ApiRequestOptions, "method" | "body"> = {}): Promise<T> {
  return apiRequest<T>(path, { ...options, method: "POST", body });
}

export function apiPut<T>(path: string, body?: unknown, options: Omit<ApiRequestOptions, "method" | "body"> = {}): Promise<T> {
  return apiRequest<T>(path, { ...options, method: "PUT", body });
}

export function apiPatch<T>(path: string, body?: unknown, options: Omit<ApiRequestOptions, "method" | "body"> = {}): Promise<T> {
  return apiRequest<T>(path, { ...options, method: "PATCH", body });
}

export function apiDelete<T>(path: string, options: Omit<ApiRequestOptions, "method" | "body"> = {}): Promise<T> {
  return apiRequest<T>(path, { ...options, method: "DELETE" });
}

async function parseApiError(response: Response, path: string): Promise<ApiErrorResponse> {
  try {
    return (await response.json()) as ApiErrorResponse;
  } catch {
    return {
      code: `HTTP_${response.status}`,
      message: response.statusText || "Request failed",
      path,
      timestamp: new Date().toISOString()
    };
  }
}

function emitAuthEvents(status: number): void {
  if (status === 401) {
    window.dispatchEvent(new Event("api:unauthorized"));
  }
  if (status === 403) {
    window.dispatchEvent(new Event("api:forbidden"));
  }
}
