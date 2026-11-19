#!/usr/bin/env bash
# Hook Stop: cuando Claude termina de responder, corre los tests UNITARIOS
# de las partes con cambios sin commitear (backend y/o frontend).
# Si fallan, devuelve el resumen a Claude (exit 2) para que siga trabajando.
# Los tests de integración (./mvnw verify) no se corren acá porque son lentos.
# Requiere: jq y git.

set -u

INPUT=$(cat)

# Evita el bucle infinito: si este hook ya hizo que Claude siga trabajando, no vuelve a bloquear.
if [ "$(echo "$INPUT" | jq -r '.stop_hook_active // false')" = "true" ]; then
  exit 0
fi

cd "$CLAUDE_PROJECT_DIR" || exit 0
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || exit 0

FAILED=0
REPORT=""

# Backend: solo si hay cambios sin commitear en backend/.
if [ -x backend/mvnw ] && [ -n "$(git status --porcelain -- backend)" ]; then
  if ! OUT=$(cd backend && ./mvnw -q test 2>&1); then
    FAILED=1
    REPORT+=$'\n--- Backend (./mvnw test) ---\n'
    REPORT+="$(echo "$OUT" | grep -E "FAIL|ERROR|Tests run" | head -n 40)"
  fi
fi

# Frontend: solo si hay cambios sin commitear en frontend/.
# Nota: si el runner por defecto de Angular 22 fuera Karma, agregar --browsers=ChromeHeadless.
if [ -f frontend/package.json ] && [ -n "$(git status --porcelain -- frontend)" ]; then
  if ! OUT=$(cd frontend && npx ng test --watch=false 2>&1); then
    FAILED=1
    REPORT+=$'\n--- Frontend (ng test) ---\n'
    REPORT+="$(echo "$OUT" | tail -n 40)"
  fi
fi

if [ "$FAILED" = "1" ]; then
  echo "Hay tests unitarios fallando. Corregilos antes de terminar:${REPORT}" >&2
  exit 2
fi

exit 0
