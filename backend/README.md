# modular-monolith (Gradle multi-module)

Модули:
- `app` — запускаемое Spring Boot приложение.
- `auth` — библиотечный модуль с демонстрационным `AuthService`.

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
