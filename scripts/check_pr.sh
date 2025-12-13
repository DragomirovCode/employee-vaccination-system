#!/usr/bin/env bash
# ================================================================
# Скрипт для проверки корректности оформления Pull Request (PR)
# ================================================================
# Ожидаемый формат заголовка:
#   <module> | <type> | #<issue_number> - <описание>
#
# Пример:
#   auth | fix | #42 - Исправить проверку токена
# ================================================================

set -e

# --- Цвета ---
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

FAILURE=false

# --- Проверка переменных окружения ---
if [ -z "$PR_TITLE" ]; then
  echo -e "${RED}❌ Не задана переменная окружения PR_TITLE.${NC}"
  exit 1
fi

if [ -z "$PR_BODY" ]; then
  echo -e "${RED}❌ Не задана переменная окружения PR_BODY.${NC}"
  exit 1
fi

# --- Разрешённые модули ---
MODULES="generic|auth"

# --- Разрешённые типы ---
TYPES="feature|fix|docs|refactor|perf|test|config|security"

# --- Маска заголовка ---
TITLE_MASK="^(${MODULES})[[:space:]]\|[[:space:]](${TYPES})[[:space:]]\|[[:space:]]\#[[:digit:]]+[[:space:]]-[[:space:]].+"

# --- Маска Changelog (в конце описания) ---
CHANGELOG_MASK="Changelog:[[:space:]](${TYPES})([[:space:]]*)$"

echo " "
echo "##### 1. Проверка заголовка PR..."
echo "Текущий заголовок: '${PR_TITLE}'"

if [[ "$PR_TITLE" =~ $TITLE_MASK ]]; then
  echo -e "${GREEN}✅ Заголовок PR корректен.${NC}"
else
  echo -e "${RED}❌ Некорректный заголовок PR.${NC}"
  echo -e "${RED}Ожидаемый формат:${NC}"
  echo -e "${BLUE}<module> | <type> | #<issue> - <описание>${NC}"
  echo -e "${BLUE}Пример:${NC}"
  echo -e "${BLUE}auth | fix | #42 - Исправить проверку токена${NC}"
  echo -e "${RED}Доступные модули: ${MODULES}${NC}"
  echo -e "${RED}Доступные типы: ${TYPES}${NC}"
  FAILURE=true
fi

echo " "
echo "##### 2. Проверка наличия Changelog в конце описания..."

if [[ "$PR_BODY" =~ $CHANGELOG_MASK ]]; then
  echo -e "${GREEN}✅ Changelog оформлен корректно.${NC}"
else
  echo -e "${RED}❌ Не найден корректный трейлер Changelog в конце описания PR.${NC}"
  echo -e "${RED}Описание PR должно заканчиваться строкой:${NC}"
  echo -e "${BLUE}Changelog: fix${NC}"
  FAILURE=true
fi

# --- Итог ---
echo " "
if [ "$FAILURE" = true ]; then
  echo -e "${RED}❌ Проверка PR не пройдена. Исправьте замечания выше.${NC}"
  exit 1
else
  echo -e "${GREEN}🎉 Проверка PR успешно пройдена.${NC}"
  exit 0
fi
