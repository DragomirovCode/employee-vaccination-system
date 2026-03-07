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

Default credentials:
- DB: `evs`
- User: `evs`
- Password: `evs`
- Port: `5432`

`app` module uses these defaults via `application.yml`, or you can override with:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

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
- `vaccinations.next_dose_date` and `vaccinations.revaccination_date` are persisted fields
- `documents` table stores file metadata for vaccination documents
- `documents.vaccination_id` is FK to `vaccinations(id)`

## Reporting module
- `reporting` module is included in backend multi-module build
- no own tables, reads data from `employee`, `vaccine`, `vaccination`
- endpoint:
  - `GET /reports/revaccination-due?days=30&departmentId=<uuid>&page=0&size=20`
  - `GET /reports/vaccination-coverage?dateFrom=2026-01-01&dateTo=2026-12-31&departmentId=<uuid>`
- response fields:
  - `employeeId`, `fullName`, `departmentId`, `vaccineName`
  - `lastVaccinationDate`, `revaccinationDate`, `daysLeft`

## Audit module
- `audit` module is included in backend multi-module build
- `audit_log` table stores immutable audit records
- captures:
  - `action`: CREATE/UPDATE/DELETE
  - `entity_type`: VACCINATION/DOCUMENT
  - `entity_id`, `user_id`, `old_value`, `new_value`, `created_at`
- integrated with `vaccination` module services for `vaccinations` and `documents`

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
  - `GET /employees`, `GET /employees/{id}` - any authenticated user
  - `POST /employees`, `PUT /employees/{id}`, `DELETE /employees/{id}` - `HR`, `ADMIN`

Employee module business rules:
- Department hierarchy cycles are rejected with `400`
- Employee `departmentId` must reference existing department (`400` on invalid reference)
- `employees.user_id` must be unique when present (`409` on conflict)

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
