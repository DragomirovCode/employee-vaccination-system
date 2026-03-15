#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/app}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.stand.yml}"

cd "$APP_DIR"

docker compose -f "$COMPOSE_FILE" up -d --build --remove-orphans
docker compose -f "$COMPOSE_FILE" ps

echo "Waiting for frontend..."
for i in {1..30}; do
  if curl -fsS http://127.0.0.1/ > /dev/null; then
    echo "Frontend is up"
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "Frontend did not become ready in time"
    docker compose -f "$COMPOSE_FILE" logs frontend --tail=200
    exit 1
  fi
  sleep 2
done

echo "Waiting for backend..."
for i in {1..60}; do
  status_code="$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/ || true)"
  if [ -n "$status_code" ] && [ "$status_code" -ge 200 ] && [ "$status_code" -lt 500 ]; then
    echo "Backend is up (HTTP $status_code)"
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo "Backend did not become ready in time"
    docker compose -f "$COMPOSE_FILE" logs backend --tail=200
    exit 1
  fi
  sleep 2
done
