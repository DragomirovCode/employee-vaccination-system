#!/usr/bin/env bash
# ================================================================
# Проверка commit message при push в master
# ================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

# Берём только первую строку commit message
COMMIT_MESSAGE="$(git log -1 --pretty=%B | head -n 1)"

MODULES="generic|auth|employee|vaccine|vaccination|reporting|audit"
TYPES="feature|fix|docs|refactor|perf|test|config|security"
SCOPES="docs|backend|frontend|devops"

COMMIT_MASK="^MERGED:[[:space:]](((${SCOPES})[[:space:]]\|[[:space:]])?(${MODULES}))[[:space:]]\|[[:space:]](${TYPES})[[:space:]]\|[[:space:]]\#[[:digit:]]+[[:space:]]-[[:space:]].+"

echo "Последний commit message (первая строка):"
echo "----------------------------------------"
echo "$COMMIT_MESSAGE"
echo "----------------------------------------"

if [[ "$COMMIT_MESSAGE" =~ $COMMIT_MASK ]]; then
  echo -e "${GREEN}✅ Commit message корректен.${NC}"
else
  echo -e "${RED}❌ Некорректный commit message в master.${NC}"
  echo -e "${RED}Ожидаемый формат:${NC}"
  echo -e "MERGED: <module> | <type> | #<issue> - <описание>"
  echo -e "MERGED: <scope> | <module> | <type> | #<issue> - <описание>  (scope: backend|frontend, optional)"
  exit 1
fi
