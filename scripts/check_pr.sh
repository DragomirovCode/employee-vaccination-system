#!/usr/bin/env bash
# ================================================================
# DEBUG: Проверка запуска скрипта в CI
# ================================================================

echo "=== DEBUG: script started ==="
echo "User: $(whoami)"
echo "PWD: $(pwd)"
echo "Script path: $0"
echo

echo "=== DEBUG: ls -l scripts ==="
ls -l scripts || true
echo

echo "=== DEBUG: ls -l this file ==="
ls -l "$0" || true
echo

echo "=== DEBUG: file this file ==="
file "$0" || true
echo

echo "=== DEBUG: PR_TITLE ==="
echo "${PR_TITLE:-<empty>}"
echo

echo "=== DEBUG: PR_BODY ==="
echo "${PR_BODY:-<empty>}"
echo

echo "=== DEBUG: end ==="
