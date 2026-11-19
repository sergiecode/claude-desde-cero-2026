# SPEC — Cotizador de seguros de vehículos

Versión 1.0 — 19/11/2026

Esta carpeta describe **cómo se construye** el sistema. **Qué hace** el sistema está en `docs/manual-funcional.md`, y los datos del negocio en `relevamiento.md`.

**Si la SPEC y el manual funcional se contradicen, o falta información para implementar algo, no se supone nada: se detiene el trabajo y se pregunta.**

---

## 1. Índice

| Archivo | Contenido | Cuándo leerlo |
| --- | --- | --- |
| `README.md` | Stack, estructura del repositorio, cómo levantar el proyecto, convenciones | Siempre, primero |
| `data-model.md` | Tablas, columnas, enums y relaciones | Al tocar entidades, repositorios o migraciones |
| `api.md` | Endpoints, requests, responses y errores. **Contrato único entre backend y frontend** | Al tocar controllers, DTOs o servicios HTTP del frontend |
| `backend.md` | Arquitectura, seguridad, cálculo, validaciones, numeración, transacciones, tests | Al trabajar en `backend/` |
| `frontend.md` | Pantallas, componentes, formularios, navegación, tests | Al trabajar en `frontend/` |
| `seed-data.md` | Datos precargados para las migraciones de Flyway | Al escribir las migraciones de datos |
| `acceptance.md` | Criterios de aceptación con resultado esperado | Al escribir tests y al verificar una etapa |

---

## 2. Stack

| Capa | Tecnología | Versión |
| --- | --- | --- |
| Backend | Java | 21 |
| Backend | Spring Boot | 4.1.x (última versión estable al 19/11/2026) |
| Backend | Build | Maven (con Maven Wrapper `./mvnw`) |
| Base de datos | PostgreSQL | 17 (imagen Docker `postgres:17`) |
| Migraciones | Flyway | La que gestiona Spring Boot 4.1 |
| Documentación API | springdoc-openapi | Versión compatible con Spring Boot 4.1 |
| Frontend | Angular | 22 |
| Frontend | Angular Material | 22 |
| Frontend | TypeScript | La requerida por Angular 22 (6.0.x) |
| Frontend | Node.js | 22.22.3 o superior (o 24.15.0 o superior) |
| Contenedores | Docker Compose | Solo para PostgreSQL |

El backend se genera con **Spring Initializr** y el frontend con **Angular CLI**. Los nombres exactos de los starters de Spring Boot 4.1 se toman de Spring Initializr, no de memoria.

---

## 3. Estructura del repositorio

Un solo repositorio:

```
cotizador/
├── CLAUDE.md
├── docker-compose.yml
├── relevamiento.md
├── docs/
│   ├── manual-funcional.md
│   └── spec/
│       ├── README.md
│       ├── data-model.md
│       ├── api.md
│       ├── backend.md
│       ├── frontend.md
│       ├── seed-data.md
│       └── acceptance.md
├── backend/          ← proyecto Maven (Spring Boot)
└── frontend/         ← proyecto Angular
```

La estructura interna de cada proyecto está en `backend.md` y `frontend.md`.

---

## 4. Cómo se levanta

| Paso | Comando (desde la raíz) | Resultado |
| --- | --- | --- |
| 1. Base de datos | `docker compose up -d` | PostgreSQL en `localhost:5432` |
| 2. Backend | `cd backend && ./mvnw spring-boot:run` | API en `http://localhost:8080`. Flyway crea las tablas y carga los datos al arrancar |
| 3. Frontend | `cd frontend && npm install && npm start` | App en `http://localhost:4200` |

**`docker-compose.yml`**: un único servicio `postgres` (imagen `postgres:17`), base `cotizador`, usuario `cotizador`, contraseña `cotizador`, puerto `5432`, con un volumen con nombre para persistir los datos.

**Swagger UI:** `http://localhost:8080/swagger-ui.html`.

**Frontend → backend:** el frontend usa un proxy de desarrollo (`proxy.conf.json`) que redirige `/api` a `http://localhost:8080`. No hace falta configurar CORS.

**Reiniciar los datos:** `docker compose down -v` y volver a levantar. Flyway recrea todo.

---

## 5. Convenciones generales

### 5.1. Idioma

| Elemento | Idioma |
| --- | --- |
| Código, nombres de clases, variables, tablas, columnas, endpoints, JSON, enums, commits | Inglés |
| Todo lo que ve el usuario: textos de pantalla, etiquetas, mensajes de error (`detail` de los errores del API) | Español |

Los enums viajan por el API como código en inglés (por ejemplo `TAXI_REMIS`). El frontend los traduce a español con los mapas de `frontend.md`.

### 5.2. Dinero

- Siempre `BigDecimal` en Java, `NUMERIC(15,2)` en la base y `number` en el JSON.
- Moneda única: `ARS`.
- Redondeo: 2 decimales, `RoundingMode.HALF_UP`, solo en los puntos que indica `backend.md`.
- En pantalla, formato es-AR: `$ 46.332,00`.

### 5.3. Fechas y hora

- Zona horaria del negocio: `America/Argentina/Buenos_Aires`.
- "Hoy" y "ahora" se obtienen siempre de un `java.time.Clock` inyectable, nunca de `LocalDate.now()` sin argumentos. Así los tests pueden fijar la fecha.
- En el JSON: fechas `YYYY-MM-DD` y fecha-hora ISO-8601 con offset (`2026-11-19T10:00:00-03:00`).
- En pantalla: `dd/MM/yyyy` y `dd/MM/yyyy HH:mm`.

### 5.4. Trazabilidad con el manual

Las reglas del manual tienen código (C1, V4, S2, Q3, E2…). Donde una validación o un test implementa una regla, se menciona el código en un comentario o en el nombre del test. Ejemplo: `shouldRejectExpiredQuote_E3()`.

### 5.5. Calidad

- El frontend **no calcula** precios ni sumas aseguradas: solo muestra lo que devuelve el backend.
- El backend nunca confía en valores calculados por el frontend: recalcula siempre.
- Las contraseñas nunca se guardan ni se registran en logs en texto plano.

---

## 6. Fuera de alcance técnico

- Endpoints de alta, modificación o baja de productores, clientes y catálogos.
- Endpoints de coberturas sueltas y de provincias o localidades.
- Pagos, PDF, emails, anulación de pólizas.
- Paginación de listados (el volumen de datos es chico).
- Tests end-to-end del frontend.
- Despliegue a un servidor: solo se ejecuta en local.
