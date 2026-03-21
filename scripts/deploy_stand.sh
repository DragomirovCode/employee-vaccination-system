#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/app}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.stand.yml}"
GHCR_REGISTRY="${GHCR_REGISTRY:-ghcr.io}"

cd "$APP_DIR"

if [ -n "${GHCR_USERNAME:-}" ] && [ -n "${GHCR_TOKEN:-}" ]; then
  echo "$GHCR_TOKEN" | docker login "$GHCR_REGISTRY" -u "$GHCR_USERNAME" --password-stdin
fi

docker compose -f "$COMPOSE_FILE" pull backend frontend
docker compose -f "$COMPOSE_FILE" up -d --remove-orphans
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
  if bash -c "</dev/tcp/127.0.0.1/8080" >/dev/null 2>&1; then
    echo "Backend port 8080 is accepting connections"
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo "Backend did not become ready in time"
    docker compose -f "$COMPOSE_FILE" logs backend --tail=200
    exit 1
  fi
  sleep 2
done
