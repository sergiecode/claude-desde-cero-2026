# Backend

Java 21 · Spring Boot 4.1.x · Maven · PostgreSQL 17 · Flyway.

**JDK:** el código apunta a Java 21 (`java.version` = 21 en el `pom.xml`) y compila con cualquier JDK del 21 al 25. Si el JDK instalado es otro dentro de ese rango, se usa sin preguntar.

---

## 1. Proyecto

| Dato | Valor |
| --- | --- |
| Carpeta | `backend/` |
| groupId | `com.cotizador` |
| artifactId | `cotizador-backend` |
| Paquete raíz | `com.cotizador` |
| Puerto | 8080 |

### 1.1. Dependencias

Se agregan desde Spring Initializr (nombres exactos según Spring Boot 4.1):

- Spring Web (MVC)
- Spring Data JPA
- Validation
- Spring Security
- OAuth2 Resource Server (para validar JWT; también provee `JwtEncoder` con Nimbus)
- Flyway Migration + soporte de Flyway para PostgreSQL
- PostgreSQL Driver
- Testcontainers (PostgreSQL) para tests de integración
- springdoc-openapi (starter de Web MVC UI), versión compatible con Spring Boot 4.1

No se agregan otras librerías sin consultarlo.

---

## 2. Arquitectura en capas

```
Controller  →  Service  →  Repository  →  PostgreSQL
    ↑             ↑
   DTO        Entity + reglas de negocio
```

| Capa | Responsabilidad | No hace |
| --- | --- | --- |
| Controller | Recibe el request, valida el DTO (`@Valid`), llama al service, devuelve el DTO | Lógica de negocio, acceso a datos |
| Service | Reglas de negocio, cálculos, transacciones (`@Transactional`) | Conocer HTTP |
| Repository | Acceso a datos con Spring Data JPA | Lógica de negocio |
| Entity | Mapeo JPA de las tablas | Exponerse en el API |
| DTO | Contrato del API (`record` de Java) | Mapear tablas |

Las entidades **nunca** se devuelven en un controller. La conversión entidad ↔ DTO se hace en clases `*Mapper` escritas a mano.

### 2.1. Paquetes

```
com.cotizador
├── CotizadorApplication.java
├── config/          ← ClockConfig, OpenApiConfig, PricingProperties, JwtProperties
├── security/        ← SecurityConfig, JwtService, CurrentProducer, handlers de 401
├── controller/      ← AuthController, ClientController, CatalogController,
│                       InsurancePlanController, InsuredValueController,
│                       QuoteController, PolicyController
├── service/         ← AuthService, ClientService, CatalogService, ValuationService,
│                       PricingService, QuoteService, PolicyService, NumberSequenceService
├── validation/      ← DocumentValidator (DNI/CUIT), PatentValidator
├── repository/      ← un repositorio por entidad
├── entity/          ← entidades JPA y enums
├── dto/             ← records de request y response
├── mapper/          ← conversión entidad ↔ DTO
└── exception/       ← BusinessException, ErrorCode, GlobalExceptionHandler
```

---

## 3. Configuración (`application.yml`)

```yaml
spring:
  config:
    import: optional:file:../.env[.properties]   # .env de la raíz del repo
  datasource:
    url: jdbc:postgresql://localhost:5432/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true

app:
  timezone: America/Argentina/Buenos_Aires
  jwt:
    secret: ${JWT_SECRET}
    expiration: 8h
  pricing:
    tax-rate: 0.21
    company-driver-age-factor: 1.00
    quote-validity-days: 15
    min-vehicle-year: 2010
    min-client-age: 18
    depreciation-rate-per-year: 0.05
    depreciation-floor: 0.30
    insured-amount-min-ratio: 0.80
    insured-amount-max-ratio: 1.20
    max-start-date-days-ahead: 30
```

Los secretos y credenciales salen del `.env` de la raíz (ver `README.md` §4); no tienen valores por defecto en el código ni en `application.yml`. Los tests de integración no los necesitan: la base la provee Testcontainers y el secreto del JWT se define en la configuración de test.

Los parámetros `app.pricing.*` se leen con un `@ConfigurationProperties` (`PricingProperties`). Ningún valor de negocio queda escrito como literal en el código.

### 3.1. Reloj

`ClockConfig` declara un bean `Clock` con la zona `app.timezone`. Todos los services reciben el `Clock` por constructor y lo usan para obtener hoy (`LocalDate.now(clock)`), ahora (`OffsetDateTime.now(clock)`) y el año actual. Los tests reemplazan el bean por un `Clock.fixed(...)`.

---

## 4. Cálculos

### 4.1. Edad y antigüedad

Se calculan con la fecha de hoy según el `Clock` (regla P6):

- **Edad del cliente:** `Period.between(birthDate, today).getYears()`. Solo para DNI.
- **Antigüedad del vehículo:** `today.getYear() - manufacturingYear`.

### 4.2. Suma asegurada (`ValuationService`)

```
factorDepreciacion = max(1 − depreciationRatePerYear × antigüedad, depreciationFloor)
sugerido = redondearAMiles(referenceValue × factorDepreciacion)
mínimo   = redondearAMiles(sugerido × insuredAmountMinRatio)
máximo   = redondearAMiles(sugerido × insuredAmountMaxRatio)
```

`redondearAMiles(x)` = `x` dividido 1000, redondeado a entero con `HALF_UP`, multiplicado por 1000. Resultado con escala 2.

Si la versión no existe o su `reference_value` es NULL → `VALUATION_NOT_FOUND`.

### 4.3. Precio (`PricingService`)

```
basePremium  = round2(insuredAmount × planRate)        ← solo informativo
premium      = round2(insuredAmount × planRate × zoneFactor × usageFactor
                      × vehicleAgeFactor × driverAgeFactor)
taxes        = round2(premium × taxRate)
totalPrice   = premium + taxes
```

- `round2` = `setScale(2, RoundingMode.HALF_UP)`.
- `premium` se calcula multiplicando todo con precisión completa y se redondea **una sola vez al final**. No se usa `basePremium` (redondeado) como punto de partida.
- `zoneFactor`: el de la zona de la ciudad del cliente.
- `usageFactor`: de `usage_factors`.
- `vehicleAgeFactor`: la fila de `vehicle_age_factors` cuyo rango contiene la antigüedad.
- `driverAgeFactor`: si el cliente es DNI, la fila de `driver_age_factors` cuyo rango contiene la edad; si es CUIT, `app.pricing.company-driver-age-factor`.
- `taxRate`: `app.pricing.tax-rate`.

Si no se encuentra un factor (configuración incompleta) es un error interno (`INTERNAL_ERROR`) y se registra en el log.

---

## 5. Validaciones

### 5.1. Formato (en DTOs y validadores)

| Campo | Regla | Error | Regla del manual |
| --- | --- | --- | --- |
| Campos obligatorios | `@NotNull` / `@NotBlank` | `VALIDATION_ERROR` con "Completá este campo" | — |
| DNI | `^\d{7,8}$` | `INVALID_DOCUMENT` | C3 |
| CUIT | Se quitan los guiones; `^\d{11}$`; dígito verificador válido (5.2) | `INVALID_DOCUMENT` | C4 |
| Patente | Se pasa a mayúsculas y se quitan espacios; `^[A-Z]{3}\d{3}$` | `INVALID_PATENT` | V6 |
| Año | Entre `min-vehicle-year` y el año actual | `INVALID_YEAR` | V3 |
| Fecha de inicio | Entre hoy y hoy + `max-start-date-days-ahead` (inclusive) | `INVALID_START_DATE` | E4 |
| Suma asegurada | > 0 y con 2 decimales como máximo | `VALIDATION_ERROR` | — |

### 5.2. Dígito verificador del CUIT

```
pesos = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2]
suma  = Σ (dígito[i] × pesos[i])  para los primeros 10 dígitos
resto = 11 − (suma mod 11)
si resto == 11 → verificador = 0
si resto == 10 → CUIT inválido
si no          → verificador = resto
El CUIT es válido si el dígito 11 == verificador.
```

### 5.3. Negocio (en services), en este orden para `calculate` y `save`

1. El cliente existe (`CLIENT_NOT_FOUND`).
2. Si es DNI, tiene la edad mínima (`CLIENT_UNDERAGE`).
3. El año es válido (`INVALID_YEAR`).
4. La marca es del tipo indicado; el modelo es de esa marca y está disponible para el año; la versión es de ese modelo (`INVALID_VEHICLE`).
5. El tipo de uso está permitido para el tipo de vehículo (`USAGE_NOT_ALLOWED`).
6. La patente tiene formato válido (`INVALID_PATENT`).
7. El plan existe (`PLAN_NOT_FOUND`).
8. Hay valuación (`VALUATION_NOT_FOUND`) y la suma asegurada está dentro del rango (`INSURED_AMOUNT_OUT_OF_RANGE`).

---

## 6. Numeración (`NumberSequenceService`)

Formato: `PREFIJO-AAAA-NNNNNN`, por ejemplo `COT-2026-000001` (reglas Q7 y E9).

```
next(prefix):                                  ← dentro de la transacción del llamador
  year = año actual según el Clock
  SELECT last_value FROM number_sequences
    WHERE prefix = ? AND year = ? FOR UPDATE   ← bloquea la fila
  si no existe: INSERT (prefix, year, 1) → valor 1
               (si otro proceso la insertó antes: reintentar el SELECT FOR UPDATE)
  si existe:    UPDATE last_value = last_value + 1 → nuevo valor
  return prefix + "-" + year + "-" + valor con 6 dígitos y ceros a la izquierda
```

El año de la cotización es el de `created_at`; el de la póliza, el de `issued_at`.

---

## 7. Cotizaciones y pólizas

### 7.1. Guardar cotización (`QuoteService.save`)

En una transacción:

1. Ejecuta las validaciones de 5.3 y el cálculo completo (no usa ningún valor calculado por el frontend).
2. Genera el número con `NumberSequenceService.next("COT")`.
3. Guarda la cotización con todas las copias de valores (`data-model.md` §7.1), `status = CALCULATED`, `created_at = ahora` y `valid_until = created_at + quote-validity-days días`.

### 7.2. Vencimiento al consultar (reglas Q3 y §8 del manual)

No hay procesos programados. Al leer:

- **Cotización** (`GET /api/quotes`, `GET /api/quotes/{id}`, y al emitir): si `status = CALCULATED` y `ahora > valid_until` → se actualiza a `EXPIRED` y se guarda.
- **Póliza** (`GET /api/policies/{id}`): si `status = ACTIVE` y `hoy > end_date` → se actualiza a `EXPIRED` y se guarda.

Estos métodos de lectura son `@Transactional` (no `readOnly`) porque pueden escribir.

### 7.3. Emitir póliza (`PolicyService.issue`)

En una transacción:

1. Busca la cotización del productor con bloqueo pesimista (`@Lock(PESSIMISTIC_WRITE)`). Si no existe o es de otro productor → `QUOTE_NOT_FOUND`.
2. Aplica el vencimiento de 7.2.
3. Si `status = EMITTED` → `QUOTE_ALREADY_ISSUED` (con el número de la póliza existente en el mensaje).
4. Si `status = EXPIRED` → `QUOTE_EXPIRED`.
5. Valida la fecha de inicio (`INVALID_START_DATE`).
6. Genera el número con `NumberSequenceService.next("POL")`.
7. Crea la póliza: `status = ACTIVE`, `start_date` = la recibida, `end_date = start_date.plusYears(1)`, montos copiados de la cotización, `issued_at = ahora`.
8. Cambia la cotización a `EMITTED`.

La restricción `UNIQUE (quote_id)` en `policies` es la segunda línea de defensa: si se viola (`DataIntegrityViolationException`), se responde `QUOTE_ALREADY_ISSUED`.

### 7.4. Visibilidad (reglas Q5 y E10)

Todas las consultas de cotizaciones y pólizas filtran por el `producer_id` del token **en la consulta a la base** (por ejemplo `findByIdAndProducerId`), no después de leer. Lo que no pertenece al productor responde 404, igual que lo inexistente.

---

## 8. Migraciones (Flyway)

Ubicación: `backend/src/main/resources/db/migration/`.

| Archivo | Contenido |
| --- | --- |
| `V1__create_schema.sql` | Todas las tablas, restricciones e índices de `data-model.md` |
| `V2__seed_tariffs.sql` | Zonas, provincias, ciudades, planes, factores de uso, usos permitidos, factores de antigüedad y de edad |
| `V3__seed_vehicle_catalog.sql` | Marcas, modelos y versiones |
| `V4__seed_producers_and_clients.sql` | Productores y clientes |

- Los datos salen de `seed-data.md`, incluidos los ids, que se insertan explícitamente (`OVERRIDING SYSTEM VALUE`). Al final de cada migración se ajustan las secuencias de identidad para que el próximo id sea el siguiente al último insertado.
- Las contraseñas se guardan como hash BCrypt. La contraseña en texto plano no va en el SQL.
- **El hash de `Demo2026!` nunca se escribe a mano ni se copia de un ejemplo.** Se genera **ejecutando** `BCryptPasswordEncoder` (por ejemplo, con un test descartable o `jshell` con el jar de Spring Security en el classpath), se pega en `V4` y se verifica con el criterio AC-SETUP-03 (`encoder.matches("Demo2026!", hash)` para cada productor cargado).
- Una migración aplicada **nunca se modifica**: los cambios van en una migración nueva.

---

## 9. Seguridad

- Sesión **stateless**; CSRF deshabilitado (API con token).
- `POST /api/auth/login`, `/v3/api-docs/**` y `/swagger-ui/**` son públicos. El resto requiere token.
- Login: busca el productor por email (en minúsculas), verifica `active` y la contraseña con BCrypt. Cualquier falla → `INVALID_CREDENTIALS`.
- Token: JWT firmado con HS256 usando `app.jwt.secret`. `sub` = id del productor. Expira según `app.jwt.expiration`.
- `CurrentProducer`: componente que devuelve el id del productor autenticado a partir del token.
- Sin token, con token inválido o vencido → `401 UNAUTHORIZED` en formato Problem Details.

---

## 10. Manejo de errores

- `BusinessException(ErrorCode code, Object... args)`: excepción única para errores de negocio. `ErrorCode` es un enum con el estado HTTP y el mensaje en español de `api.md` §2.1, con marcadores para los valores variables (año actual, montos, número de póliza).
- `GlobalExceptionHandler` (`@RestControllerAdvice`) convierte:
  - `BusinessException` → su código y mensaje.
  - Errores de `@Valid` → `VALIDATION_ERROR` con `errors` por campo.
  - Parámetros faltantes o mal tipados → `VALIDATION_ERROR`.
  - `DataIntegrityViolationException` al emitir → `QUOTE_ALREADY_ISSUED`.
  - Cualquier otra excepción → `INTERNAL_ERROR`, con el detalle solo en el log.

---

## 11. Tests

| Tipo | Herramientas | Qué cubre |
| --- | --- | --- |
| Unitarios | JUnit 5 + Mockito + AssertJ | `PricingService`, `ValuationService`, `DocumentValidator`, `PatentValidator`, `NumberSequenceService` (formato), reglas de vencimiento |
| Integración | `@SpringBootTest` + MockMvc + Testcontainers (PostgreSQL 17) | Endpoints completos con los datos de las migraciones |

- Todos los tests usan un `Clock` fijo: **2026-11-19T10:00:00-03:00**, salvo que el criterio indique otra fecha.
- Cada criterio de `acceptance.md` marcado como **B** tiene al menos un test con su código en el nombre del método, con guiones bajos (por ejemplo `AC_PRICE_01_autoInCabaYoungDriver()`).
- **Nombres de clases:** tests unitarios terminan en `Test` y los corre Surefire (`./mvnw test`). Tests de integración terminan en `IT` y los corre Failsafe (`./mvnw verify`).
- Los tests de integración necesitan Docker corriendo.