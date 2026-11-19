# Criterios de aceptación

Cada criterio tiene un resultado esperado concreto. Una etapa se considera terminada cuando **todos sus criterios pasan como tests automáticos**.

- **Fecha de referencia:** salvo que se indique otra, todos los criterios se evalúan con el reloj fijo en **2026-11-19T10:00:00-03:00**.
- **Datos:** los de `seed-data.md`. Los ids entre paréntesis son los de esos datos.
- **Capa:** B = test de backend (unitario o integración) · F = test unitario de frontend.
- **Productor por defecto:** Ana Torres (id 1), salvo que se indique otro.

---

## 1. Precio (manual §7)

| ID | Capa | Dado | Resultado esperado | Reglas |
| --- | --- | --- | --- | --- |
| AC-PRICE-01 | B | Cliente Lucía Fernández (1, 22 años, CABA) · Auto VW Gol Base 2018 (versión 5) · Particular · Terceros completo (2) · suma asegurada 10.800.000,00 | basePremium 27.000,00 · zona ZONA_1 1,20 · uso 1,00 · antigüedad 8 años 1,10 · edad 22 años 1,30 · **premium 46.332,00** · taxes 9.729,72 · **totalPrice 56.061,72** | §7.7 ejemplo 1 |
| AC-PRICE-02 | B | Cliente Transportes del Litoral S.A. (5, Rosario) · Camión Iveco Tector Base 2014 (versión 41) · Transporte de carga · RC (1) · suma asegurada 50.000.000,00 | basePremium 50.000,00 · zona ZONA_5 1,10 · uso 1,25 · antigüedad 12 años 1,20 · driverAge null, factor 1,00 · **premium 82.500,00** · taxes 17.325,00 · **totalPrice 99.825,00** | §7.7 ejemplo 2 |
| AC-PRICE-03 | B | Cliente Martín Gómez (2, 27 años, Quilmes) · Moto Honda Wave 110 Base 2024 (versión 13) · Delivery · Todo riesgo (3) · suma asegurada 2.250.000,00 | zona ZONA_2 1,15 · uso 1,30 · antigüedad 2 años 1,00 · edad 27 años 1,15 · **premium 17.407,41** · taxes 3.655,56 · **totalPrice 21.062,97** | Redondeo solo al final (§7.1) |
| AC-PRICE-04 | B | Cliente Jorge Pereyra (4, 72 años, Mendoza) · Camioneta Ford Ranger Base 2010 (versión 29) · Agrícola · Terceros completo (2) · suma asegurada 15.600.000,00 | zona ZONA_7 0,95 · uso 1,05 · antigüedad 16 años 1,20 · edad 72 años 1,15 · **premium 53.685,45** · taxes 11.273,94 · **totalPrice 64.959,39** | Factores máximos de edad y antigüedad |
| AC-PRICE-05 | B | Mismos datos que AC-PRICE-01 llamando a `calculate` dos veces | Las dos respuestas son idénticas y no se guardó ninguna cotización | Q1 |

## 2. Suma asegurada (S1–S6)

| ID | Capa | Dado | Resultado esperado | Reglas |
| --- | --- | --- | --- | --- |
| AC-VAL-01 | B | `GET /insured-values?versionId=5&year=2018` | mínimo 8.640.000,00 · sugerido 10.800.000,00 · máximo 12.960.000,00 | S2, S3 |
| AC-VAL-02 | B | `GET /insured-values?versionId=41&year=2014` | mínimo 35.200.000,00 · sugerido 44.000.000,00 · máximo 52.800.000,00 | S2, S3 |
| AC-VAL-03 | B | `GET /insured-values?versionId=29&year=2010` (16 años) | sugerido 15.600.000,00 (piso del 30 %) · mínimo 12.480.000,00 · máximo 18.720.000,00 | S2 |
| AC-VAL-04 | B | Datos de AC-PRICE-01 con suma asegurada 8.639.000,00 | `400 INSURED_AMOUNT_OUT_OF_RANGE` con "La suma asegurada debe estar entre $ 8.640.000,00 y $ 12.960.000,00" | S5 |
| AC-VAL-05 | B | Datos de AC-PRICE-01 con suma asegurada 12.960.000,00 (el máximo exacto) | `200` | S5 |
| AC-VAL-06 | B | Una versión con `reference_value` NULL (insertada solo en el test) | `404 VALUATION_NOT_FOUND` | S6 |

## 3. Cliente (C1–C7)

| ID | Capa | Dado | Resultado esperado | Reglas |
| --- | --- | --- | --- | --- |
| AC-CLI-01 | B | Buscar DNI 30123456 | `200`, Lucía Fernández, edad 22, ciudad CABA | C1 |
| AC-CLI-02 | B | Buscar CUIT `30-71234567-1` (con guiones) | `200`, Transportes del Litoral S.A., `birthDate` y `age` null | C2, C4 |
| AC-CLI-03 | B | Buscar DNI 99999999 | `404 CLIENT_NOT_FOUND` | C1 |
| AC-CLI-04 | B | Buscar DNI `123` | `400 INVALID_DOCUMENT` | C3 |
| AC-CLI-05 | B | Buscar CUIT `30712345672` (verificador incorrecto) | `400 INVALID_DOCUMENT` | C4 |
| AC-CLI-06 | B | Un cliente DNI nacido el 2008-11-20 (17 años, insertado solo en el test) | Búsqueda y `calculate` → `422 CLIENT_UNDERAGE` | C5 |
| AC-CLI-07 | B | El mismo cliente con el reloj en 2026-11-20 | Búsqueda `200`, edad 18 | C5 |

## 4. Vehículo y catálogos (V1–V7)

| ID | Capa | Dado | Resultado esperado | Reglas |
| --- | --- | --- | --- | --- |
| AC-VEH-01 | B | `GET /catalogs/years?vehicleType=MOTO` | 17 años: de 2026 a 2010, descendente | V3 |
| AC-VEH-02 | B | `GET /catalogs/models?vehicleType=AUTO&year=2015&brandId=2` | Solo Gol (Polo existe desde 2018) | V4 |
| AC-VEH-03 | B | `GET /catalogs/models?vehicleType=CAMIONETA&year=2020&brandId=8` | Solo Ranger (Maverick existe desde 2022) | V4 |
| AC-VEH-04 | B | `GET /catalogs/brands?vehicleType=AUTO&year=2015` | Fiat, Toyota, Volkswagen (Fiat aparece por Argo) | V4 |
| AC-VEH-05 | B | `GET /catalogs/usage-types?vehicleType=MOTO` | PARTICULAR, COMERCIAL, DELIVERY | V5 |
| AC-VEH-06 | B | Datos de AC-PRICE-01 con año 2009 | `400 INVALID_YEAR` | V3 |
| AC-VEH-07 | B | Datos de AC-PRICE-03 con uso TAXI_REMIS | `400 USAGE_NOT_ALLOWED` | V5 |
| AC-VEH-08 | B | Datos de AC-PRICE-01 con versión 13 (de otro modelo) | `400 INVALID_VEHICLE` | V4 |
| AC-VEH-09 | B | Datos de AC-PRICE-01 con modelo Polo (4), versión 7, año 2015 | `400 INVALID_VEHICLE` | V4 |
| AC-VEH-10 | B | Patente `AB123CD` | `400 INVALID_PATENT` | V6 |
| AC-VEH-11 | B | Patente ` abc123 ` | Se acepta y se guarda como `ABC123` | V6 |

## 5. Cotización (Q1–Q7)

| ID | Capa | Dado | Resultado esperado | Reglas |
| --- | --- | --- | --- | --- |
| AC-QUO-01 | B | Guardar con los datos de AC-PRICE-01 (sin cotizaciones previas) | `201`, número `COT-2026-000001`, estado CALCULATED, validUntil `2026-12-04T10:00:00-03:00`, mismos importes que AC-PRICE-01 | Q2, Q3, Q7 |
| AC-QUO-02 | B | Guardar una segunda cotización | Número `COT-2026-000002` | Q7 |
| AC-QUO-03 | B | Guardar una cotización con el reloj en 2027-01-02 (después de AC-QUO-02) | Número `COT-2027-000001` | Q7 |
| AC-QUO-04 | B | Guardar una cotización, cambiar el factor de la ZONA_1 a 1,50 en la base y leer la cotización | Los importes y `zone.factor` siguen siendo los originales | Q2 |
| AC-QUO-05 | B | Cotización guardada; reloj en 2026-12-04T10:00:00-03:00; leerla | Estado CALCULATED (todavía vigente) | Q3 |
| AC-QUO-06 | B | Misma cotización; reloj en 2026-12-04T10:00:01-03:00; leerla | Estado EXPIRED, guardado en la base | Q3, §8.1 |
| AC-QUO-07 | B | Ana tiene 2 cotizaciones y Diego 1; Ana pide `GET /quotes` | Recibe solo sus 2, la más reciente primero | Q5 |
| AC-QUO-08 | B | Diego pide `GET /quotes/{id}` de una cotización de Ana | `404 QUOTE_NOT_FOUND` | Q5 |
| AC-QUO-09 | B | `POST /quotes` con un `totalPrice` agregado al body con otro valor | Se ignora; los importes guardados son los recalculados | §5.5 README |

## 6. Emisión y póliza (E1–E10)

| ID | Capa | Dado | Resultado esperado | Reglas |
| --- | --- | --- | --- | --- |
| AC-POL-01 | B | Cotización de AC-QUO-01; emitir con startDate 2026-11-20 | `201`, número `POL-2026-000001`, ACTIVE, endDate 2027-11-20, importes de la cotización; la cotización pasa a EMITTED | E1, E5, E6, E8, E9 |
| AC-POL-02 | B | Emitir con startDate 2026-11-19 (hoy) | `201` | E4 |
| AC-POL-03 | B | Emitir con startDate 2026-12-19 (hoy + 30) | `201` | E4 |
| AC-POL-04 | B | Emitir con startDate 2026-11-18 (ayer) | `400 INVALID_START_DATE` | E4 |
| AC-POL-05 | B | Emitir con startDate 2026-12-20 (hoy + 31) | `400 INVALID_START_DATE` | E4 |
| AC-POL-06 | B | Emitir de nuevo la cotización de AC-POL-01 | `409 QUOTE_ALREADY_ISSUED` con "Esta cotización ya fue emitida (póliza POL-2026-000001)" | E2 |
| AC-POL-07 | B | Dos pedidos de emisión simultáneos de la misma cotización | Uno `201` y el otro `409 QUOTE_ALREADY_ISSUED`; existe una sola póliza | E2 |
| AC-POL-08 | B | Cotización vencida (reloj después de `valid_until`); emitir | `409 QUOTE_EXPIRED`; la cotización queda EXPIRED | E3 |
| AC-POL-09 | B | Diego emite una cotización de Ana | `404 QUOTE_NOT_FOUND` | E10 |
| AC-POL-10 | B | Diego pide la póliza de AC-POL-01 | `404 POLICY_NOT_FOUND` | E10 |
| AC-POL-11 | B | Póliza de AC-POL-01; reloj en 2027-11-20 | Estado ACTIVE | §8.2 |
| AC-POL-12 | B | Póliza de AC-POL-01; reloj en 2027-11-21 | Estado EXPIRED | §8.2 |
| AC-POL-13 | B | Leer la cotización de AC-POL-01 | `policy` con el id de la póliza y `policyNumber` "POL-2026-000001" | §5.3 |
| AC-POL-14 | B | Cotización emitida; reloj después de su `valid_until`; leerla | Sigue EMITTED (nunca pasa a vencida) | §8.1 |

## 7. Autenticación

| ID | Capa | Dado | Resultado esperado |
| --- | --- | --- | --- |
| AC-AUTH-01 | B | Login con `ana.torres@aseguradora.test` / `Demo2026!` | `200` con token y datos de Ana |
| AC-AUTH-02 | B | Login con `ANA.TORRES@aseguradora.test` / `Demo2026!` | `200` (el email no distingue mayúsculas) |
| AC-AUTH-03 | B | Login con contraseña incorrecta | `401 INVALID_CREDENTIALS` |
| AC-AUTH-04 | B | Login de un productor con `active = false` (modificado solo en el test) | `401 INVALID_CREDENTIALS` |
| AC-AUTH-05 | B | `GET /api/quotes` sin token | `401 UNAUTHORIZED` |

## 8. Frontend

| ID | Capa | Dado | Resultado esperado | Reglas |
| --- | --- | --- | --- | --- |
| AC-FE-01 | F | Nueva cotización sin cliente encontrado | Bloques de vehículo y plan deshabilitados | C1 |
| AC-FE-02 | F | Marca, modelo y versión elegidos; se cambia la marca | Modelo, versión y suma asegurada quedan vacíos | V4 |
| AC-FE-03 | F | Formulario completo sin cotizar | "Guardar cotización" no se muestra | Q1 |
| AC-FE-04 | F | Cotizado; se cambia la patente | El resultado se borra y "Guardar cotización" se oculta | Q1 |
| AC-FE-05 | F | Detalle de cotización con estado EMITTED o EXPIRED | "Emitir póliza" deshabilitado | E1 |
| AC-FE-06 | F | Desglose con `premium` 46332 y `totalPrice` 56061.72 | Muestra "$ 46.332,00" y "$ 56.061,72" | §7.1 |
| AC-FE-07 | F | `labels.ts` | Todos los códigos de todos los enums tienen etiqueta en español | — |
| AC-FE-08 | F | Patente escrita `abc123` | El campo muestra `ABC123` | V6 |

---

## 9. Criterios por etapa

| Etapa (documento inicial) | Criterios |
| --- | --- |
| 1–2. Proyecto, entidades y base | El backend arranca, Flyway aplica V1–V4 y `ddl-auto: validate` no falla |
| 3. Usuarios y autenticación | AC-AUTH-01 a 05 |
| 4. Clientes | AC-CLI-01 a 07 |
| 5–6. Catálogos y formularios | AC-VEH-01 a 05 · AC-FE-01, 02, 07, 08 |
| 7–8. Planes, suma asegurada y valuaciones | AC-VAL-01 a 03 y 06 |
| 9. Tarifas y cálculo | AC-PRICE-01 a 05 · AC-VAL-04, 05 · AC-VEH-06 a 11 · AC-FE-06 |
| 10. Cotizaciones | AC-QUO-01 a 09 · AC-FE-03, 04 |
| 11. Emisión de pólizas | AC-POL-01 a 14 · AC-FE-05 |
