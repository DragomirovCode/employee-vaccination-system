import { ApiErrorResponse, ApiHttpError } from "./types";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export async function apiGet<T>(path: string, token: string): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: "GET",
    headers: {
      "X-Auth-Token": token
    }
  });

  if (res.ok) {
    return (await res.json()) as T;
  }

  const payload = await parseApiError(res);
  emitAuthEvents(res.status);
  throw new ApiHttpError(res.status, payload);
}

async function parseApiError(response: Response): Promise<ApiErrorResponse | undefined> {
  try {
    return (await response.json()) as ApiErrorResponse;
  } catch {
    return undefined;
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
