# modular-monolith (Gradle multi-module)

Модули:
- `app` — запускаемое Spring Boot приложение.
- `auth` — библиотечный модуль с демонстрационным `AuthService`.
- `employee` — библиотечный модуль кадрового учета (departments, employees).
- `vaccine` — библиотечный модуль справочников вакцин (vaccines, diseases).
- `vaccination` — библиотечный модуль учета вакцинаций и документов.
- `reporting` — библиотечный модуль read-only отчетов.

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
