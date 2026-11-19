# Relevamiento — Cotizador de seguros de vehículos

| | |
| --- | --- |
| Negocio | Aseguradora (Argentina) |
| Sistema | Cotizador de seguros de vehículos con emisión de póliza |
| Tecnología | Frontend Angular · Backend Java (Spring Boot) · PostgreSQL · API REST/JSON |
| Fecha del relevamiento | 19/11/2026 |
| Fuentes | Documento funcional inicial del negocio + sesión de preguntas y respuestas |
| Siguiente paso | `docs/manual-funcional.md` |

> Todos los datos, montos, tasas y factores de este documento son **ficticios** y se usan solo con fines educativos.

---

## 1. Contexto

La aseguradora necesita que sus **productores de seguros** puedan cotizar un seguro para un vehículo de un cliente y, si el cliente acepta, emitir la póliza a partir de esa cotización.

El cotizador es **uno solo para cuatro tipos de vehículo**: AUTO, MOTO, CAMIONETA y CAMION. Las opciones disponibles en pantalla cambian según el tipo elegido.

Lo central del sistema es **el cotizador**. Usuarios, clientes y catálogos de vehículos vienen precargados y no se administran desde el sistema.

---

## 2. Decisiones tomadas en el relevamiento

| # | Tema | Decisión |
| --- | --- | --- |
| 1 | Tipos de vehículo | AUTO, MOTO, CAMIONETA y CAMION en el mismo cotizador; las condiciones cambian según el tipo |
| 2 | Datos iniciales | Productores, clientes, catálogos de vehículos, valuaciones, ubicaciones y planes vienen precargados |
| 3 | Fuera de alcance | Pagos, PDF, envío de emails, anulación de pólizas, alta y edición de usuarios y clientes |
| 4 | Pólizas | Solo creación (emisión) y consulta |
| 5 | Cálculo | Reglas inventadas para el ejercicio, explicadas paso a paso |
| 6 | Factores de precio | Ubicación, tipo de uso, antigüedad del vehículo y edad del conductor |
| 7 | Impuestos | Un único porcentaje ficticio: 21 % sobre la prima |
| 8 | Periodicidad | La prima es **mensual** |
| 9 | Redondeo | 2 decimales |
| 10 | Plan | Un desplegable con 3 planes (RC, Terceros completo, Todo riesgo), igual para los 4 tipos |
| 11 | Franquicia y adicionales | No se usan |
| 12 | Tipo de cliente | DNI = persona física · CUIT = empresa |
| 13 | Edad mínima | 18 años para personas físicas |
| 14 | Búsqueda de cliente | Por documento: si existe se autocompleta (solo lectura); si no existe, no se puede continuar |
| 15 | Validación de formatos | Sí (DNI, CUIT, patente) |
| 16 | Patente | Obligatoria, formato ABC123 |
| 17 | Tipos de uso | Se filtran según el tipo de vehículo |
| 18 | Conductor | Es el mismo cliente; para empresas el factor de edad es 1,00 |
| 19 | Vigencia de cotización | 15 días |
| 20 | Estados de cotización | CALCULATED, EMITTED, EXPIRED (se elimina DRAFT) |
| 21 | Edición de cotización | No se edita; si algo cambia se crea una nueva |
| 22 | Estado de póliza al emitir | ACTIVE (todo lo relacionado con el pago se considera resuelto) |
| 23 | Estados de póliza | ACTIVE y EXPIRED (se eliminan PENDING_PAYMENT y CANCELLED) |
| 24 | Vigencia de póliza | 1 año |
| 25 | Fecha de inicio | Elegida al emitir; no se permite una fecha pasada |
| 26 | Roles | Solo productor |
| 27 | Visibilidad | Cada productor ve solo sus cotizaciones y pólizas |
| 28 | Flujo de pantallas | "Cotizar" calcula sin guardar · "Guardar cotización" la registra · "Emitir póliza" desde el detalle de la cotización |
| 29 | Numeración | Cotización `COT-AAAA-NNNNNN` · Póliza `POL-AAAA-NNNNNN`; correlativo que vuelve a 1 cada año (`AAAA` = año de creación) |
| 30 | Fecha de inicio máxima | Hasta 30 días hacia adelante |
| 31 | Edad y antigüedad | Se calculan con la fecha de la cotización |
| 32 | Cómputo de la vigencia de la cotización | 15 días corridos desde que se guarda |
| 33 | Paso a vencida | La cotización o la póliza cambia de estado cuando se consulta después de su vencimiento (sin proceso automático) |
| 34 | Fin de la póliza | Mismo día y mes del año siguiente (del 20/11/2026 al 20/11/2027) |
| 35 | Valor por año del vehículo | Un único valor 0 km por versión; la diferencia por año la da la depreciación del 5 % anual |

---

## 3. Cambios respecto del documento inicial

| Documento inicial | Queda así |
| --- | --- |
| El productor carga los datos del cliente a mano | Los clientes vienen precargados; el productor solo busca por documento |
| La provincia se elige antes que la localidad | No hay selección: la ubicación sale del domicilio del cliente |
| Alta y consulta de usuarios | Los productores vienen precargados; solo existe el login |
| Coberturas individuales por tipo de vehículo | Un plan elegido de un desplegable; las coberturas son solo la descripción del plan |
| Estado DRAFT de cotización | Eliminado |
| Estados PENDING_PAYMENT y CANCELLED de póliza | Eliminados |
| Ejemplo de cálculo solo con factor geográfico | Cálculo completo con 4 factores, tasa del plan e impuestos |

---

## 4. Datos relevados

### 4.1. Tipos de uso por tipo de vehículo

| Tipo de uso | AUTO | MOTO | CAMIONETA | CAMION |
| --- | --- | --- | --- | --- |
| Particular | Sí | Sí | Sí | Sí |
| Comercial | Sí | Sí | Sí | Sí |
| Taxi / remís | Sí | No | No | No |
| Transporte de carga | No | No | Sí | Sí |
| Transporte de pasajeros | No | No | No | Sí |
| Agrícola | No | No | Sí | Sí |
| Delivery | Sí | Sí | Sí | No |

### 4.2. Planes

| Plan | Qué cubre (descripción) | Tasa mensual sobre la suma asegurada |
| --- | --- | --- |
| RC — Responsabilidad civil | Daños a terceros | 0,10 % |
| Terceros completo | RC + robo e incendio totales y parciales + destrucción total + granizo + cristales | 0,25 % |
| Todo riesgo | Terceros completo + daños parciales al propio vehículo | 0,45 % |

### 4.3. Factores de cálculo

**Factor geográfico** (según la localidad del cliente)

| Zona | Ubicación | Factor |
| --- | --- | --- |
| ZONA_1 | CABA | 1,20 |
| ZONA_2 | Gran Buenos Aires | 1,15 |
| ZONA_3 | La Plata | 1,10 |
| ZONA_4 | Córdoba Capital | 1,05 |
| ZONA_5 | Rosario | 1,10 |
| ZONA_6 | Interior de Buenos Aires | 1,00 |
| ZONA_7 | Interior del país | 0,95 |

**Factor de uso**

| Tipo de uso | Factor |
| --- | --- |
| Particular | 1,00 |
| Comercial | 1,10 |
| Agrícola | 1,05 |
| Transporte de carga | 1,25 |
| Delivery | 1,30 |
| Transporte de pasajeros | 1,35 |
| Taxi / remís | 1,40 |

**Factor de antigüedad** (antigüedad = año actual − año de fabricación)

| Antigüedad | Factor |
| --- | --- |
| 0 a 2 años | 1,00 |
| 3 a 5 años | 1,05 |
| 6 a 10 años | 1,10 |
| 11 años o más | 1,20 |

**Factor de edad del conductor** (el conductor es el cliente)

| Edad | Factor |
| --- | --- |
| 18 a 24 años | 1,30 |
| 25 a 29 años | 1,15 |
| 30 a 59 años | 1,00 |
| 60 a 69 años | 1,05 |
| 70 años o más | 1,15 |
| Empresa (CUIT) | 1,00 |

**Impuestos:** 21 % sobre la prima mensual.

### 4.4. Valuación (suma asegurada)

Cada versión de vehículo tiene un **valor 0 km de referencia**. A partir de él:

- **Sugerido** = valor 0 km × (1 − 5 % × antigüedad). Nunca baja del 30 % del valor 0 km.
- **Mínimo** = 80 % del sugerido.
- **Máximo** = 120 % del sugerido.
- Los tres valores se redondean a miles de pesos.

---

## 5. Datos precargados

### 5.1. Productores

| Nombre | Email (usuario) | Contraseña de demo | Matrícula |
| --- | --- | --- | --- |
| Ana Torres | ana.torres@aseguradora.test | Demo2026! | 12345 |
| Diego Sosa | diego.sosa@aseguradora.test | Demo2026! | 67890 |

Dos productores permiten mostrar que cada uno ve solo sus cotizaciones.

### 5.2. Clientes

| Documento | Cliente | Nacimiento | Edad* | Localidad (provincia) | Zona |
| --- | --- | --- | --- | --- | --- |
| DNI 30123456 | Lucía Fernández | 15/03/2004 | 22 | CABA (CABA) | ZONA_1 |
| DNI 35987654 | Martín Gómez | 02/07/1999 | 27 | Quilmes (Buenos Aires) | ZONA_2 |
| DNI 25456789 | Carolina Ruiz | 10/05/1981 | 45 | Córdoba (Córdoba) | ZONA_4 |
| DNI 8765432 | Jorge Pereyra | 08/02/1954 | 72 | Mendoza (Mendoza) | ZONA_7 |
| CUIT 30-71234567-1 | Transportes del Litoral S.A. | — | — | Rosario (Santa Fe) | ZONA_5 |
| CUIT 30-70987654-2 | Agro Pampa S.R.L. | — | — | La Plata (Buenos Aires) | ZONA_3 |

\* Edad al 19/11/2026. Los clientes cubren las 4 franjas de edad más frecuentes y 6 de las 7 zonas.

Todos tienen además email, teléfono, domicilio y código postal ficticios.

### 5.3. Ubicaciones

| Provincia | Localidad | Zona |
| --- | --- | --- |
| CABA | CABA | ZONA_1 |
| Buenos Aires | Quilmes | ZONA_2 |
| Buenos Aires | La Matanza | ZONA_2 |
| Buenos Aires | San Isidro | ZONA_2 |
| Buenos Aires | La Plata | ZONA_3 |
| Buenos Aires | Mar del Plata | ZONA_6 |
| Córdoba | Córdoba | ZONA_4 |
| Córdoba | Villa María | ZONA_7 |
| Santa Fe | Rosario | ZONA_5 |
| Mendoza | Mendoza | ZONA_7 |

### 5.4. Catálogo de vehículos

3 marcas × 2 modelos por tipo. Cada modelo tiene 2 versiones: **Base** (valor de la tabla) y **Full** (valor Base × 1,15). En total: 24 modelos y 48 versiones.

Salvo que se indique otra cosa, cada modelo está disponible para todos los años desde 2010 hasta el año actual.

| Tipo | Marca | Modelo | Valor 0 km versión Base (ARS) | Disponible desde |
| --- | --- | --- | --- | --- |
| AUTO | Toyota | Etios | 22.000.000 | 2010 |
| AUTO | Toyota | Corolla | 38.000.000 | 2010 |
| AUTO | Volkswagen | Gol | 18.000.000 | 2010 |
| AUTO | Volkswagen | Polo | 28.000.000 | 2018 |
| AUTO | Fiat | Cronos | 25.000.000 | 2018 |
| AUTO | Fiat | Argo | 23.000.000 | 2010 |
| MOTO | Honda | Wave 110 | 2.500.000 | 2010 |
| MOTO | Honda | CB 250 Twister | 7.000.000 | 2010 |
| MOTO | Yamaha | YBR 125 | 4.000.000 | 2010 |
| MOTO | Yamaha | FZ 25 | 7.500.000 | 2010 |
| MOTO | Motomel | Blitz 110 | 1.800.000 | 2010 |
| MOTO | Motomel | Skua 250 | 4.500.000 | 2010 |
| CAMIONETA | Toyota | Hilux | 55.000.000 | 2010 |
| CAMIONETA | Toyota | SW4 | 70.000.000 | 2010 |
| CAMIONETA | Ford | Ranger | 52.000.000 | 2010 |
| CAMIONETA | Ford | Maverick | 45.000.000 | 2022 |
| CAMIONETA | Volkswagen | Amarok | 60.000.000 | 2010 |
| CAMIONETA | Volkswagen | Saveiro | 26.000.000 | 2010 |
| CAMION | Mercedes-Benz | Accelo | 85.000.000 | 2010 |
| CAMION | Mercedes-Benz | Atego | 120.000.000 | 2010 |
| CAMION | Iveco | Tector | 110.000.000 | 2010 |
| CAMION | Iveco | Daily | 70.000.000 | 2010 |
| CAMION | Scania | Serie P | 150.000.000 | 2010 |
| CAMION | Scania | Serie R | 210.000.000 | 2010 |

Los modelos con "Disponible desde" posterior a 2010 sirven para mostrar que el catálogo se filtra por año.

---

## 6. Fuera de alcance

- Pagos y medios de pago
- Generación de PDF de la cotización o de la póliza
- Envío de emails
- Anulación, endosos o renovación de pólizas
- Alta, edición o baja de productores, clientes y catálogos
- Comparación de planes o coberturas en una misma cotización
- Coberturas adicionales, franquicias, asistencia mecánica, vehículo sustituto y accidentes personales
- Roles distintos de productor (supervisor, administrador)

---

## 7. Simplificaciones respecto de la realidad

Para que el ejercicio sea manejable se simplificaron algunos aspectos del negocio real:

- **Patentes:** en Argentina conviven el formato viejo (ABC123), el Mercosur (AB123CD) y formatos propios de motos. Se acepta solo ABC123.
- **Impuestos:** en la realidad hay varios (IVA, ingresos brutos, sellados) y dependen de la provincia. Se usa un único 21 %.
- **Conductor:** se asume que maneja el cliente. En la realidad puede declararse otro conductor habitual.
- **Tarifas:** las tasas y factores son inventados y no reflejan ninguna tarifa real.