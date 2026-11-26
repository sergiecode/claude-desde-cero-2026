# Datos precargados

Datos ficticios que cargan las migraciones de Flyway (`backend.md` §8). Los ids se insertan explícitamente porque los criterios de aceptación los usan.

---

## 1. Tarifas — `V2__seed_tariffs.sql`

### 1.1. `tariff_zones`

| id | code | name | factor |
| --- | --- | --- | --- |
| 1 | ZONA_1 | CABA | 1.2000 |
| 2 | ZONA_2 | Gran Buenos Aires | 1.1500 |
| 3 | ZONA_3 | La Plata | 1.1000 |
| 4 | ZONA_4 | Córdoba Capital | 1.0500 |
| 5 | ZONA_5 | Rosario | 1.1000 |
| 6 | ZONA_6 | Interior de Buenos Aires | 1.0000 |
| 7 | ZONA_7 | Interior del país | 0.9500 |

### 1.2. `provinces`

| id | name |
| --- | --- |
| 1 | CABA |
| 2 | Buenos Aires |
| 3 | Córdoba |
| 4 | Santa Fe |
| 5 | Mendoza |

### 1.3. `cities`

| id | province_id | name | zone_id |
| --- | --- | --- | --- |
| 1 | 1 | CABA | 1 |
| 2 | 2 | Quilmes | 2 |
| 3 | 2 | La Matanza | 2 |
| 4 | 2 | San Isidro | 2 |
| 5 | 2 | La Plata | 3 |
| 6 | 2 | Mar del Plata | 6 |
| 7 | 3 | Córdoba | 4 |
| 8 | 3 | Villa María | 7 |
| 9 | 4 | Rosario | 5 |
| 10 | 5 | Mendoza | 7 |

### 1.4. `insurance_plans`

| id | code | name | description | monthly_rate | display_order |
| --- | --- | --- | --- | --- | --- |
| 1 | RC | RC — Responsabilidad civil | Daños a terceros | 0.0010 | 1 |
| 2 | TERCEROS_COMPLETO | Terceros completo | RC + robo e incendio totales y parciales + destrucción total + granizo + cristales | 0.0025 | 2 |
| 3 | TODO_RIESGO | Todo riesgo | Terceros completo + daños parciales al propio vehículo | 0.0045 | 3 |

### 1.5. `usage_factors`

| usage_type | factor |
| --- | --- |
| PARTICULAR | 1.0000 |
| COMERCIAL | 1.1000 |
| TAXI_REMIS | 1.4000 |
| TRANSPORTE_CARGA | 1.2500 |
| TRANSPORTE_PASAJEROS | 1.3500 |
| AGRICOLA | 1.0500 |
| DELIVERY | 1.3000 |

### 1.6. `allowed_usages`

| vehicle_type | usage_type |
| --- | --- |
| AUTO | PARTICULAR, COMERCIAL, TAXI_REMIS, DELIVERY |
| MOTO | PARTICULAR, COMERCIAL, DELIVERY |
| CAMIONETA | PARTICULAR, COMERCIAL, TRANSPORTE_CARGA, AGRICOLA, DELIVERY |
| CAMION | PARTICULAR, COMERCIAL, TRANSPORTE_CARGA, TRANSPORTE_PASAJEROS, AGRICOLA |

(Una fila por combinación: 17 filas en total.)

### 1.7. `vehicle_age_factors`

| id | min_years | max_years | factor |
| --- | --- | --- | --- |
| 1 | 0 | 2 | 1.0000 |
| 2 | 3 | 5 | 1.0500 |
| 3 | 6 | 10 | 1.1000 |
| 4 | 11 | NULL | 1.2000 |

### 1.8. `driver_age_factors`

| id | min_age | max_age | factor |
| --- | --- | --- | --- |
| 1 | 18 | 24 | 1.3000 |
| 2 | 25 | 29 | 1.1500 |
| 3 | 30 | 59 | 1.0000 |
| 4 | 60 | 69 | 1.0500 |
| 5 | 70 | NULL | 1.1500 |

---

## 2. Catálogo de vehículos — `V3__seed_vehicle_catalog.sql`

12 marcas, 24 modelos y 48 versiones. Cada modelo tiene una versión **Base** y una **Full** (valor Base × 1,15). Montos en ARS.

### 2.1. `brands`

| id | vehicle_type | name |
| --- | --- | --- |
| 1 | AUTO | Toyota |
| 2 | AUTO | Volkswagen |
| 3 | AUTO | Fiat |
| 4 | MOTO | Honda |
| 5 | MOTO | Yamaha |
| 6 | MOTO | Motomel |
| 7 | CAMIONETA | Toyota |
| 8 | CAMIONETA | Ford |
| 9 | CAMIONETA | Volkswagen |
| 10 | CAMION | Mercedes-Benz |
| 11 | CAMION | Iveco |
| 12 | CAMION | Scania |

### 2.2. `models`

| id | brand_id | name | available_from_year |
| --- | --- | --- | --- |
| 1 | 1 | Etios | 2010 |
| 2 | 1 | Corolla | 2010 |
| 3 | 2 | Gol | 2010 |
| 4 | 2 | Polo | 2018 |
| 5 | 3 | Cronos | 2018 |
| 6 | 3 | Argo | 2010 |
| 7 | 4 | Wave 110 | 2010 |
| 8 | 4 | CB 250 Twister | 2010 |
| 9 | 5 | YBR 125 | 2010 |
| 10 | 5 | FZ 25 | 2010 |
| 11 | 6 | Blitz 110 | 2010 |
| 12 | 6 | Skua 250 | 2010 |
| 13 | 7 | Hilux | 2010 |
| 14 | 7 | SW4 | 2010 |
| 15 | 8 | Ranger | 2010 |
| 16 | 8 | Maverick | 2022 |
| 17 | 9 | Amarok | 2010 |
| 18 | 9 | Saveiro | 2010 |
| 19 | 10 | Accelo | 2010 |
| 20 | 10 | Atego | 2010 |
| 21 | 11 | Tector | 2010 |
| 22 | 11 | Daily | 2010 |
| 23 | 12 | Serie P | 2010 |
| 24 | 12 | Serie R | 2010 |

### 2.3. `versions`

| id | model_id | name | reference_value |
| --- | --- | --- | --- |
| 1 | 1 | Base | 22.000.000,00 |
| 2 | 1 | Full | 25.300.000,00 |
| 3 | 2 | Base | 38.000.000,00 |
| 4 | 2 | Full | 43.700.000,00 |
| 5 | 3 | Base | 18.000.000,00 |
| 6 | 3 | Full | 20.700.000,00 |
| 7 | 4 | Base | 28.000.000,00 |
| 8 | 4 | Full | 32.200.000,00 |
| 9 | 5 | Base | 25.000.000,00 |
| 10 | 5 | Full | 28.750.000,00 |
| 11 | 6 | Base | 23.000.000,00 |
| 12 | 6 | Full | 26.450.000,00 |
| 13 | 7 | Base | 2.500.000,00 |
| 14 | 7 | Full | 2.875.000,00 |
| 15 | 8 | Base | 7.000.000,00 |
| 16 | 8 | Full | 8.050.000,00 |
| 17 | 9 | Base | 4.000.000,00 |
| 18 | 9 | Full | 4.600.000,00 |
| 19 | 10 | Base | 7.500.000,00 |
| 20 | 10 | Full | 8.625.000,00 |
| 21 | 11 | Base | 1.800.000,00 |
| 22 | 11 | Full | 2.070.000,00 |
| 23 | 12 | Base | 4.500.000,00 |
| 24 | 12 | Full | 5.175.000,00 |
| 25 | 13 | Base | 55.000.000,00 |
| 26 | 13 | Full | 63.250.000,00 |
| 27 | 14 | Base | 70.000.000,00 |
| 28 | 14 | Full | 80.500.000,00 |
| 29 | 15 | Base | 52.000.000,00 |
| 30 | 15 | Full | 59.800.000,00 |
| 31 | 16 | Base | 45.000.000,00 |
| 32 | 16 | Full | 51.750.000,00 |
| 33 | 17 | Base | 60.000.000,00 |
| 34 | 17 | Full | 69.000.000,00 |
| 35 | 18 | Base | 26.000.000,00 |
| 36 | 18 | Full | 29.900.000,00 |
| 37 | 19 | Base | 85.000.000,00 |
| 38 | 19 | Full | 97.750.000,00 |
| 39 | 20 | Base | 120.000.000,00 |
| 40 | 20 | Full | 138.000.000,00 |
| 41 | 21 | Base | 110.000.000,00 |
| 42 | 21 | Full | 126.500.000,00 |
| 43 | 22 | Base | 70.000.000,00 |
| 44 | 22 | Full | 80.500.000,00 |
| 45 | 23 | Base | 150.000.000,00 |
| 46 | 23 | Full | 172.500.000,00 |
| 47 | 24 | Base | 210.000.000,00 |
| 48 | 24 | Full | 241.500.000,00 |

---

## 3. Productores y clientes — `V4__seed_producers_and_clients.sql`

### 3.1. `producers`

| id | first_name | last_name | email | password | license_number | active |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Ana | Torres | ana.torres@aseguradora.test | `Demo2026!` (guardar hash BCrypt) | 12345 | true |
| 2 | Diego | Sosa | diego.sosa@aseguradora.test | `Demo2026!` (guardar hash BCrypt) | 67890 | true |

### 3.2. `clients`

| id | document_type | document_number | first_name | last_name | business_name | birth_date | city_id |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | DNI | 30123456 | Lucía | Fernández | — | 2004-03-15 | 1 (CABA) |
| 2 | DNI | 35987654 | Martín | Gómez | — | 1999-07-02 | 2 (Quilmes) |
| 3 | DNI | 25456789 | Carolina | Ruiz | — | 1981-05-10 | 7 (Córdoba) |
| 4 | DNI | 8765432 | Jorge | Pereyra | — | 1954-02-08 | 10 (Mendoza) |
| 5 | CUIT | 30712345671 | — | — | Transportes del Litoral S.A. | — | 9 (Rosario) |
| 6 | CUIT | 30709876542 | — | — | Agro Pampa S.R.L. | — | 5 (La Plata) |

Datos de contacto:

| id | email | phone | address | postal_code |
| --- | --- | --- | --- | --- |
| 1 | lucia.fernandez@mail.test | 11 5555-0101 | Av. Rivadavia 5000 | C1406 |
| 2 | martin.gomez@mail.test | 11 5555-0202 | Mitre 450 | B1878 |
| 3 | carolina.ruiz@mail.test | 351 555-0303 | Colón 1200 | X5000 |
| 4 | jorge.pereyra@mail.test | 261 555-0404 | San Martín 980 | M5500 |
| 5 | contacto@litoral.test | 341 555-0505 | Av. Pellegrini 1500 | S2000 |
| 6 | administracion@agropampa.test | 221 555-0606 | Calle 7 820 | B1900 |

"—" significa NULL. Los dos CUIT tienen dígito verificador válido.

Edades al 19/11/2026: Lucía 22, Martín 27, Carolina 45, Jorge 72.

---

## 4. Sin datos iniciales

`quotes`, `policies` y `number_sequences` empiezan vacías.