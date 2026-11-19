#!/usr/bin/env bash
# Hook PostToolUse (Edit|Write): compila el backend cuando Claude edita un archivo Java.
# Si no compila, devuelve los errores a Claude (exit 2) para que los corrija.
# Requiere: jq.

set -u

FILE_PATH=$(jq -r '.tool_input.file_path // empty')

# Solo actúa sobre archivos .java del backend.
case "$FILE_PATH" in
  "$CLAUDE_PROJECT_DIR"/backend/*.java) ;;
  *) exit 0 ;;
esac

# Si el proyecto Maven todavía no existe (etapa 1), no hace nada.
cd "$CLAUDE_PROJECT_DIR/backend" 2>/dev/null || exit 0
[ -x ./mvnw ] || exit 0

if ! OUTPUT=$(./mvnw -q -DskipTests compile 2>&1); then
  echo "El backend no compila después de editar $FILE_PATH. Errores:" >&2
  echo "$OUTPUT" | grep -E "ERROR" | head -n 30 >&2
  exit 2
fi

exit 0
