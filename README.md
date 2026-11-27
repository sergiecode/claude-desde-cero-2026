# Cotizador de seguros de vehículos

Ejercicio práctico de la clase **Claude desde cero 2026**.

Es un cotizador de seguros para autos, motos, camionetas y camiones, construido con **Angular** y **Java (Spring Boot)** usando **Claude Code**. El proyecto recorre el proceso completo:

1. **Relevamiento**: qué necesita el negocio, en decisiones claras y sin huecos.
2. **Manual funcional**: qué hace el sistema, en lenguaje de negocio.
3. **SPEC**: cómo se construye, con criterios de aceptación verificables.
4. **Configuración de Claude Code**: `CLAUDE.md` y la carpeta `.claude/` (rules, settings, hooks, commands, skills y agents).

Todos los datos del proyecto son ficticios.

## Autor

**Sergie Code**, Software Engineer.

- YouTube: https://www.youtube.com/@SergieCode
- LinkedIn: https://www.linkedin.com/in/sergiecode/
- GitHub: https://github.com/sergiecode
- Instagram: https://www.instagram.com/sergiecode
- Facebook: https://www.facebook.com/sergiecodeok/

---

## Documentación

| Archivo | Contenido |
| --- | --- |
| `relevamiento.md` | Decisiones del negocio, tarifas y datos precargados |
| `docs/manual-funcional.md` | Qué hace el sistema: pantallas, reglas, cálculo y estados |
| `docs/spec/README.md` | Índice de la SPEC: stack, estructura y cómo se levanta |
| `docs/spec/acceptance.md` | Criterios de aceptación y etapas de desarrollo |
| `CLAUDE.md` | Instrucciones del proyecto para Claude Code |
| `.claude/` | Configuración de Claude Code |

---

## Instalaciones mínimas

| Herramienta | Versión | Descarga |
| --- | --- | --- |
| Java (JDK) | 21 a 25 | https://adoptium.net/temurin/releases/?version=21 |
| Node.js | 22.22.3 o superior (o 24.15.0 o superior) | https://nodejs.org/en/download |
| Docker | Docker Desktop (Windows y macOS) o Docker Engine con Compose (Linux) | https://www.docker.com/products/docker-desktop/ |
| Git | Reciente | https://git-scm.com/downloads |

Maven y Angular CLI no hacen falta: el backend trae Maven Wrapper (`mvnw`) y el CLI de Angular se usa con `npx`.

Claude Code solo hace falta para seguir construyendo o modificando el proyecto, no para levantarlo: https://code.claude.com/docs/en/setup

Verificación:

```
java -version
node -v
docker --version
docker compose version
git --version
```

---

## Cómo levantar el proyecto

Los comandos se ejecutan desde la raíz del repositorio. Donde el comando cambia según el sistema operativo, se indican las dos variantes.

### 1. Clonar

```
git clone <url-del-repositorio>
cd <carpeta-del-repositorio>
```

### 2. Crear el archivo `.env`

El `.env` tiene las credenciales locales. No se sube al repositorio.

macOS / Linux:

```
cp .env.example .env
```

Windows (PowerShell):

```
Copy-Item .env.example .env
```

Completar los valores:

| Variable | Valor |
| --- | --- |
| `DB_NAME` | `cotizador` |
| `DB_USER` | `cotizador` |
| `DB_PASSWORD` | Cualquier contraseña |
| `JWT_SECRET` | Una cadena aleatoria de al menos 32 caracteres |

Para generar `JWT_SECRET`:

```
node -e "console.log(require('crypto').randomBytes(48).toString('base64'))"
```

### 3. Levantar la base de datos

Con Docker abierto:

```
docker compose up -d
```

PostgreSQL queda escuchando en el puerto `5432`.

### 4. Levantar el backend

En una terminal:

macOS / Linux:

```
cd backend
./mvnw spring-boot:run
```

Windows (PowerShell):

```
cd backend
.\mvnw.cmd spring-boot:run
```

Al arrancar, **Flyway aplica las migraciones automáticamente**: crea las tablas y carga los datos básicos (productores, clientes, ubicaciones, catálogo de vehículos, planes y tarifas). No hay que ejecutar nada más.

El backend queda en http://localhost:8080. La documentación de la API está en http://localhost:8080/swagger-ui.html

### 5. Levantar el frontend

En otra terminal:

```
cd frontend
npm install
npm start
```

La aplicación queda en http://localhost:4200

---

## Ingresar a la aplicación

| Usuario | Contraseña |
| --- | --- |
| `ana.torres@aseguradora.test` | `Demo2026!` |
| `diego.sosa@aseguradora.test` | `Demo2026!` |

Cada productor ve solo sus propias cotizaciones y pólizas.

### Clientes para cotizar

En "Nueva cotización", buscar el cliente por tipo y número de documento:

| Documento | Cliente | Localidad |
| --- | --- | --- |
| DNI `30123456` | Lucía Fernández | CABA |
| DNI `35987654` | Martín Gómez | Quilmes |
| DNI `25456789` | Carolina Ruiz | Córdoba |
| DNI `8765432` | Jorge Pereyra | Mendoza |
| CUIT `30-71234567-1` | Transportes del Litoral S.A. | Rosario |
| CUIT `30-70987654-2` | Agro Pampa S.R.L. | La Plata |

Un documento que no está en la lista devuelve "Cliente no encontrado".

---

## Datos y reinicio de la base

### ¿Los datos se pisan cada vez que se levanta el proyecto?

No. Flyway registra en la base qué migraciones ya aplicó y **cada migración se ejecuta una sola vez**. Los datos se guardan en un volumen de Docker:

- Si se detiene y se vuelve a levantar (`docker compose stop` / `docker compose up -d`), los datos siguen ahí, incluidas las cotizaciones y pólizas creadas.
- Al volver a arrancar el backend, Flyway detecta que las migraciones ya están aplicadas y no vuelve a cargar los datos básicos.

### Empezar con la base limpia

Para borrar todo y volver a los datos básicos:

```
docker compose down -v
docker compose up -d
```

`-v` borra el volumen con los datos. Después hay que volver a levantar el backend para que Flyway cree las tablas y cargue los datos otra vez.

### Cuándo hace falta reiniciar la base

| Situación | Qué pasa | Solución |
| --- | --- | --- |
| Se cambió `DB_PASSWORD` en `.env` después de crear la base | PostgreSQL sigue usando la contraseña anterior y el backend no puede conectarse | `docker compose down -v` y `docker compose up -d` |
| Quedó un volumen de una versión anterior del proyecto | Flyway falla al validar las migraciones | `docker compose down -v` y `docker compose up -d` |
| El puerto `5432` está ocupado por otra base | `docker compose up -d` falla | Detener la otra base o liberar el puerto |

En todos los casos, `docker compose down -v` borra las cotizaciones y pólizas creadas.

---

## Tests

| Qué | Comando |
| --- | --- |
| Backend, unitarios | `cd backend` y `./mvnw test` (Windows: `.\mvnw.cmd test`) |
| Backend, unitarios e integración | `cd backend` y `./mvnw verify` (Windows: `.\mvnw.cmd verify`). Necesita Docker abierto |
| Frontend | `cd frontend` y `npx ng test --watch=false` |

---

## Estructura del repositorio

```
├── CLAUDE.md               Instrucciones para Claude Code
├── .claude/                Configuración de Claude Code
├── relevamiento.md         Relevamiento del negocio
├── docs/
│   ├── manual-funcional.md Manual funcional
│   └── spec/               SPEC del sistema
├── docker-compose.yml      PostgreSQL
├── .env.example            Variables de entorno (plantilla)
├── backend/                Java 21 · Spring Boot · Maven · Flyway
└── frontend/               Angular 22 · Angular Material
```