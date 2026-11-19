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
4. **Solo en la etapa 1:** creá vos en la raíz del repositorio `docker-compose.yml`, `.env.example` y `.gitignore`, según `docs/spec/README.md` §3 y §4. El `.gitignore` incluye `.env` y `qa-reports/`. No crees ni leas `.env`: pedile al usuario que haga `cp .env.example .env` y complete los valores antes de verificar AC-SETUP-01.

## 4. Verificación

1. Pedile a `test-runner` que corra los tests de los criterios de la etapa y todos los tests unitarios. Si es una etapa de backend, también `./mvnw verify`.
2. Si algo falla, devolvé el fallo al agente que corresponde para que lo corrija. Como máximo **2 intentos**; si sigue fallando, detenete y reportá qué falla y por qué.
3. Pedile a `revisor-aceptacion` que revise la etapa contra la SPEC. Si marca un problema bloqueante, corregilo y volvé a verificar.

## 5. Cierre de la etapa

- **Etapa 1 a 11:** hacé el commit con el formato `feat(stage-$0): <nombre de la etapa en inglés>` y, en el cuerpo, la lista de criterios cubiertos. No hagas `git push`.
- **Cierre:** verificá que la base esté recién creada y que backend y frontend estén corriendo; si no, avisá qué falta. Después delegá en `qa-e2e` la prueba exploratoria (AC-E2E-01 a 09).

## 6. Informe

Terminá con:

- Una tabla: criterio · resultado (✓ / ✗) · test o evidencia.
- El hash del commit (o el informe de `qa-e2e` si fue el cierre).
- Cualquier decisión que hayas tomado que no estuviera en la SPEC.