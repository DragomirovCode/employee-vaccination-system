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

export type NotificationBulkReadResponse = {
  updated: number;
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

export type DepartmentWriteRequest = {
  name: string;
  parentId: string | null;
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

export type EmployeeWriteRequest = {
  userId: string | null;
  departmentId: string;
  firstName: string;
  lastName: string;
  middleName: string | null;
  birthDate: string | null;
  position: string | null;
  hireDate: string | null;
};

export type AuthUserDto = {
  id: string;
  email: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
};

export type AuthUserWriteRequest = {
  email: string;
  isActive: boolean;
};

export type AuthUserStatusRequest = {
  active: boolean;
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

export type VaccineWriteRequest = {
  name: string;
  manufacturer: string | null;
  validityDays: number;
  dosesRequired: number;
  daysBetween: number | null;
  isActive: boolean;
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

export type VaccinationPage = PageResponse<VaccinationReadDto>;

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

export type VaccinationCoverageDepartmentItem = {
  departmentId: string;
  departmentName: string;
  employeesTotal: number;
  employeesCovered: number;
  coveragePercent: number;
};

export type VaccinationCoverageVaccineItem = {
  vaccineId: string;
  vaccineName: string;
  employeesTotal: number;
  employeesCovered: number;
  coveragePercent: number;
};

export type DiseaseDto = {
  id: number;
  name: string;
  description: string | null;
};

export type DiseaseWriteRequest = {
  name: string;
  description: string | null;
};

export type VaccineDiseaseLinkDto = {
  vaccineId: string;
  diseaseId: number;
};

export type VaccinationWriteRequest = {
  employeeId: string;
  vaccineId: string;
  vaccinationDate: string;
  doseNumber: number;
  batchNumber: string | null;
  expirationDate: string | null;
  notes: string | null;
};

export type VaccinationWriteResponse = {
  id: string;
};

export type DocumentWriteRequest = {
  vaccinationId: string;
  fileName: string;
  filePath: string;
  fileSize: number;
  mimeType: string;
};

export type DocumentWriteResponse = {
  id: string;
};
