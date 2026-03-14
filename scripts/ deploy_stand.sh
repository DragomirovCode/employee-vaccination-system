#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/app}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.stand.yml}"

cd "$APP_DIR"

docker compose -f "$COMPOSE_FILE" up -d --build --remove-orphans
docker compose -f "$COMPOSE_FILE" ps

curl -fsS http://127.0.0.1/ > /dev/null
curl -fsS http://127.0.0.1:8080/v3/api-docs > /dev/null
