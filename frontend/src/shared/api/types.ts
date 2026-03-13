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

export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

export type RevaccinationDueItem = {
  employeeId: string;
  fullName: string;
  departmentId: string;
  vaccineName: string;
  lastVaccinationDate: string;
  revaccinationDate: string;
  daysLeft: number;
};

export type DepartmentDto = {
  id: string;
  name: string;
  parentId: string | null;
  createdAt: string;
};

export type EmployeeDto = {
  id: string;
  userId: string | null;
  departmentId: string;
  firstName: string;
  lastName: string;
  middleName: string | null;
  birthDate: string | null;
  position: string | null;
  hireDate: string | null;
  createdAt: string;
  updatedAt: string;
};

export type VaccineDto = {
  id: string;
  name: string;
  manufacturer: string | null;
  validityDays: number;
  dosesRequired: number;
  daysBetween: number | null;
  isActive: boolean;
  createdAt: string;
};

export type VaccinationReadDto = {
  id: string;
  employeeId: string;
  vaccineId: string;
  performedBy: string;
  vaccinationDate: string;
  doseNumber: number;
  batchNumber: string | null;
  expirationDate: string | null;
  nextDoseDate: string | null;
  revaccinationDate: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
};

export type DocumentDto = {
  id: string;
  vaccinationId: string;
  fileName: string;
  filePath: string;
  fileSize: number;
  mimeType: string;
  uploadedBy: string;
  uploadedAt: string;
};
