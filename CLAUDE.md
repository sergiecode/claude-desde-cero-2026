# Cotizador de seguros de vehículos

Sistema para que productores de una aseguradora coticen seguros de autos, motos, camionetas y camiones, y emitan la póliza a partir de la cotización. Proyecto educativo: todos los datos son ficticios.

## Documentación (fuente de verdad)

| Qué | Dónde |
| --- | --- |
| Decisiones del negocio y datos | `relevamiento.md` |
| Qué hace el sistema (reglas C1, V1, S1, P1, Q1, E1…) | `docs/manual-funcional.md` |
| Cómo se construye: índice de la SPEC | `docs/spec/README.md` |
| Criterios de aceptación y etapas | `docs/spec/acceptance.md` |

- Antes de implementar algo, leer `docs/spec/README.md` y la parte de la SPEC que corresponda.
- Si la SPEC y el manual se contradicen, o falta información para implementar, **detenerse y preguntar**. No suponer.
- No implementar nada que esté fuera de alcance (`docs/spec/README.md` §6).

## Stack

- `backend/`: Java 21 (versión del código; sirve cualquier JDK instalado del 21 al 25), Spring Boot 4.1, Maven, PostgreSQL 17, Flyway.
- `frontend/`: Angular 22, Angular Material, Signal Forms.
- PostgreSQL corre en Docker Compose; backend y frontend corren en local.

## Comandos

| Acción | Comando |
| --- | --- |
| Levantar la base | `docker compose up -d` |
| Reiniciar la base (borra los datos) | `docker compose down -v && docker compose up -d` |
| Backend | `cd backend && ./mvnw spring-boot:run` |
| Tests unitarios del backend | `cd backend && ./mvnw test` |
| Tests unitarios + integración del backend | `cd backend && ./mvnw verify` (necesita Docker) |
| Frontend | `cd frontend && npm start` |
| Tests del frontend | `cd frontend && npx ng test --watch=false` |

## Convenciones

- Código, nombres, endpoints, JSON y commits en **inglés**. Todo lo que ve el usuario en **español**.
- Dinero con `BigDecimal`; fechas con un `Clock` inyectable (zona `America/Argentina/Buenos_Aires`).
- El frontend no calcula precios ni sumas aseguradas; el backend nunca confía en valores calculados por el frontend.
- Los secretos están en `.env` (no se lee ni se sube). Las variables están documentadas en `docs/spec/README.md` §4.
- Las convenciones específicas de cada parte están en `.claude/rules/`.

## Cómo se trabaja

- El trabajo se hace **por etapas** (las 11 de `docs/spec/acceptance.md` §9) con `/implementar-etapa <n>`.
- Una etapa está terminada cuando pasan todos sus criterios de aceptación como tests.
- Un commit por etapa terminada, con el formato `feat(stage-<n>): <nombre en inglés>` y los criterios cubiertos en el cuerpo. Nunca hacer `git push` sin que lo pida el usuario.
- Agentes disponibles: `backend-dev`, `frontend-dev`, `test-runner`, `revisor-aceptacion` y `qa-e2e` (en `.claude/agents/`).