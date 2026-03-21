# modular-monolith (Gradle multi-module)

Модули:
- `app` — запускаемое Spring Boot приложение.
- `auth` — библиотечный модуль с демонстрационным `AuthService`.
- `employee` — библиотечный модуль кадрового учета (departments, employees).
- `vaccine` — библиотечный модуль справочников вакцин (vaccines, diseases).
- `vaccination` — библиотечный модуль учета вакцинаций и документов.
- `reporting` — библиотечный модуль read-only отчетов.
- `audit` — библиотечный модуль журнала изменений.

## Требования
- JDK 21

## Запуск
```bash
./gradlew :app:bootRun
```

Проверка эндпоинта:
```bash
curl -i http://localhost:8080/hello
curl -i -H 'X-Auth-Token: dev-token' http://localhost:8080/hello
```

Swagger UI:
```bash
http://localhost:8080/swagger-ui/index.html
```

## Сборка
```bash
./gradlew build
```

## PostgreSQL (Docker)
From repository root:
```bash
docker compose up -d postgres
```

PostgreSQL + MinIO:
```bash
docker compose up -d postgres minio
```

Default credentials:
- DB: `evs`
- User: `evs`
- Password: `evs`
- Port: `5432`

`app` module uses these defaults via `application.yml`, or you can override with:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `STORAGE_PROVIDER` (`inmemory` by default, set `minio` for object storage)
- `MINIO_ENDPOINT` (default `http://localhost:9000`)
- `MINIO_ACCESS_KEY` (default `minioadmin`)
- `MINIO_SECRET_KEY` (default `minioadmin`)
- `MINIO_BUCKET` (default `evs-documents`)

## Starter seed data
- On application startup, Flyway migration `V8__seed_initial_data.sql` fills all main tables with demo data.
- Seeded users for quick API checks (`X-Auth-Token` accepts the UUID directly):
  - `admin@evs.local` - `ADMIN` - `018f4fd2-75f8-7f2e-b95e-9df7ac8e3a10`
  - `hr@evs.local` - `HR` - `018f4fd2-75f8-7f2e-b95e-9df7ac8e3a11`
  - `medical@evs.local` - `MEDICAL` - `018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12`
  - `petr.orlov@evs.local` - `PERSON` - `018f4fd2-75f8-7f2e-b95e-9df7ac8e3a13`
  - `polina.smirnova@evs.local` - `PERSON` - `018f4fd2-75f8-7f2e-b95e-9df7ac8e3a14`
- Seed also includes:
  - department hierarchy
  - employees linked to seeded users
  - diseases, vaccines, vaccine-disease links
  - vaccination records, document metadata, notifications, and audit log entries

## Auth persistence
- `users` table with UUID v7 ids
- `roles` table
- `user_roles` table (composite PK: `user_id`, `role_id`)

## Employee module
- `employee` module is included in backend multi-module build
- `departments` table uses UUID v4 ids
- `employees` table uses UUID v7 ids
- `employees.user_id` is nullable and unique

## Vaccine module
- `vaccine` module is included in backend multi-module build
- `vaccines` table uses UUID v4 ids
- `diseases` table uses integer identity ids
- `vaccine_diseases` uses composite PK (`vaccine_id`, `disease_id`)

## Vaccination module
- `vaccination` module is included in backend multi-module build
- `vaccinations` table stores vaccination facts
- `vaccinations.expiration_date` stores vial expiration date and is required for create/update
- `vaccinations.next_dose_date` and `vaccinations.revaccination_date` are persisted fields
- `documents` table stores file metadata for vaccination documents
- `documents.vaccination_id` is FK to `vaccinations(id)`

## Reporting module
- `reporting` module is included in backend multi-module build
- no own tables, reads data from `employee`, `vaccine`, `vaccination`
- endpoint:
  - `GET /reports/revaccination-due?days=30&departmentId=<uuid>&page=0&size=20`
  - `GET /reports/vaccination-coverage?dateFrom=2026-01-01&dateTo=2026-12-31&departmentId=<uuid>`
  - `GET /reports/vaccination-coverage-by-vaccine?dateFrom=2026-01-01&dateTo=2026-12-31&departmentId=<uuid>`
  - `GET /reports/revaccination-due/export?days=30&departmentId=<uuid>&format=csv|xlsx|pdf`
  - `GET /reports/vaccination-coverage/export?dateFrom=2026-01-01&dateTo=2026-12-31&departmentId=<uuid>&format=csv|xlsx|pdf`
  - `GET /reports/vaccination-coverage-by-vaccine/export?dateFrom=2026-01-01&dateTo=2026-12-31&departmentId=<uuid>&format=csv|xlsx|pdf`
- response fields:
  - `employeeId`, `fullName`, `departmentId`, `vaccineName`
  - `lastVaccinationDate`, `revaccinationDate`, `daysLeft`
  - `vaccineId`, `vaccineName`, `employeesTotal`, `employeesCovered`, `coveragePercent`
 - export:
  - formats: `csv`, `xlsx`, `pdf`
  - response headers include `Content-Disposition: attachment` and format-specific `Content-Type`

## Audit module
- `audit` module is included in backend multi-module build
- `audit_log` table stores immutable audit records
- captures:
  - `action`: CREATE/UPDATE/DELETE
  - `entity_type`: USER/USER_ROLE/DEPARTMENT/EMPLOYEE/VACCINE/DISEASE/VACCINE_DISEASE/VACCINATION/DOCUMENT
  - `entity_id`, `user_id`, `old_value`, `new_value`, `created_at`
- integrated with write services in modules: `auth`, `employee`, `vaccine`, `vaccination`

## Security model (RBAC + scope)

Authentication header:
- `X-Auth-Token: <user-uuid>`
- optional form: `X-Auth-Token: Bearer <user-uuid>`

Status codes:
- `401` - token missing/invalid, user not found, or inactive user
- `403` - authenticated but role/scope is not sufficient

Role matrix for reporting endpoints:
- `PERSON` - can read only own records (`employees.user_id = current user`)
- `HR` - can read employees from own department and all descendant departments
- `MEDICAL` - full reporting scope
- `ADMIN` - full reporting scope

Endpoints:
- `GET /reports/revaccination-due`
- `GET /reports/vaccination-coverage`
- `GET /reports/vaccination-coverage-by-vaccine`
- `GET /reports/revaccination-due/export`
- `GET /reports/vaccination-coverage/export`
- `GET /reports/vaccination-coverage-by-vaccine/export`

Scope is enforced in query/service layer, not only in controllers.

Write permissions for vaccination module:
- `POST /vaccinations`, `PUT /vaccinations/{id}`, `DELETE /vaccinations/{id}`: `MEDICAL`, `ADMIN`
- `POST /documents`, `PUT /documents/{id}`, `DELETE /documents/{id}`: `MEDICAL`, `ADMIN`
- `HR`, `PERSON` receive `403` for write operations.

Employee module API:
- departments:
  - `GET /departments`, `GET /departments/{id}` - any authenticated user
  - `POST /departments`, `PUT /departments/{id}`, `DELETE /departments/{id}` - `HR`, `ADMIN`
- employees:
  - `GET /employees`, `GET /employees/{id}` - any authenticated user, but data is scope-limited by role
  - `POST /employees`, `PUT /employees/{id}` - `HR`, `ADMIN`
  - `DELETE /employees/{id}` - `ADMIN`

Employee module business rules:
- Department hierarchy cycles are rejected with `400`
- Employee `departmentId` must reference existing department (`400` on invalid reference)
- `employees.user_id` must be unique when present (`409` on conflict)
- `PERSON` can read only own employee profile
- `HR` can read employees only from own department and all descendant departments

Vaccine module API:
- vaccines:
  - `GET /vaccines`, `GET /vaccines/{id}` - any authenticated user
  - `POST /vaccines`, `PUT /vaccines/{id}`, `DELETE /vaccines/{id}` - `MEDICAL`, `ADMIN`
- diseases:
  - `GET /diseases`, `GET /diseases/{id}` - any authenticated user
  - `POST /diseases`, `PUT /diseases/{id}`, `DELETE /diseases/{id}` - `MEDICAL`, `ADMIN`
- vaccine-disease links:
  - `GET /vaccines/{vaccineId}/diseases` - any authenticated user
  - `POST /vaccines/{vaccineId}/diseases/{diseaseId}`, `DELETE /vaccines/{vaccineId}/diseases/{diseaseId}` - `MEDICAL`, `ADMIN`

Vaccine module business rules:
- duplicate `vaccine_id + disease_id` link is rejected with `409`
- link creation with unknown `vaccineId` or `diseaseId` is rejected with `400`
- deleting vaccine/disease with existing links is rejected with `409`
- disease name cannot be changed if any linked vaccine already has vaccination records (`409`)
- vaccine-disease link cannot be deleted if the vaccine already has vaccination records (`409`)
- for a vaccine that already has vaccination records, only `isActive` can be changed; other fields are rejected with `409`

Auth admin API (`ADMIN` only):
- users:
  - `GET /auth/users`, `GET /auth/users/{id}`
  - `POST /auth/users`, `PUT /auth/users/{id}`
  - `PATCH /auth/users/{id}/status`
- roles:
  - `GET /auth/roles`
  - `GET /auth/users/{id}/roles`
  - `POST /auth/users/{id}/roles/{roleCode}`
  - `DELETE /auth/users/{id}/roles/{roleCode}`

Auth admin business rules:
- duplicate `email` is rejected with `409`
- admin create/update user payload uses `email` and `isActive`; `passwordHash` is not part of frontend-facing admin contract
- duplicate user-role assignment is rejected with `409`
- role assignment stores `assigned_by` as current admin user id

Notifications API (personal):
- `GET /notifications?onlyUnread=false&page=0&size=20` - current user's notifications
- `PATCH /notifications/{id}/read` - mark one notification as read
- `PATCH /notifications/read-all` - mark all current user's notifications as read

Notifications business rules:
- authenticated user can read and update only own notifications
- access to someone else's notification id returns `404`
- vaccination create/update emits notification when target employee has linked `user_id`

Vaccination read API:
- `GET /vaccinations/{id}` - vaccination card
- `GET /vaccinations` - list with filters: `employeeId`, `vaccineId`, `dateFrom`, `dateTo`, `page`, `size`
- `GET /employees/{employeeId}/vaccinations` - employee vaccination history
- `GET /vaccinations/{vaccinationId}/documents` - documents by vaccination
- `GET /documents/{id}` - document metadata card

Vaccination read access scope:
- `PERSON` - only own records
- `HR` - own department and all descendant departments
- `MEDICAL`, `ADMIN` - full access
- invalid date range (`dateFrom > dateTo`) returns `400`

Document content API:
- `POST /documents/{id}/content` - upload file content for existing document metadata
- `GET /documents/{id}/content` - download file content
- `DELETE /documents/{id}/content` - delete file content from storage

Storage providers:
- default: `inmemory` (for local run and tests)
- MinIO: set `STORAGE_PROVIDER=minio` and provide:
  - `MINIO_ENDPOINT`
  - `MINIO_ACCESS_KEY`
  - `MINIO_SECRET_KEY`
  - `MINIO_BUCKET`

## Unified API error format

All API errors are returned as JSON with fields:
- `code`: machine-readable error code
- `message`: human-readable error message
- `details`: optional list of validation details
- `path`: request path
- `timestamp`: error time (UTC, ISO-8601)
- `traceId`: optional value from `X-Trace-Id` request header

Current mappings:
- `ResponseStatusException` -> HTTP status from exception, code by status (`BAD_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, or `HTTP_<status>`)
- `MethodArgumentNotValidException` / `BindException` -> `400`, `VALIDATION_ERROR`
- `HttpMessageNotReadableException` -> `400`, `INVALID_REQUEST_BODY`
- `IllegalArgumentException` -> `400`, `INVALID_ARGUMENT`
- any other unhandled exception -> `500`, `INTERNAL_ERROR`
