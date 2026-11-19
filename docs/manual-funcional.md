# Manual funcional — Cotizador de seguros de vehículos

| | |
| --- | --- |
| Versión | 1.0 — 19/11/2026 |
| Fuente | `relevamiento.md` |
| Siguiente paso | `docs/spec.md` (especificación técnica) |

> Este manual describe **qué hace el sistema** en lenguaje de negocio. No describe cómo se construye: eso va en la SPEC. Todos los montos, tasas y factores son **ficticios**.

---

## 1. Objetivo

Permitir que un **productor de seguros** cotice el seguro de un vehículo para uno de sus clientes y, si el cliente acepta, **emita la póliza** a partir de esa cotización.

El sistema cotiza cuatro tipos de vehículo con un único cotizador: **auto, moto, camioneta y camión**. Lo que se puede elegir en pantalla cambia según el tipo de vehículo.

---

## 2. Alcance

**Incluye**

- Ingreso del productor al sistema
- Búsqueda del cliente por documento
- Selección del vehículo y de su tipo de uso
- Suma asegurada sugerida y ajustable
- Elección de un plan
- Cálculo del precio mensual con su desglose
- Guardado y consulta de cotizaciones
- Emisión y consulta de pólizas

**No incluye**

- Pagos (se considera que la póliza está paga al emitirse)
- PDF, envío de emails, anulación o renovación de pólizas
- Alta o modificación de productores, clientes o catálogos (vienen precargados)
- Comparar planes, sumar coberturas adicionales o aplicar franquicias

---

## 3. Actores

| Actor | Qué hace |
| --- | --- |
| **Productor** | Único usuario del sistema. Ingresa, cotiza, guarda cotizaciones y emite pólizas. Ve solo su propia información. |
| **Cliente** | No usa el sistema. Es la persona o empresa para la que se cotiza. Ya existe en el sistema. |

---

## 4. Información disponible desde el inicio

Al iniciar, el sistema ya cuenta con esta información (el detalle está en `relevamiento.md`, sección 4):

| Información | Contenido |
| --- | --- |
| Productores | 2 productores con usuario y contraseña de demo |
| Clientes | 6 clientes: 4 personas físicas (DNI) y 2 empresas (CUIT) |
| Ubicaciones | 10 localidades, cada una asignada a una zona tarifaria |
| Vehículos | 4 tipos, 3 marcas por tipo, 2 modelos por marca, 2 versiones por modelo |
| Valuaciones | Un valor 0 km de referencia por versión |
| Planes | RC, Terceros completo y Todo riesgo |

---

## 5. Recorrido del productor

```
Ingreso → Nueva cotización → [Cotizar] → [Guardar cotización] → Detalle de cotización → [Emitir póliza] → Detalle de póliza
                                                                       ↑
                                                              Mis cotizaciones
```

### 5.1. Ingreso

- El productor ingresa con su **email y contraseña**.
- Si los datos son incorrectos: *"Usuario o contraseña incorrectos"*.
- Al ingresar, ve el listado **Mis cotizaciones** y el botón **Nueva cotización**.

### 5.2. Nueva cotización

Es un único formulario con tres bloques, que se completan en orden.

#### Bloque 1 — Cliente

1. El productor elige el **tipo de documento** (DNI o CUIT) y escribe el **número**.
2. El sistema busca el cliente:
   - **Si existe:** muestra sus datos en modo **solo lectura**.
     - Persona física: nombre, apellido, fecha de nacimiento y edad.
     - Empresa: razón social.
     - En ambos casos: email, teléfono, domicilio, localidad y provincia.
   - **Si no existe:** muestra *"Cliente no encontrado"* y **no permite continuar** con los bloques siguientes.

#### Bloque 2 — Vehículo

Los campos se completan en este orden. Cada elección filtra las opciones del campo siguiente, y **si se cambia un campo, se vacían los que dependen de él**.

| Orden | Campo | Opciones |
| --- | --- | --- |
| 1 | Tipo de vehículo | Auto, Moto, Camioneta, Camión |
| 2 | Año de fabricación | Desde 2010 hasta el año actual |
| 3 | Marca | Las marcas del tipo elegido que tengan modelos para ese año |
| 4 | Modelo | Los modelos de esa marca disponibles para ese año |
| 5 | Versión | Las versiones de ese modelo |
| 6 | Tipo de uso | Solo los permitidos para el tipo de vehículo (ver tabla) |
| 7 | Patente | Texto libre con formato ABC123 |

**Tipos de uso permitidos**

| Tipo de uso | Auto | Moto | Camioneta | Camión |
| --- | --- | --- | --- | --- |
| Particular | ✓ | ✓ | ✓ | ✓ |
| Comercial | ✓ | ✓ | ✓ | ✓ |
| Taxi / remís | ✓ | | | |
| Transporte de carga | | | ✓ | ✓ |
| Transporte de pasajeros | | | | ✓ |
| Agrícola | | | ✓ | ✓ |
| Delivery | ✓ | ✓ | ✓ | |

#### Bloque 3 — Plan y suma asegurada

1. **Plan:** un desplegable con los tres planes. Cada uno muestra una breve descripción de lo que cubre.

   | Plan | Qué cubre |
   | --- | --- |
   | RC — Responsabilidad civil | Daños a terceros |
   | Terceros completo | RC + robo e incendio totales y parciales + destrucción total + granizo + cristales |
   | Todo riesgo | Terceros completo + daños parciales al propio vehículo |

2. **Suma asegurada:** al elegir la versión, el sistema muestra el **mínimo**, el **máximo** y carga automáticamente el **valor sugerido**. El productor puede cambiarlo, siempre dentro del rango.

#### Botones

- **Cotizar:** se habilita cuando todos los campos están completos y son válidos. Calcula el precio y muestra el desglose (ver sección 7). **No guarda nada.**
  - Si el productor cambia cualquier dato después de cotizar, el resultado se borra y tiene que volver a cotizar.
- **Guardar cotización:** aparece solo después de cotizar y mientras no se hayan cambiado los datos.
  - Registra la cotización con su número, en estado **Calculada** y con vencimiento a 15 días.
  - Lleva al **Detalle de cotización**.

### 5.3. Detalle de cotización

Muestra, en solo lectura:

- Número, estado, fecha de creación y fecha de vencimiento
- Datos del cliente y del vehículo
- Plan y suma asegurada
- Desglose completo del cálculo (cada factor aplicado), prima mensual, impuestos y total
- Si ya se emitió, el número de póliza con acceso a su detalle

**Botón Emitir póliza**

- Solo se habilita si la cotización está **Calculada** (vigente y sin emitir).
- Abre un diálogo donde el productor elige la **fecha de inicio** de la póliza y confirma.
- Al confirmar, se crea la póliza y se pasa al **Detalle de póliza**.

### 5.4. Detalle de póliza

Muestra, en solo lectura:

- Número de póliza y estado
- Fecha de inicio y fecha de fin
- Número de la cotización de origen
- Cliente, vehículo, plan, suma asegurada, prima mensual, impuestos y total

### 5.5. Mis cotizaciones

- Listado con: número, fecha, cliente, vehículo (tipo, marca y modelo), plan, total mensual y estado.
- Ordenado de la más reciente a la más antigua.
- Muestra **solo las cotizaciones del productor que ingresó**.
- Al hacer clic en una cotización se abre su detalle.

---

## 6. Reglas de negocio

### 6.1. Cliente

| # | Regla |
| --- | --- |
| C1 | Solo se puede cotizar para un cliente existente. |
| C2 | DNI identifica a una **persona física**; CUIT identifica a una **empresa**. |
| C3 | El DNI tiene 7 u 8 dígitos numéricos. |
| C4 | El CUIT tiene 11 dígitos y su dígito verificador debe ser válido. Se acepta con o sin guiones. |
| C5 | Una persona física debe tener **18 años o más** el día de la cotización. |
| C6 | Los datos del cliente no se pueden modificar desde el cotizador. |
| C7 | La ubicación del cliente (localidad) determina la zona tarifaria. |

### 6.2. Vehículo

| # | Regla |
| --- | --- |
| V1 | Solo se cotizan auto, moto, camioneta y camión. |
| V2 | El año es el de **fabricación**, no el de patentamiento ni el de entrega. |
| V3 | El año va desde 2010 hasta el año actual. |
| V4 | Las marcas dependen del tipo y del año; los modelos, del tipo, el año y la marca; las versiones, del modelo. |
| V5 | El tipo de uso debe estar permitido para el tipo de vehículo. |
| V6 | La patente es obligatoria, con formato de **3 letras y 3 números** (ABC123). Se guarda en mayúsculas. |
| V7 | Una cotización corresponde a **un solo vehículo**. |

### 6.3. Suma asegurada

| # | Regla |
| --- | --- |
| S1 | El sistema calcula la suma sugerida a partir del valor 0 km de la versión y la antigüedad del vehículo. |
| S2 | Sugerido = valor 0 km × (1 − 5 % × antigüedad). Nunca baja del 30 % del valor 0 km. |
| S3 | Mínimo = 80 % del sugerido. Máximo = 120 % del sugerido. |
| S4 | Sugerido, mínimo y máximo se redondean a miles de pesos. |
| S5 | El productor puede modificar la suma asegurada solo dentro del rango [mínimo, máximo]. |
| S6 | Si la versión no tiene valuación, no se puede continuar. |

### 6.4. Plan

| # | Regla |
| --- | --- |
| P1 | Se elige **un único plan** por cotización. |
| P2 | Los tres planes están disponibles para los cuatro tipos de vehículo. |
| P3 | No hay coberturas adicionales ni franquicias. |

### 6.5. Cotización

| # | Regla |
| --- | --- |
| Q1 | Solo se puede guardar una cotización que se haya calculado con los datos actuales del formulario. |
| Q2 | La cotización guarda **todos los valores usados en el cálculo** (factores, tasa, suma asegurada, precios). Si después cambian las tarifas, la cotización no cambia. |
| Q3 | Vence **15 días corridos** después de haberse guardado. |
| Q4 | Una cotización guardada **no se modifica**. Si algo cambia, se crea una nueva. |
| Q5 | Cada productor ve solo sus cotizaciones. Si intenta abrir la de otro productor, el sistema responde *"Cotización no encontrada"*. |
| Q6 | La cotización registra qué productor la creó. |
| Q7 | Número: `COT-AAAA-NNNNNN` (año de creación + correlativo de 6 dígitos que vuelve a 1 cada año). Ejemplo: COT-2026-000001. |

### 6.6. Emisión de póliza

| # | Regla |
| --- | --- |
| E1 | Solo se emite una cotización **Calculada** (vigente y no emitida). |
| E2 | Una cotización genera **como máximo una póliza**. Si se intenta emitir dos veces (por ejemplo, con doble clic), solo se crea una. |
| E3 | No se emite una cotización vencida. |
| E4 | La fecha de inicio puede ser hoy o una fecha futura de hasta 30 días. No se permiten fechas pasadas. |
| E5 | La póliza dura **1 año**: termina el mismo día y mes del año siguiente a la fecha de inicio. Ejemplo: del 20/11/2026 al 20/11/2027. |
| E6 | La póliza conserva **exactamente** las condiciones y precios de la cotización. |
| E7 | No requiere aprobación de un supervisor. |
| E8 | Al emitirse, la póliza queda **Activa** (el pago se considera realizado) y la cotización pasa a **Emitida**. |
| E9 | Número: `POL-AAAA-NNNNNN` (año de emisión + correlativo de 6 dígitos que vuelve a 1 cada año). Ejemplo: POL-2026-000001. Es único. |
| E10 | Cada productor ve solo las pólizas de sus cotizaciones. |

---

## 7. Cálculo del precio

### 7.1. Fórmula

```
Prima mensual = Suma asegurada × Tasa del plan
                × Factor geográfico
                × Factor de uso
                × Factor de antigüedad
                × Factor de edad

Impuestos     = Prima mensual × 21 %
Total mensual = Prima mensual + Impuestos
```

- La prima mensual y los impuestos se redondean a **2 decimales** (de 0,005 hacia arriba se redondea hacia arriba).
- El redondeo se hace **solo al final** de cada resultado, no en cada multiplicación.
- Todos los importes están en **pesos argentinos (ARS)**.
- El cálculo lo hace siempre el sistema. La pantalla solo muestra el resultado.

### 7.2. Tasa del plan (mensual, sobre la suma asegurada)

| Plan | Tasa |
| --- | --- |
| RC | 0,10 % |
| Terceros completo | 0,25 % |
| Todo riesgo | 0,45 % |

### 7.3. Factor geográfico (según la zona de la localidad del cliente)

| Zona | Ubicación | Factor |
| --- | --- | --- |
| Zona 1 | CABA | 1,20 |
| Zona 2 | Gran Buenos Aires | 1,15 |
| Zona 3 | La Plata | 1,10 |
| Zona 4 | Córdoba Capital | 1,05 |
| Zona 5 | Rosario | 1,10 |
| Zona 6 | Interior de Buenos Aires | 1,00 |
| Zona 7 | Interior del país | 0,95 |

*Por qué:* en las grandes ciudades hay más tránsito y más robos.

### 7.4. Factor de uso

| Tipo de uso | Factor |
| --- | --- |
| Particular | 1,00 |
| Agrícola | 1,05 |
| Comercial | 1,10 |
| Transporte de carga | 1,25 |
| Delivery | 1,30 |
| Transporte de pasajeros | 1,35 |
| Taxi / remís | 1,40 |

*Por qué:* cuanto más horas y kilómetros circula el vehículo por trabajo, mayor es el riesgo.

### 7.5. Factor de antigüedad

Antigüedad = año actual − año de fabricación.

| Antigüedad | Factor |
| --- | --- |
| 0 a 2 años | 1,00 |
| 3 a 5 años | 1,05 |
| 6 a 10 años | 1,10 |
| 11 años o más | 1,20 |

*Por qué:* los vehículos más viejos tienen menos elementos de seguridad y más fallas mecánicas.

### 7.6. Factor de edad del conductor

El conductor es el cliente. La edad se calcula al día de la cotización.

| Edad | Factor |
| --- | --- |
| 18 a 24 años | 1,30 |
| 25 a 29 años | 1,15 |
| 30 a 59 años | 1,00 |
| 60 a 69 años | 1,05 |
| 70 años o más | 1,15 |
| Empresa (CUIT) | 1,00 |

*Por qué:* los conductores más jóvenes y los mayores tienen estadísticamente más siniestros.

### 7.7. Ejemplos

**Ejemplo 1 — Auto particular en CABA, conductora joven**

| Dato | Valor |
| --- | --- |
| Cliente | Lucía Fernández, 22 años, CABA → factor edad 1,30 · factor geográfico 1,20 |
| Vehículo | Auto, Volkswagen Gol Base 2018 → antigüedad 8 años → factor 1,10 |
| Uso | Particular → factor 1,00 |
| Plan | Terceros completo → tasa 0,25 % |
| Suma asegurada | Valor 0 km 18.000.000 × (1 − 5 % × 8) = **10.800.000** (sugerido, rango 8.640.000 a 12.960.000) |

| Paso | Cálculo | Resultado |
| --- | --- | --- |
| Base del plan | 10.800.000 × 0,25 % | 27.000,00 |
| × factor geográfico | × 1,20 | 32.400,00 |
| × factor de uso | × 1,00 | 32.400,00 |
| × factor de antigüedad | × 1,10 | 35.640,00 |
| × factor de edad | × 1,30 | **46.332,00** prima mensual |
| Impuestos | 46.332,00 × 21 % | 9.729,72 |
| **Total mensual** | | **56.061,72** |

**Ejemplo 2 — Camión de carga de una empresa en Rosario**

| Dato | Valor |
| --- | --- |
| Cliente | Transportes del Litoral S.A. (CUIT), Rosario → factor edad 1,00 · factor geográfico 1,10 |
| Vehículo | Camión, Iveco Tector Base 2014 → antigüedad 12 años → factor 1,20 |
| Uso | Transporte de carga → factor 1,25 |
| Plan | RC → tasa 0,10 % |
| Suma asegurada | Sugerido: 110.000.000 × (1 − 5 % × 12) = 44.000.000 (rango 35.200.000 a 52.800.000). El productor la ajusta a **50.000.000**. |

| Paso | Cálculo | Resultado |
| --- | --- | --- |
| Base del plan | 50.000.000 × 0,10 % | 50.000,00 |
| × factor geográfico | × 1,10 | 55.000,00 |
| × factor de uso | × 1,25 | 68.750,00 |
| × factor de antigüedad | × 1,20 | 82.500,00 |
| × factor de edad | × 1,00 | **82.500,00** prima mensual |
| Impuestos | 82.500,00 × 21 % | 17.325,00 |
| **Total mensual** | | **99.825,00** |

---

## 8. Estados

### 8.1. Cotización

| Estado | Significado | Cuándo se asigna |
| --- | --- | --- |
| **Calculada** (CALCULATED) | Guardada, vigente y sin emitir | Al guardar la cotización |
| **Emitida** (EMITTED) | Ya generó su póliza | Al emitir la póliza |
| **Vencida** (EXPIRED) | Pasaron los 15 días sin emitirse | Al consultarla, o al intentar emitirla, después de su fecha de vencimiento |

```
Calculada ──(emitir)──────────────► Emitida
    │
    └──(pasan 15 días sin emitir)──► Vencida
```

Una cotización emitida nunca pasa a vencida.

### 8.2. Póliza

| Estado | Significado | Cuándo se asigna |
| --- | --- | --- |
| **Activa** (ACTIVE) | Vigente | Al emitirse |
| **Vencida** (EXPIRED) | Pasó su fecha de fin | Al consultarla después de su fecha de fin |

Una póliza con fecha de inicio futura se muestra como Activa desde su emisión.

---

## 9. Mensajes al usuario

| Situación | Mensaje |
| --- | --- |
| Email o contraseña incorrectos | Usuario o contraseña incorrectos |
| El documento no tiene el formato correcto | El número de documento no es válido |
| El documento no corresponde a ningún cliente | Cliente no encontrado |
| El cliente persona física es menor de 18 años | El cliente debe ser mayor de 18 años |
| Patente con formato incorrecto | La patente debe tener el formato ABC123 |
| La versión no tiene valuación | No hay valuación disponible para este vehículo |
| Suma asegurada fuera del rango | La suma asegurada debe estar entre $ [mínimo] y $ [máximo] |
| Falta completar un campo obligatorio | Completá este campo |
| Intento de emitir una cotización vencida | La cotización está vencida. Generá una nueva |
| Intento de emitir una cotización ya emitida | Esta cotización ya fue emitida (póliza [número]) |
| Fecha de inicio pasada o a más de 30 días | La fecha de inicio debe estar entre hoy y los próximos 30 días |
| Cotización o póliza de otro productor, o inexistente | Cotización no encontrada / Póliza no encontrada |
| Cotización guardada | Cotización [número] guardada |
| Póliza emitida | Póliza [número] emitida correctamente |

---

## 10. Glosario

| Término | Significado |
| --- | --- |
| Productor | Persona que vende seguros en nombre de la aseguradora; es el usuario del sistema |
| Cotización | Precio calculado para asegurar un vehículo, con vigencia de 15 días |
| Póliza | Contrato de seguro emitido a partir de una cotización |
| Suma asegurada | Valor del vehículo que cubre el seguro |
| Prima | Precio del seguro antes de impuestos (en este sistema, mensual) |
| Tasa | Porcentaje de la suma asegurada que define la prima base de cada plan |
| Factor | Multiplicador que sube o baja la prima según una característica del riesgo |
| Zona tarifaria | Agrupación de localidades con el mismo factor geográfico |
| Antigüedad | Años transcurridos desde la fabricación del vehículo |
| Plan | Conjunto de coberturas que se contrata (RC, Terceros completo, Todo riesgo) |