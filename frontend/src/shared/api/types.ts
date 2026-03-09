export type ApiErrorResponse = {
  code: string;
  message: string;
  details?: string[];
  path: string;
  timestamp: string;
  traceId?: string;
};

export class ApiHttpError extends Error {
  readonly status: number;
  readonly payload?: ApiErrorResponse;

  constructor(status: number, payload?: ApiErrorResponse) {
    super(payload?.message ?? `Request failed with status ${status}`);
    this.name = "ApiHttpError";
    this.status = status;
    this.payload = payload;
  }
}

export type NotificationDto = {
  id: string;
  type: "REVACCINATION_DUE" | "REVOKED_DOCUMENT" | "SYSTEM";
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
  readAt: string | null;
  payload: string | null;
};

export type NotificationPage = {
  content: NotificationDto[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};
