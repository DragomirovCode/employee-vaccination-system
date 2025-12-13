#!/usr/bin/env bash
# ================================================================
# Скрипт для проверки корректности оформления Pull Request (PR)
# ================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

FAILURE=false

if [ -z "$PR_TITLE" ]; then
  echo -e "${RED}❌ Не задана переменная окружения PR_TITLE.${NC}"
  exit 1
fi

if [ -z "$PR_BODY" ]; then
  echo -e "${RED}❌ Не задана переменная окружения PR_BODY.${NC}"
  exit 1
fi

MODULES="generic|auth"
TYPES="feature|fix|docs|refactor|perf|test|config|security"

TITLE_MASK="^(${MODULES})[[:space:]]\|[[:space:]](${TYPES})[[:space:]]\|[[:space:]]\#[[:digit:]]+[[:space:]]-[[:space:]].+"
CHANGELOG_MASK="^Changelog:[[:space:]](${TYPES})$"

# --- НОВОЕ: берём последнюю непустую строку ---
PR_BODY_LAST_LINE="$(printf '%s\n' "$PR_BODY" | sed '/^[[:space:]]*$/d' | tail -n 1)"

echo
echo "##### 1. Проверка заголовка PR..."
echo "Текущий заголовок: '${PR_TITLE}'"

if [[ "$PR_TITLE" =~ $TITLE_MASK ]]; then
  echo -e "${GREEN}✅ Заголовок PR корректен.${NC}"
else
  echo -e "${RED}❌ Некорректный заголовок PR.${NC}"
  FAILURE=true
fi

echo
echo "##### 2. Проверка наличия Changelog в конце описания..."

if [[ "$PR_BODY_LAST_LINE" =~ $CHANGELOG_MASK ]]; then
  echo -e "${GREEN}✅ Changelog оформлен корректно.${NC}"
else
  echo -e "${RED}❌ Не найден корректный трейлер Changelog в конце описания PR.${NC}"
  echo -e "${BLUE}Ожидается:${NC} Changelog: fix"
  FAILURE=true
fi

echo
if [ "$FAILURE" = true ]; then
  echo -e "${RED}❌ Проверка PR не пройдена.${NC}"
  exit 1
else
  echo -e "${GREEN}🎉 Проверка PR успешно пройдена.${NC}"
fi
