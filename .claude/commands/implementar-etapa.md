---
description: Implementa una etapa del cotizador (1 a 11, o "cierre"), verifica sus criterios de aceptación y hace el commit
argument-hint: "[número de etapa 1-11 | cierre]"
disable-model-invocation: true
---

# Implementar la etapa $0

Sos el coordinador de esta etapa. Delegás el trabajo en los agentes del proyecto y no avanzás a un paso sin terminar el anterior.

## 1. Entender la etapa

1. Si `$0` no es un número del 1 al 11 ni `cierre`, detenete y pedí una etapa válida.
2. Buscá la etapa `$0` en la tabla de `docs/spec/acceptance.md` §9: su nombre y sus criterios.
3. Leé esos criterios en `docs/spec/acceptance.md`, las reglas del manual que mencionan (`docs/manual-funcional.md`) y las partes de la SPEC que hacen falta (`docs/spec/README.md` indica qué leer).
4. Revisá con `git status` y `git log` que las etapas anteriores estén commiteadas y que no haya cambios pendientes. Si los hay, detenete y preguntá.
5. Si `$0` es `cierre`, saltá directo al paso 5 (prueba exploratoria).

## 2. Plan

Presentá un plan corto:

- Qué se hace en backend y qué en frontend.
- Qué criterios cubre cada parte.
- Si falta información o algo de la SPEC se contradice, decilo acá.

**Esperá mi confirmación antes de seguir.**

## 3. Implementación

1. Delegá la parte de backend en `backend-dev`. Pasale la etapa, los criterios y las secciones de la SPEC que aplican.
2. Delegá la parte de frontend en `frontend-dev`, con la misma información. Si el frontend depende de endpoints nuevos, esperá a que el backend esté terminado.
3. Si una etapa no tiene parte de backend o de frontend, no delegues esa parte.
4. **Solo en la etapa 1, antes que nada:** creá vos en la raíz del repositorio `.gitignore`, `docker-compose.yml` y `.env.example`, según `docs/spec/README.md` §3 y §4. El `.gitignore` incluye `.env`, `qa-reports/`, `.claude/settings.local.json`, `CLAUDE.local.md`, `backend/target/`, `frontend/node_modules/`, `frontend/dist/` y `frontend/.angular/`. Antes de cualquier `git add`, verificá con `git status` que `.env` no aparezca. No leas ni modifiques `.env`. Si no existe, es lo único que se le pide al usuario (`cp .env.example .env` y completar los valores), y se espera su respuesta antes de verificar. 
## 4. Verificación (siempre automática: el usuario no prueba nada a mano)

1. Pedile a `test-runner` que corra los tests de los criterios de la etapa y todos los tests unitarios. Si es una etapa de backend, también `./mvnw verify`.
2. **Prueba de arranque real**, en todas las etapas a partir de la 1. La hacés vos con Bash, sin pedirle nada al usuario:
   1. `docker compose up -d` y esperá a que PostgreSQL acepte conexiones.
   2. Levantá el backend en segundo plano (`cd backend && ./mvnw spring-boot:run`) y esperá, como máximo 3 minutos, a que `http://localhost:8080/v3/api-docs` responda `200`.
   3. Si la etapa tiene frontend, corré `npx ng build` y levantá el frontend en segundo plano (`cd frontend && npm start`). Esperá a que `http://localhost:4200` responda `200`.
   4. Si la etapa agrega endpoints, probá con `curl` al menos un caso exitoso de cada uno contra el backend levantado (con login si hace falta).
   5. Detené el backend y el frontend que levantaste. La base puede quedar corriendo.

   Si algo no arranca, se trata igual que un test fallido.
3. Si algo falla, devolvé el fallo al agente que corresponde para que lo corrija. Como máximo **2 intentos**; si sigue fallando, detenete y reportá qué falla y por qué.
4. Pedile a `revisor-aceptacion` que revise la etapa contra la SPEC. Si marca un problema bloqueante, corregilo y volvé a verificar.

## 5. Cierre de la etapa

- **Etapa 1 a 11:** hacé el commit con el formato `feat(stage-$0): <nombre de la etapa en inglés>` y, en el cuerpo, la lista de criterios cubiertos. Incluí en el commit `.claude/agent-memory/` si cambió (la memoria del revisor se comparte con el equipo). No hagas `git push`.
- **Cierre:** preparás vos el entorno, sin pedirle nada al usuario:
  1. `docker compose down -v && docker compose up -d` (base recién creada).
  2. Levantá backend y frontend en segundo plano, como en 4.2, y esperá a que respondan.
  3. Delegá en `qa-e2e` la prueba exploratoria (AC-E2E-01 a 09).
  4. Al terminar, dejá backend y frontend corriendo y decile al usuario en qué URLs está la app.

## 6. Informe

Terminá con:

- Una tabla: criterio · resultado (✓ / ✗) · test o evidencia.
- El hash del commit (o el informe de `qa-e2e` si fue el cierre).
- Cualquier decisión que hayas tomado que no estuviera en la SPEC.