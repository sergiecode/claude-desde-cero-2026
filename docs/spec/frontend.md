# Frontend

Angular 22 · Angular Material 22 · TypeScript 6.0 · componentes standalone · Signal Forms · OnPush.

---

## 1. Proyecto

| Dato | Valor |
| --- | --- |
| Carpeta | `frontend/` |
| Creación | `ng new frontend` con routing y SCSS |
| Puerto | 4200 |
| Proxy | `proxy.conf.json`: `/api` → `http://localhost:8080` (configurado en `angular.json` para `ng serve`) |
| Locale | `es-AR` (registrar `localeEsAr` y proveer `LOCALE_ID` y `DEFAULT_CURRENCY_CODE = 'ARS'`) |
| Tema | Un tema prediseñado de Angular Material |
| Detección de cambios | OnPush (por defecto en Angular 22) |
| HTTP | `HttpClient` con Fetch (por defecto en Angular 22) |

---

## 2. Estructura

```
src/app/
├── app.config.ts            ← providers: router, HttpClient con interceptores, locale, Material
├── app.routes.ts
├── core/
│   ├── auth/                ← AuthService, authInterceptor, authGuard
│   ├── api/                 ← un servicio HTTP por recurso (ClientApi, CatalogApi, PlanApi,
│   │                          InsuredValueApi, QuoteApi, PolicyApi)
│   ├── models/              ← interfaces TypeScript idénticas a los DTOs de api.md
│   └── errors/              ← errorInterceptor y ErrorMessageService
├── shared/
│   ├── labels.ts            ← traducción de enums a español (§6)
│   └── components/          ← StatusChipComponent, CalculationBreakdownComponent
├── layout/                  ← ShellComponent (toolbar con nombre del productor y "Salir")
└── features/
    ├── login/               ← LoginPageComponent
    ├── quotes/              ← QuoteListPageComponent, QuoteNewPageComponent,
    │                          QuoteDetailPageComponent, IssuePolicyDialogComponent
    └── policies/            ← PolicyDetailPageComponent
```

Reglas:

- Componentes **standalone**. Sin `NgModule`.
- Estado local con **signals** (`signal`, `computed`, `linkedSignal`, `resource` cuando corresponda).
- **Formularios con Signal Forms** (`@angular/forms/signals`): el modelo se crea con `form()`, las reglas con los validadores de esquema (`required`, `email`, `pattern`, etc.) y los campos se conectan con la directiva `FormField`. No se usan formularios reactivos ni template-driven.
- **Signal Forms + Angular Material:** se integran directamente; cómo usarlos está en §2.1.
- Los componentes no usan `HttpClient` directamente: llaman a los servicios de `core/api`.
- El frontend **no calcula** precios, sumas aseguradas, edades ni vencimientos: los muestra tal como llegan del backend.
- Los textos visibles están en español. Nombres de archivos, clases, variables y propiedades en inglés.

---

## 2.1. Signal Forms con Angular Material

En Angular 22, Signal Forms es estable y Angular Material lo soporta directamente: la directiva `FormField` se aplica sobre `matInput` y `mat-select` **sin adaptadores ni librerías extra**. No hace falta preguntar ni verificar si la integración existe: se usa.

**Reglas:**

- Enlazar cada control con **`[formField]`** (no `[field]`, que es la forma vieja).
- El modelo es un `signal` y el formulario se arma con `form(modelo, schema)`.
- **Validaciones y `disabled` van en el schema** (`required()`, `email()`, `pattern()`, `min()`, `max()`, `disabled()`…), nunca como atributos del template.
- **Sin `null`** en el modelo: inicializar con `''` u otro valor vacío.
- `mat-error`: se muestra cuando el control es inválido y fue tocado, o cuando se envía el formulario. Si un campo puede tener varios errores, el template decide cuál mostrar (`@if`, `@for` sobre `errors()`).
- El botón de enviar se deshabilita con `form().invalid()`.
- **Fecha de inicio (`mat-datepicker`):** `[formField]` sobre `<input matInput [matDatepicker]>`; en Angular 22, los controles basados en `ControlValueAccessor` funcionan con Signal Forms. Si en los tests no funciona, usar `<input matInput type="date" [formField]>` y seguir, sin preguntar.

Ejemplo de referencia (input, select y errores):

```ts
import { Component, signal } from '@angular/core';
import { form, FormField, required, email } from '@angular/forms/signals';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

@Component({
  imports: [FormField, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <mat-form-field>
      <mat-label>Email</mat-label>
      <input matInput type="email" [formField]="loginForm.email" />
      @if (loginForm.email().errors().length) {
        <mat-error>{{ loginForm.email().errors()[0].message }}</mat-error>
      }
    </mat-form-field>

    <mat-form-field>
      <mat-label>Tipo de documento</mat-label>
      <mat-select [formField]="loginForm.documentType">
        <mat-option value="DNI">DNI</mat-option>
        <mat-option value="CUIT">CUIT</mat-option>
      </mat-select>
    </mat-form-field>
  `,
})
export class Example {
  readonly model = signal({ email: '', documentType: '' });
  readonly loginForm = form(this.model, (f) => {
    required(f.email, { message: 'Completá este campo' });
    email(f.email, { message: 'Email inválido' });
    required(f.documentType, { message: 'Completá este campo' });
  });
}
```

Referencias: [Angular Material – Input](https://material.angular.dev/components/input), [Angular Material – Select](https://material.angular.dev/components/select), [Angular Material – Form field](https://material.angular.dev/components/form-field), [Signal Forms](https://angular.dev/guide/forms/signals/overview).

---

## 3. Rutas

| Ruta | Componente | Protegida |
| --- | --- | --- |
| `/login` | LoginPageComponent | No |
| `/quotes` | QuoteListPageComponent | Sí |
| `/quotes/new` | QuoteNewPageComponent | Sí |
| `/quotes/:id` | QuoteDetailPageComponent | Sí |
| `/policies/:id` | PolicyDetailPageComponent | Sí |
| `''` y cualquier otra | Redirige a `/quotes` | — |

Las rutas protegidas se muestran dentro del `ShellComponent` y usan `authGuard`.

**Carga diferida:** cada pantalla se carga con `loadComponent`, para que el bundle inicial no supere el límite de 500 kB que trae el proyecto. No se sube el límite.

---

## 4. Autenticación y errores

- **AuthService:** hace el login, guarda token, vencimiento y datos del productor en `sessionStorage`, expone el productor actual y `logout()`.
- **authGuard:** sin token o con token vencido → redirige a `/login`.
- **authInterceptor:** agrega `Authorization: Bearer <token>` a todas las llamadas a `/api` salvo el login.
- **errorInterceptor:**
  - `401` (salvo en el login) → `logout()`, redirige a `/login` y muestra el `detail`.
  - Otros errores → el componente los recibe y decide; si no los maneja, se muestra el `detail` en un `MatSnackBar`.
  - Sin respuesta del servidor → "No se pudo conectar con el servidor".
- **Errores de campo:** si el error trae `errors`, cada mensaje se muestra en el `mat-error` del campo correspondiente.
- **Quién maneja cada error:** cada llamada declara qué `code` maneja la pantalla (por ejemplo, la búsqueda de cliente maneja `CLIENT_NOT_FOUND`, `INVALID_DOCUMENT` y `CLIENT_UNDERAGE`). El `errorInterceptor` no muestra snackbar para esos códigos; todos los demás van al snackbar. Este patrón lo arma la etapa 3 y lo siguen todas las pantallas.
- **Recarga de la página:** los datos del productor se recuperan de `sessionStorage`; no se llama a `/api/auth/me` al recargar.

---

## 5. Pantallas

### 5.1. Login (`/login`)

- Campos: **Email** (obligatorio, formato email) y **Contraseña** (obligatoria, oculta).
- Botón **Ingresar**: deshabilitado mientras el formulario sea inválido o haya una llamada en curso.
- Error `INVALID_CREDENTIALS`: mensaje debajo del formulario.
- Éxito: navega a `/quotes`.

### 5.2. Shell

- Toolbar con el título "Cotizador de seguros", el nombre del productor y el botón **Salir** (`logout()` y navega a `/login`).

### 5.3. Mis cotizaciones (`/quotes`)

- Botón **Nueva cotización** → `/quotes/new`.
- `mat-table` con columnas: N.º, Fecha, Cliente, Vehículo (tipo + marca + modelo), Plan, Total mensual, Estado.
- Fecha en `dd/MM/yyyy`, total en moneda es-AR, estado con `StatusChipComponent`.
- Clic en una fila → `/quotes/:id`.
- Sin cotizaciones: "Todavía no tenés cotizaciones".

### 5.4. Nueva cotización (`/quotes/new`)

Un formulario con Signal Forms, con tres bloques (`mat-card` cada uno). Los bloques 2 y 3 están deshabilitados hasta encontrar un cliente válido.

#### Bloque 1 — Cliente

| Campo | Control | Reglas |
| --- | --- | --- |
| Tipo de documento | `mat-select` (DNI, CUIT) | Obligatorio |
| Número | `input` | Obligatorio. DNI: 7–8 dígitos. CUIT: 11 dígitos con o sin guiones |
| Botón **Buscar** | — | Llama a `GET /api/clients/search` |

- Encontrado: muestra los datos del cliente **en solo lectura** (persona: nombre, apellido, nacimiento y edad; empresa: razón social; ambos: email, teléfono, domicilio, localidad y provincia) y habilita los bloques 2 y 3.
- `CLIENT_NOT_FOUND`, `INVALID_DOCUMENT` o `CLIENT_UNDERAGE`: muestra el `detail` debajo del campo y deja los bloques 2 y 3 deshabilitados.
- Si se cambia el tipo o el número de documento, se borra el cliente encontrado y se vuelven a deshabilitar los bloques 2 y 3.

#### Bloque 2 — Vehículo (cascada)

| Orden | Campo | Control | Carga opciones con | Al cambiar, vacía |
| --- | --- | --- | --- | --- |
| 1 | Tipo de vehículo | `mat-select` | `GET /catalogs/vehicle-types` (al iniciar) | Año, marca, modelo, versión, tipo de uso, la suma asegurada y el resultado. La patente **no** se vacía |
| 2 | Año de fabricación | `mat-select` | `GET /catalogs/years` | Marca, modelo, versión, suma asegurada |
| 3 | Marca | `mat-select` | `GET /catalogs/brands` | Modelo, versión, suma asegurada |
| 4 | Modelo | `mat-select` | `GET /catalogs/models` | Versión, suma asegurada |
| 5 | Versión | `mat-select` | `GET /catalogs/versions` | Suma asegurada (se vuelve a pedir) |
| 6 | Tipo de uso | `mat-select` | `GET /catalogs/usage-types` (cuando cambia el tipo) | — |
| 7 | Patente | `input` | — | — |

- Cada select está deshabilitado hasta que el anterior tenga valor.
- Los tipos de vehículo y de uso se muestran con las etiquetas de §6.
- **Patente:** se convierte a mayúsculas mientras se escribe; validación `^[A-Z]{3}\d{3}$` con el mensaje "La patente debe tener el formato ABC123".

#### Bloque 3 — Plan y suma asegurada

| Campo | Control | Reglas |
| --- | --- | --- |
| Plan | `mat-select` con nombre y descripción | Obligatorio. Carga con `GET /api/insurance-plans` al iniciar |
| Suma asegurada | `input` numérico con prefijo `$` | Al elegir la versión se llama a `GET /api/insured-values` y se carga `suggestedAmount`. Debajo se muestra "Mínimo $ X — Máximo $ Y". Validación de rango en el formulario con el mensaje del manual |

- `VALUATION_NOT_FOUND`: muestra el mensaje y no permite cotizar.

#### Botones y resultado

| Botón | Habilitado cuando | Acción |
| --- | --- | --- |
| **Cotizar** | Cliente encontrado y formulario válido | `POST /api/quotes/calculate` y muestra el resultado |
| **Guardar cotización** | Visible solo si hay un resultado y el formulario no cambió desde que se cotizó | `POST /api/quotes`; muestra "Cotización [número] guardada" y navega a `/quotes/:id` |

- El resultado se muestra con `CalculationBreakdownComponent` (§5.7).
- **Cualquier cambio** en el formulario después de cotizar borra el resultado y oculta "Guardar cotización" (regla Q1).
- Los botones se deshabilitan mientras hay una llamada en curso (evita dobles envíos).
- Errores del backend con `errors` por campo se muestran en los campos; el resto en el snackbar.

### 5.5. Detalle de cotización (`/quotes/:id`)

- Encabezado: número, `StatusChipComponent`, fecha de creación y "Vence el dd/MM/yyyy HH:mm".
- Secciones de solo lectura: Cliente, Vehículo, Plan y suma asegurada, y desglose (`CalculationBreakdownComponent`).
- Si `policy` no es null: "Póliza emitida: [número]" con enlace a `/policies/:id`.
- Botón **Emitir póliza**: habilitado solo si `status === 'CALCULATED'`. Abre `IssuePolicyDialogComponent`.
- `QUOTE_NOT_FOUND`: muestra "Cotización no encontrada" con un botón para volver al listado.

### 5.6. Diálogo Emitir póliza

- `mat-datepicker` **Fecha de inicio**: valor inicial hoy; mínimo hoy; máximo hoy + 30 días. Esta validación es solo una ayuda: la que vale es la del backend.
- Botones **Cancelar** y **Confirmar emisión** (deshabilitado durante la llamada).
- Éxito: cierra el diálogo, muestra "Póliza [número] emitida correctamente" y navega a `/policies/:id`.
- `QUOTE_EXPIRED` o `QUOTE_ALREADY_ISSUED`: cierra el diálogo, muestra el `detail` y recarga el detalle de la cotización.
- `INVALID_START_DATE`: muestra el `detail` dentro del diálogo.

### 5.7. Componente de desglose (`CalculationBreakdownComponent`)

Recibe un `QuoteCalculation` y muestra, sin calcular nada:

| Concepto | Valor |
| --- | --- |
| Suma asegurada | `insuredAmount` |
| Tasa del plan | `planRate` como porcentaje (0,25 %) |
| Prima base | `basePremium` |
| Factor geográfico ([zona]) | `zone.factor` |
| Factor de uso | `usageFactor` |
| Factor de antigüedad ([n] años) | `vehicleAgeFactor` |
| Factor de edad ([n] años / Empresa) | `driverAgeFactor` |
| **Prima mensual** | `premium` |
| Impuestos ([21 %]) | `taxes` |
| **Total mensual** | `totalPrice` |

Los factores se muestran con 2 decimales y coma (`1,20`).

### 5.8. Detalle de póliza (`/policies/:id`)

- Encabezado: número, `StatusChipComponent`, "Vigencia: dd/MM/yyyy al dd/MM/yyyy".
- Secciones: Cliente, Vehículo, Plan, Suma asegurada, Prima mensual, Impuestos y Total mensual.
- Enlace a la cotización de origen.
- Todo en solo lectura. `POLICY_NOT_FOUND`: "Póliza no encontrada".

---

## 6. Etiquetas en español (`shared/labels.ts`)

| Enum | Código → Etiqueta |
| --- | --- |
| `VehicleType` | AUTO → Auto · MOTO → Moto · CAMIONETA → Camioneta · CAMION → Camión |
| `VehicleUsageType` | PARTICULAR → Particular · COMERCIAL → Comercial · TAXI_REMIS → Taxi / remís · TRANSPORTE_CARGA → Transporte de carga · TRANSPORTE_PASAJEROS → Transporte de pasajeros · AGRICOLA → Agrícola · DELIVERY → Delivery |
| `DocumentType` | DNI → DNI · CUIT → CUIT |
| `QuoteStatus` | CALCULATED → Calculada · EMITTED → Emitida · EXPIRED → Vencida |
| `PolicyStatus` | ACTIVE → Activa · EXPIRED → Vencida |

Colores del `StatusChipComponent`: Calculada y Activa en el color primario; Emitida en verde; Vencida en gris.

---

## 7. Tests unitarios

Con el runner de tests que trae por defecto el CLI de Angular 22. Se ejecutan con `npx ng test --watch=false`. Cada test que cubre un criterio de `acceptance.md` incluye su código al inicio de la descripción, por ejemplo `it('AC-FE-02 vacía modelo y versión al cambiar la marca', ...)`.

| Qué | Casos mínimos |
| --- | --- |
| `labels.ts` | Todos los códigos de cada enum tienen etiqueta |
| `authInterceptor` | Agrega el header con token; no lo agrega en el login |
| `authGuard` | Sin token redirige a `/login` |
| `QuoteNewPageComponent` | Cambiar la marca vacía modelo y versión · "Guardar cotización" no aparece antes de cotizar · cambiar un campo después de cotizar oculta "Guardar cotización" · bloques 2 y 3 deshabilitados sin cliente |
| `QuoteDetailPageComponent` | "Emitir póliza" deshabilitado si el estado no es `CALCULATED` |
| `CalculationBreakdownComponent` | Muestra los valores recibidos sin modificarlos |

Los servicios HTTP se simulan; estos tests no llaman al backend.

## 8. Prueba exploratoria

Además de los tests unitarios, un agente (`qa-e2e`) usa la aplicación en un navegador real, como lo haría un productor, y recorre los escenarios de `acceptance.md` §8.1. No escribe tests ni modifica código: devuelve un informe con el resultado de cada escenario y capturas de pantalla.