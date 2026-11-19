---
description: Corre los tests de uno o varios criterios de aceptación (por ID o prefijo, por ejemplo AC-PRICE-01 o AC-PRICE)
argument-hint: "[ID o prefijo de criterio, ej. AC-PRICE-01 | AC-PRICE | AC-FE]"
disable-model-invocation: true
---

# Correr los criterios $ARGUMENTS

Delegá en `test-runner` lo siguiente:

1. Buscar los tests que cubren `$ARGUMENTS`:
   - **Backend:** métodos cuyo nombre empieza con el ID con guiones bajos (`AC-PRICE-01` → `AC_PRICE_01`) en `backend/src/test/`. Los de clases `*Test` se corren con `./mvnw test -Dtest=...`; los de clases `*IT`, con `./mvnw verify -Dit.test=...`.
   - **Frontend:** tests cuya descripción empieza con el ID (`AC-FE-02`) en `frontend/src/`. Se corren con `npx ng test --watch=false` incluyendo solo esos archivos.
2. Correrlos.
3. Devolver una tabla: criterio · test · resultado (✓ / ✗) · motivo del fallo en una línea.

Si un criterio de `docs/spec/acceptance.md` que coincide con `$ARGUMENTS` **no tiene ningún test**, marcarlo como "sin test" en la tabla. No escribas tests ni modifiques código: este comando solo informa.
