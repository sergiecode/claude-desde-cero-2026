---
paths:
  - "backend/**"
---

# Convenciones del backend

Antes de modificar el backend, leer `docs/spec/backend.md`, `docs/spec/data-model.md` y `docs/spec/api.md`.

## Lo que no se negocia

- **Capas clásicas:** controller → service → repository. Las entidades nunca salen de un controller; los DTOs son `record` y se convierten con mappers escritos a mano.
- **Paquetes por capa** bajo `com.cotizador` (`controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `validation`, `exception`, `security`, `config`).
- **Fechas:** siempre con el `Clock` inyectado (`LocalDate.now(clock)`). Nunca `now()` sin argumentos.
- **Dinero:** `BigDecimal` con `RoundingMode.HALF_UP`, redondeando solo donde lo indica `backend.md` §4.
- **Parámetros de negocio** (tasas, días, porcentajes) en `application.yml` bajo `app.pricing.*`, nunca como literales en el código.
- **Errores:** siempre `BusinessException` con un `ErrorCode` de `api.md` §2.1; los mensajes al usuario en español.
- **Visibilidad:** cotizaciones y pólizas se filtran por el productor del token **en la consulta** (`findByIdAndProducerId`).
- **Recalcular siempre:** el backend no confía en importes que vengan del frontend.
- **Contrato:** los endpoints y JSON son exactamente los de `api.md`. Si hace falta cambiar algo, se cambia primero `api.md` y se avisa.

## Tests

- Unitarios terminan en `Test` (`./mvnw test`); integración termina en `IT` (`./mvnw verify`, con Testcontainers).
- El nombre del método incluye el código del criterio: `AC_PRICE_01_...`.
- Todos los tests usan un `Clock` fijo (`2026-11-19T10:00:00-03:00` salvo que el criterio indique otra fecha).
- No agregar dependencias que no estén en `backend.md` §1.1 sin preguntar.
