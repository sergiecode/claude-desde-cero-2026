---
name: project-decisions
description: Decisiones del proyecto cotizador que no son obvias en la SPEC (conteos de seed, convenciones aceptadas en revisiones previas)
metadata:
  type: project
---

- `allowed_usages` tiene **17** filas (no 19). La nota de `seed-data.md` §1.6 decía 19 pero ambas tablas listan 17; el usuario decidió 17 y la nota se corrigió en la etapa 2 (2026-11-26).
  **Why:** contradicción en la SPEC resuelta por el usuario.
  **How to apply:** no marcar 17 como error; si aparece 19 en código/tests, sí es error.
- Etapa 2 (revisada 2026-11-26): V1 agrega CHECKs extra no pedidos literalmente (IN de enums, email en minúsculas, patente en mayúsculas, currency='ARS', rangos min<=max). Se aceptaron como no bloqueantes porque derivan de la columna "Restricciones" de data-model.md.
- Los dos productores de V4 comparten el mismo hash BCrypt (misma sal). Aceptado: AC-SETUP-03 pasa; solo es observación.
- Entidades de `models`/`versions` se llaman `VehicleModel`/`VehicleVersion` (repos `VehicleModelRepository`/`VehicleVersionRepository`).
- Etapa 3 (revisada 2026-11-26): con Spring Boot 4 / Framework 7, `ProblemDetail.forStatusAndDetail` serializa SIN `"type"` (equivale a about:blank por RFC 9457), mientras que el entry point de seguridad lo escribe a mano. api.md §2 muestra `"type":"about:blank"`. Lo marqué menor (recomendado corregir en `ProblemDetails.of` y testear `$.type` en errores de negocio). Corregido en etapa 4: `ProblemDetails.of` setea `about:blank` y ClientSearchIT testea `$.type`.
- Etapa 3: errores 4xx del framework (404/405/415) salen como Problem Details sin `code` (desvío menor de backend.md §10 "cualquier otra → INTERNAL_ERROR"); frontend usa HttpContextToken `HANDLED_ERROR_CODES` para que el componente maneje códigos sin snackbar. Ambos aceptables.
- Etapa 4 (revisada 2026-11-26): AC-CLI-06 solo cubre la búsqueda; la parte `calculate` se movió a etapa 9 por decisión del usuario (acceptance.md §9). No marcarlo faltante en etapa 4; sí exigirlo en etapa 9.
- Etapa 4: bloques 2 y 3 de Nueva cotización se deshabilitan con `inert` + `aria-disabled` en el `mat-card` (aún vacíos). En etapas 5+ los controles reales deben deshabilitarse con `disabled()` en el schema (frontend.md §2.1); revisar.
- Error recurrente: agentes en Windows convierten archivos existentes LF a CRLF (app.routes.ts en etapa 4) -> diff de archivo completo. Chequear con `file` / `git diff --ignore-cr-at-eol --stat`.
- Etapa 5 (revisada 2026-11-26): alcance acordado = backend catálogos + frontend Bloque 2 SIN vaciado de dependientes (AC-FE-02), SIN mayúsculas en patente (AC-FE-08) y SIN labels.spec completo (AC-FE-07) -> todo eso exigirlo en etapa 6. Decisiones aceptadas: brands/models con año fuera de rango (2009) -> 200 lista vacía (no 400); years exige vehicleType sin cambiar rango; test del componente mockea CatalogApi con promesas inmediatas; todos los campos de vehículo `required` con "Completá este campo". Bloque 2 conserva `inert` en el mat-card además de `disabled()` en el schema (redundante, aceptado).
- `labels.ts` nace en etapa 4 sin `labels.spec.ts` (frontend.md §7 lo pide); el Record<Enum,string> lo garantiza en compilación. Menor; exigir el spec cuando labels esté completo.
