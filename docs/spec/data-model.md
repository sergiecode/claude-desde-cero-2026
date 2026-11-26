# Modelo de datos

Todas las tablas se crean con migraciones de Flyway (ver `backend.md` §8). JPA solo valida el esquema (`ddl-auto: validate`), nunca lo crea.

Convenciones:

- Nombres de tablas en plural y `snake_case`; entidades Java en singular y `PascalCase`.
- Clave primaria `id BIGINT GENERATED ALWAYS AS IDENTITY`, salvo que se indique otra cosa.
- Dinero: `NUMERIC(15,2)`. Tasas y factores: `NUMERIC(6,4)`.
- Enums: se guardan como `VARCHAR` con el nombre del enum (`@Enumerated(EnumType.STRING)`).

---

## 1. Diagrama

```
producers ───────────────┐
                         ▼
clients ──► cities ──► provinces          quotes ──────────► policies
   │          │                            │  │  │               (1 a 0..1)
   │          └──► tariff_zones            │  │  └──► insurance_plans
   └──────────────────────────────────────►│  └─────► versions ──► models ──► brands
                                           └────────► producers

Tablas de tarifa:  tariff_zones · usage_factors · allowed_usages · vehicle_age_factors · driver_age_factors
Numeración:        number_sequences
```

---

## 2. Enums

```java
public enum DocumentType { DNI, CUIT }

public enum VehicleType { AUTO, MOTO, CAMIONETA, CAMION }

public enum VehicleUsageType {
    PARTICULAR, COMERCIAL, TAXI_REMIS, TRANSPORTE_CARGA,
    TRANSPORTE_PASAJEROS, AGRICOLA, DELIVERY
}

public enum QuoteStatus { CALCULATED, EMITTED, EXPIRED }

public enum PolicyStatus { ACTIVE, EXPIRED }
```

---

## 3. Seguridad y personas

### 3.1. `producers`

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK |
| first_name | VARCHAR(100) | NOT NULL |
| last_name | VARCHAR(100) | NOT NULL |
| email | VARCHAR(150) | NOT NULL, UNIQUE (guardado en minúsculas) |
| password_hash | VARCHAR(100) | NOT NULL (BCrypt) |
| license_number | VARCHAR(20) | NOT NULL, UNIQUE |
| active | BOOLEAN | NOT NULL, DEFAULT TRUE |

Un productor con `active = false` no puede ingresar.

### 3.2. `clients`

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK |
| document_type | VARCHAR(10) | NOT NULL (`DocumentType`) |
| document_number | VARCHAR(11) | NOT NULL, solo dígitos (CUIT sin guiones) |
| first_name | VARCHAR(100) | Obligatorio si es DNI, NULL si es CUIT |
| last_name | VARCHAR(100) | Obligatorio si es DNI, NULL si es CUIT |
| business_name | VARCHAR(200) | Obligatorio si es CUIT, NULL si es DNI |
| birth_date | DATE | Obligatorio si es DNI, NULL si es CUIT |
| email | VARCHAR(150) | NOT NULL |
| phone | VARCHAR(30) | NOT NULL |
| address | VARCHAR(200) | NOT NULL |
| postal_code | VARCHAR(10) | NOT NULL |
| city_id | BIGINT | NOT NULL, FK → `cities` |

- UNIQUE (`document_type`, `document_number`).
- CHECK: si `document_type = 'DNI'` entonces `first_name`, `last_name` y `birth_date` no son NULL y `business_name` es NULL. Si es `'CUIT'`, al revés.

---

## 4. Ubicación y zonas

### 4.1. `tariff_zones`

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK |
| code | VARCHAR(10) | NOT NULL, UNIQUE (`ZONA_1` … `ZONA_7`) |
| name | VARCHAR(100) | NOT NULL |
| factor | NUMERIC(6,4) | NOT NULL, > 0 |

### 4.2. `provinces`

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK |
| name | VARCHAR(100) | NOT NULL, UNIQUE |

### 4.3. `cities`

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK |
| province_id | BIGINT | NOT NULL, FK → `provinces` |
| name | VARCHAR(100) | NOT NULL |
| zone_id | BIGINT | NOT NULL, FK → `tariff_zones` |

UNIQUE (`province_id`, `name`).

---

## 5. Catálogo de vehículos

### 5.1. `brands`

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK |
| vehicle_type | VARCHAR(20) | NOT NULL (`VehicleType`) |
| name | VARCHAR(100) | NOT NULL |

UNIQUE (`vehicle_type`, `name`). Una misma marca que fabrica varios tipos (por ejemplo Toyota auto y camioneta) tiene una fila por tipo.

### 5.2. `models`

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK |
| brand_id | BIGINT | NOT NULL, FK → `brands` |
| name | VARCHAR(100) | NOT NULL |
| available_from_year | INTEGER | NOT NULL, ≥ 2010 |

UNIQUE (`brand_id`, `name`). Un modelo está disponible para un año si `available_from_year <= año`.

### 5.3. `versions`

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK |
| model_id | BIGINT | NOT NULL, FK → `models` |
| name | VARCHAR(100) | NOT NULL |
| reference_value | NUMERIC(15,2) | NULL permitido (sin valuación → regla S6) |

UNIQUE (`model_id`, `name`). `reference_value` es el valor 0 km de referencia en ARS.

---

## 6. Planes y tarifas

### 6.1. `insurance_plans`

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK |
| code | VARCHAR(30) | NOT NULL, UNIQUE (`RC`, `TERCEROS_COMPLETO`, `TODO_RIESGO`) |
| name | VARCHAR(100) | NOT NULL (texto visible, en español) |
| description | VARCHAR(300) | NOT NULL (texto visible, en español) |
| monthly_rate | NUMERIC(6,4) | NOT NULL, > 0 (expresada como fracción: 0,25 % = `0.0025`) |
| display_order | INTEGER | NOT NULL |

### 6.2. `usage_factors`

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| usage_type | VARCHAR(30) | PK (`VehicleUsageType`) |
| factor | NUMERIC(6,4) | NOT NULL, > 0 |

### 6.3. `allowed_usages`

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| vehicle_type | VARCHAR(20) | PK compuesta |
| usage_type | VARCHAR(30) | PK compuesta, FK → `usage_factors` |

Solo existen filas para las combinaciones permitidas.

### 6.4. `vehicle_age_factors`

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK |
| min_years | INTEGER | NOT NULL, ≥ 0 |
| max_years | INTEGER | NULL = sin tope |
| factor | NUMERIC(6,4) | NOT NULL, > 0 |

Los rangos son inclusivos y no se superponen.

### 6.5. `driver_age_factors`

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK |
| min_age | INTEGER | NOT NULL, ≥ 18 |
| max_age | INTEGER | NULL = sin tope |
| factor | NUMERIC(6,4) | NOT NULL, > 0 |

Los rangos son inclusivos y no se superponen. El factor para empresas (CUIT) no está en esta tabla: es un parámetro de configuración (`backend.md` §4).

---

## 7. Cotizaciones y pólizas

### 7.1. `quotes`

Una cotización guarda **todos los valores usados en el cálculo** (regla Q2). Si después cambian las tarifas o el catálogo, la cotización no cambia. El vehículo no es una entidad aparte: sus datos viven en la cotización.

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK |
| quote_number | VARCHAR(20) | NOT NULL, UNIQUE (`COT-2026-000001`) |
| producer_id | BIGINT | NOT NULL, FK → `producers` |
| client_id | BIGINT | NOT NULL, FK → `clients` |
| vehicle_type | VARCHAR(20) | NOT NULL |
| manufacturing_year | INTEGER | NOT NULL |
| brand_id | BIGINT | NOT NULL, FK → `brands` |
| model_id | BIGINT | NOT NULL, FK → `models` |
| version_id | BIGINT | NOT NULL, FK → `versions` |
| brand_name | VARCHAR(100) | NOT NULL (copia) |
| model_name | VARCHAR(100) | NOT NULL (copia) |
| version_name | VARCHAR(100) | NOT NULL (copia) |
| patent | VARCHAR(6) | NOT NULL, mayúsculas |
| usage_type | VARCHAR(30) | NOT NULL |
| plan_id | BIGINT | NOT NULL, FK → `insurance_plans` |
| plan_name | VARCHAR(100) | NOT NULL (copia) |
| plan_rate | NUMERIC(6,4) | NOT NULL (copia) |
| insured_amount | NUMERIC(15,2) | NOT NULL |
| zone_code | VARCHAR(10) | NOT NULL (copia). El nombre de la zona no se copia: para mostrarlo se lee de `tariff_zones` por este código |
| zone_factor | NUMERIC(6,4) | NOT NULL (copia) |
| usage_factor | NUMERIC(6,4) | NOT NULL (copia) |
| vehicle_age_years | INTEGER | NOT NULL |
| vehicle_age_factor | NUMERIC(6,4) | NOT NULL (copia) |
| driver_age | INTEGER | NULL si el cliente es empresa |
| driver_age_factor | NUMERIC(6,4) | NOT NULL (copia) |
| base_premium | NUMERIC(15,2) | NOT NULL (suma asegurada × tasa, solo informativo, redondeado) |
| premium | NUMERIC(15,2) | NOT NULL (prima mensual) |
| tax_rate | NUMERIC(6,4) | NOT NULL (copia) |
| taxes | NUMERIC(15,2) | NOT NULL |
| total_price | NUMERIC(15,2) | NOT NULL |
| currency | VARCHAR(3) | NOT NULL, `ARS` |
| status | VARCHAR(20) | NOT NULL (`QuoteStatus`) |
| created_at | TIMESTAMPTZ | NOT NULL |
| valid_until | TIMESTAMPTZ | NOT NULL (`created_at` + 15 días) |

Índice: (`producer_id`, `created_at` DESC).

### 7.2. `policies`

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK |
| policy_number | VARCHAR(20) | NOT NULL, UNIQUE (`POL-2026-000001`) |
| quote_id | BIGINT | NOT NULL, **UNIQUE**, FK → `quotes` (garantiza la regla E2) |
| producer_id | BIGINT | NOT NULL, FK → `producers` |
| client_id | BIGINT | NOT NULL, FK → `clients` |
| start_date | DATE | NOT NULL |
| end_date | DATE | NOT NULL (`start_date` + 1 año) |
| insured_amount | NUMERIC(15,2) | NOT NULL (copia de la cotización) |
| premium | NUMERIC(15,2) | NOT NULL (copia) |
| taxes | NUMERIC(15,2) | NOT NULL (copia) |
| total_price | NUMERIC(15,2) | NOT NULL (copia) |
| currency | VARCHAR(3) | NOT NULL, `ARS` |
| status | VARCHAR(20) | NOT NULL (`PolicyStatus`) |
| issued_at | TIMESTAMPTZ | NOT NULL |

Los datos del vehículo y del plan de la póliza se leen de su cotización (que nunca se modifica).

### 7.3. `number_sequences`

Correlativos que vuelven a 1 cada año (reglas Q7 y E9).

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| prefix | VARCHAR(5) | PK compuesta (`COT`, `POL`) |
| year | INTEGER | PK compuesta |
| last_value | INTEGER | NOT NULL |

El uso de esta tabla está en `backend.md` §6.