---
paths:
  - "frontend/**"
---

# Convenciones del frontend

Antes de modificar el frontend, leer `docs/spec/frontend.md` y `docs/spec/api.md`.

## Lo que no se negocia

- **Angular 22:** componentes standalone, signals para el estado y OnPush.
- **Formularios con Signal Forms** (`@angular/forms/signals`): `form()`, validadores de esquema y la directiva `FormField`. No usar formularios reactivos ni template-driven.
- **Angular Material** para todos los componentes visuales.
- **El frontend no calcula** precios, sumas aseguradas, edades ni vencimientos: muestra lo que devuelve el backend.
- **HTTP:** los componentes no usan `HttpClient`; llaman a los servicios de `core/api`. Las interfaces de `core/models` son idénticas a los DTOs de `api.md`.
- **Textos visibles en español**; nombres de archivos, clases y variables en inglés. Las etiquetas de los enums salen de `shared/labels.ts`.
- **Contrato:** si el frontend necesita algo que `api.md` no tiene, detenerse y preguntar. No inventar endpoints ni campos.

## Prioridad cuando hay conflicto

Las skills oficiales de Angular (`angular-developer`, `angular-new-app`) dan buenas prácticas generales. **Si una skill sugiere algo distinto a la SPEC, manda la SPEC.**

## Tests

- Se corren con `npx ng test --watch=false`.
- La descripción de cada test que cubre un criterio empieza con su código: `it('AC-FE-02 ...')`.
- Los servicios HTTP se simulan; los tests no llaman al backend.
