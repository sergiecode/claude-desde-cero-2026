---
paths:
  - "backend/src/main/resources/db/migration/**"
---

# Migraciones de Flyway

- **Una migración que ya se aplicó nunca se modifica.** Cualquier cambio de esquema o de datos va en una migración nueva con el número siguiente (`V5__...`, `V6__...`).
- Nombres: `V<número>__<descripcion_en_ingles>.sql`, con doble guion bajo.
- El esquema sale de `docs/spec/data-model.md` y los datos de `docs/spec/seed-data.md`, incluidos los ids, que se insertan con `OVERRIDING SYSTEM VALUE`. Al final de cada migración de datos, ajustar las secuencias de identidad.
- Las contraseñas se guardan como hash BCrypt. Nunca la contraseña en texto plano.
- Durante el desarrollo, si una migración todavía no aplicada en ningún otro lado necesita corregirse, preguntar antes de editarla y reiniciar la base con `docker compose down -v`.
