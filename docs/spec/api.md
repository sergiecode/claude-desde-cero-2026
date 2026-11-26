# API REST

**Contrato único entre backend y frontend.** Si un endpoint necesita cambiar, se cambia primero acá.

---

## 1. Convenciones

- Base: `/api`. JSON con nombres en `camelCase`.
- Autenticación: header `Authorization: Bearer <token>` en todos los endpoints salvo `POST /api/auth/login`.
- El productor que hace el pedido se obtiene **siempre del token**, nunca de un parámetro.
- Enums: viajan como código en inglés (`AUTO`, `TAXI_REMIS`, `CALCULATED`).
- Dinero: número con 2 decimales (`46332.00`). Fechas: `YYYY-MM-DD`. Fecha-hora: ISO-8601 con offset.
- Listados: ordenados como se indica en cada endpoint, sin paginación.

---

## 2. Formato de error

Todos los errores usan **Problem Details (RFC 9457)**, con dos campos extra: `code` y `errors`.

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "La patente debe tener el formato ABC123",
  "code": "INVALID_PATENT",
  "errors": [
    { "field": "patent", "message": "La patente debe tener el formato ABC123" }
  ]
}
```

- `detail`: mensaje en español para mostrar al usuario (tomado del manual §9).
- `code`: código estable en inglés para que el frontend decida qué hacer.
- `errors`: solo en errores de validación de campos; lista vacía en el resto.

### 2.1. Códigos de error

| code | HTTP | detail | Regla |
| --- | --- | --- | --- |
| `INVALID_CREDENTIALS` | 401 | Usuario o contraseña incorrectos | — |
| `UNAUTHORIZED` | 401 | La sesión expiró. Ingresá nuevamente | — |
| `VALIDATION_ERROR` | 400 | Revisá los datos ingresados | Campos obligatorios: `message` = "Completá este campo" |
| `INVALID_DOCUMENT` | 400 | El número de documento no es válido | C3, C4 |
| `CLIENT_NOT_FOUND` | 404 | Cliente no encontrado | C1 |
| `CLIENT_UNDERAGE` | 422 | El cliente debe ser mayor de 18 años | C5 |
| `INVALID_YEAR` | 400 | El año debe estar entre 2010 y [año actual] | V3 |
| `INVALID_VEHICLE` | 400 | La marca, el modelo o la versión no corresponden al tipo y año elegidos | V4 |
| `USAGE_NOT_ALLOWED` | 400 | El tipo de uso no está permitido para este vehículo | V5 |
| `INVALID_PATENT` | 400 | La patente debe tener el formato ABC123 | V6 |
| `VALUATION_NOT_FOUND` | 404 | No hay valuación disponible para este vehículo | S6 |
| `INSURED_AMOUNT_OUT_OF_RANGE` | 400 | La suma asegurada debe estar entre $ [mínimo] y $ [máximo] | S5 |
| `PLAN_NOT_FOUND` | 400 | El plan seleccionado no existe | P1 |
| `QUOTE_NOT_FOUND` | 404 | Cotización no encontrada | Q5 |
| `QUOTE_EXPIRED` | 409 | La cotización está vencida. Generá una nueva | E3 |
| `QUOTE_ALREADY_ISSUED` | 409 | Esta cotización ya fue emitida (póliza [número]) | E2 |
| `INVALID_START_DATE` | 400 | La fecha de inicio debe estar entre hoy y los próximos 30 días | E4 |
| `POLICY_NOT_FOUND` | 404 | Póliza no encontrada | E10 |
| `INTERNAL_ERROR` | 500 | Ocurrió un error inesperado. Intentá nuevamente | — |

Los montos dentro de los mensajes usan formato es-AR: `$ 8.640.000,00`.

**Errores del framework** (ruta inexistente `404`, método no permitido `405`, tipo de contenido no soportado `415`): conservan su status y salen en formato Problem Details, **sin `code`**. No son errores de negocio y el frontend los trata como un error genérico (snackbar con el `detail`).

**JSON mal formado o campos faltantes** en cualquier request, incluido el login: `400 VALIDATION_ERROR`.

---

## 3. Autenticación

### `POST /api/auth/login`

Sin token.

Request:
```json
{ "email": "ana.torres@aseguradora.test", "password": "Demo2026!" }
```

Response `200`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": "2026-11-19T18:00:00-03:00",
  "producer": { "id": 1, "firstName": "Ana", "lastName": "Torres", "email": "ana.torres@aseguradora.test" }
}
```

Errores: `401 INVALID_CREDENTIALS` (email inexistente, contraseña incorrecta o productor inactivo, siempre el mismo mensaje).

### `GET /api/auth/me`

Response `200`: el objeto `producer` del login.

---

## 4. Clientes

### `GET /api/clients/search?documentType=DNI&documentNumber=30123456`

El CUIT se acepta con o sin guiones.

Response `200` (persona física):
```json
{
  "id": 1,
  "documentType": "DNI",
  "documentNumber": "30123456",
  "firstName": "Lucía",
  "lastName": "Fernández",
  "businessName": null,
  "birthDate": "2004-03-15",
  "age": 22,
  "email": "lucia.fernandez@mail.test",
  "phone": "11 5555-0101",
  "address": "Av. Rivadavia 5000",
  "postalCode": "C1406",
  "city": "CABA",
  "province": "CABA"
}
```

Para una empresa: `firstName`, `lastName`, `birthDate` y `age` son `null`, y `businessName` tiene valor.

Errores: `400 INVALID_DOCUMENT`, `404 CLIENT_NOT_FOUND`, `422 CLIENT_UNDERAGE`.

---

## 5. Catálogos

Todos responden `200` con una lista (vacía si no hay resultados).

| Endpoint | Response | Orden |
| --- | --- | --- |
| `GET /api/catalogs/vehicle-types` | `["AUTO","MOTO","CAMIONETA","CAMION"]` | Fijo, como el enum |
| `GET /api/catalogs/years?vehicleType=AUTO` | `[2026, 2025, …, 2010]` | Descendente |
| `GET /api/catalogs/brands?vehicleType=AUTO&year=2020` | `[{ "id": 3, "name": "Fiat" }, { "id": 1, "name": "Toyota" }, { "id": 2, "name": "Volkswagen" }]` | Por nombre |
| `GET /api/catalogs/models?vehicleType=AUTO&year=2020&brandId=2` | `[{ "id": 3, "name": "Gol" }, { "id": 4, "name": "Polo" }]` | Por nombre |
| `GET /api/catalogs/versions?modelId=3` | `[{ "id": 5, "name": "Base" }, { "id": 6, "name": "Full" }]` | Por id |
| `GET /api/catalogs/usage-types?vehicleType=AUTO` | `["PARTICULAR","COMERCIAL","TAXI_REMIS","DELIVERY"]` | Orden del enum |

Reglas de filtrado (V4):

- **years:** desde 2010 hasta el año actual, para todos los tipos.
- **brands:** marcas del tipo que tienen al menos un modelo con `available_from_year <= year`.
- **models:** modelos de esa marca (que debe ser del tipo indicado) con `available_from_year <= year`.
- **versions:** versiones del modelo.
- **usage-types:** los de `allowed_usages` para el tipo.

Parámetros faltantes o inválidos → `400 VALIDATION_ERROR`.

---

## 6. Planes

### `GET /api/insurance-plans`

Response `200`, ordenado por `display_order`:
```json
[
  { "id": 1, "code": "RC", "name": "RC — Responsabilidad civil", "description": "Daños a terceros" },
  { "id": 2, "code": "TERCEROS_COMPLETO", "name": "Terceros completo", "description": "RC + robo e incendio totales y parciales + destrucción total + granizo + cristales" },
  { "id": 3, "code": "TODO_RIESGO", "name": "Todo riesgo", "description": "Terceros completo + daños parciales al propio vehículo" }
]
```

La tasa no se expone en este endpoint.

---

## 7. Suma asegurada

### `GET /api/insured-values?versionId=5&year=2018`

Response `200`:
```json
{ "minimumAmount": 8640000.00, "suggestedAmount": 10800000.00, "maximumAmount": 12960000.00, "currency": "ARS" }
```

Errores: `404 VALUATION_NOT_FOUND` (versión inexistente o sin `reference_value`), `400 INVALID_YEAR`.

---

## 8. Cotizaciones

### 8.1. Request común (`QuoteRequest`)

Lo usan `POST /api/quotes/calculate` y `POST /api/quotes`.

```json
{
  "clientId": 1,
  "vehicleType": "AUTO",
  "manufacturingYear": 2018,
  "brandId": 2,
  "modelId": 3,
  "versionId": 5,
  "usageType": "PARTICULAR",
  "patent": "abc123",
  "planId": 2,
  "insuredAmount": 10800000.00
}
```

Todos los campos son obligatorios. La patente se normaliza a mayúsculas sin espacios antes de validar.

### 8.2. `POST /api/quotes/calculate`

Calcula **sin guardar nada**.

Response `200` (`QuoteCalculation`):
```json
{
  "insuredAmount": 10800000.00,
  "planRate": 0.0025,
  "basePremium": 27000.00,
  "zone": { "code": "ZONA_1", "name": "CABA", "factor": 1.20 },
  "usageFactor": 1.00,
  "vehicleAgeYears": 8,
  "vehicleAgeFactor": 1.10,
  "driverAge": 22,
  "driverAgeFactor": 1.30,
  "premium": 46332.00,
  "taxRate": 0.21,
  "taxes": 9729.72,
  "totalPrice": 56061.72,
  "currency": "ARS"
}
```

Errores: `400 VALIDATION_ERROR`, `404 CLIENT_NOT_FOUND`, `422 CLIENT_UNDERAGE`, `400 INVALID_YEAR`, `400 INVALID_VEHICLE`, `400 USAGE_NOT_ALLOWED`, `400 INVALID_PATENT`, `404 VALUATION_NOT_FOUND`, `400 INSURED_AMOUNT_OUT_OF_RANGE`, `400 PLAN_NOT_FOUND`.

### 8.3. `POST /api/quotes`

Recalcula con los mismos datos y **guarda** la cotización en estado `CALCULATED`.

Response `201` con header `Location: /api/quotes/{id}` y el `QuoteDetail` (8.5). Mismos errores que `calculate`.

### 8.4. `GET /api/quotes`

Cotizaciones del productor del token, ordenadas por `createdAt` descendente. Antes de responder, actualiza a `EXPIRED` las vencidas (ver `backend.md` §7).

Response `200`:
```json
[
  {
    "id": 1,
    "quoteNumber": "COT-2026-000001",
    "createdAt": "2026-11-19T10:00:00-03:00",
    "clientName": "Lucía Fernández",
    "vehicleType": "AUTO",
    "brandName": "Volkswagen",
    "modelName": "Gol",
    "planName": "Terceros completo",
    "totalPrice": 56061.72,
    "currency": "ARS",
    "status": "CALCULATED"
  }
]
```

`clientName`: "Nombre Apellido" para DNI, razón social para CUIT.

### 8.5. `GET /api/quotes/{id}`

Response `200` (`QuoteDetail`):
```json
{
  "id": 1,
  "quoteNumber": "COT-2026-000001",
  "status": "CALCULATED",
  "createdAt": "2026-11-19T10:00:00-03:00",
  "validUntil": "2026-12-04T10:00:00-03:00",
  "client": { "...": "mismo objeto que GET /api/clients/search" },
  "vehicle": {
    "vehicleType": "AUTO",
    "manufacturingYear": 2018,
    "brandName": "Volkswagen",
    "modelName": "Gol",
    "versionName": "Base",
    "patent": "ABC123",
    "usageType": "PARTICULAR"
  },
  "planName": "Terceros completo",
  "calculation": { "...": "mismo objeto que QuoteCalculation (8.2)" },
  "policy": null
}
```

Si la cotización está emitida, `policy` = `{ "id": 1, "policyNumber": "POL-2026-000001" }`.

La edad del cliente (`client.age`) se muestra como se guardó al cotizar (`driverAge`), no recalculada.

Errores: `404 QUOTE_NOT_FOUND` (no existe o es de otro productor).

### 8.6. `POST /api/quotes/{id}/issue`

Emite la póliza.

Request:
```json
{ "startDate": "2026-11-20" }
```

Response `201` con header `Location: /api/policies/{id}` y el `PolicyDetail` (9.1).

Errores: `404 QUOTE_NOT_FOUND`, `409 QUOTE_EXPIRED`, `409 QUOTE_ALREADY_ISSUED`, `400 INVALID_START_DATE`, `400 VALIDATION_ERROR` (falta `startDate`).

---

## 9. Pólizas

### 9.1. `GET /api/policies/{id}`

Antes de responder, actualiza a `EXPIRED` si corresponde (ver `backend.md` §7).

Response `200` (`PolicyDetail`):
```json
{
  "id": 1,
  "policyNumber": "POL-2026-000001",
  "status": "ACTIVE",
  "issuedAt": "2026-11-19T10:05:00-03:00",
  "startDate": "2026-11-20",
  "endDate": "2027-11-20",
  "quoteId": 1,
  "quoteNumber": "COT-2026-000001",
  "client": { "...": "mismo objeto que GET /api/clients/search" },
  "vehicle": { "...": "mismo objeto que en QuoteDetail" },
  "planName": "Terceros completo",
  "insuredAmount": 10800000.00,
  "premium": 46332.00,
  "taxes": 9729.72,
  "totalPrice": 56061.72,
  "currency": "ARS"
}
```

Errores: `404 POLICY_NOT_FOUND` (no existe o pertenece a otro productor).

---

## 10. Endpoints del documento inicial que no se implementan

| Endpoint | Motivo |
| --- | --- |
| `POST /api/users`, `GET /api/users`, `GET /api/users/{id}` | Los productores vienen precargados |
| `GET /api/coverages?vehicleType=` | Las coberturas son solo la descripción del plan |
| `GET /api/insurance-plans?vehicleType=`, `GET /api/insurance-plans/{id}` | Los planes son los mismos para todos los tipos; alcanza con el listado |