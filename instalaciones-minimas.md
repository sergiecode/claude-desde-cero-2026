# Instalaciones mínimas

Lo que cada participante tiene que tener instalado antes de empezar a implementar el cotizador. Sirve para **Windows, macOS y Linux**.

| # | Herramienta | Versión | Para qué se usa |
| --- | --- | --- | --- |
| 1 | Java (JDK) | 21 | Compilar y correr el backend |
| 2 | Node.js | 22.22.3 o superior (o 24.15.0 o superior) | Angular, el frontend y los hooks de Claude Code |
| 3 | Docker | Reciente | PostgreSQL y los tests de integración |
| 4 | Git | Reciente | Control de versiones (en Windows también aporta Git Bash) |
| 5 | Claude Code | Reciente | El agente |

No hace falta instalar Maven ni Angular CLI: el backend trae Maven Wrapper (`mvnw`) y el CLI de Angular se usa con `npx`.

---

## 1. Java 21 (JDK)

Recomendado: **Eclipse Temurin 21**, libre y gratuito.

- Descarga (todos los sistemas): https://adoptium.net/temurin/releases/?version=21

| Sistema | Instalación alternativa por consola |
| --- | --- |
| Windows | `winget install EclipseAdoptium.Temurin.21.JDK` |
| macOS (Homebrew) | `brew install --cask temurin@21` |
| Linux | Seguir https://adoptium.net/installation/linux/ |

En el instalador de Windows, activá la opción **Set JAVA_HOME variable**.

**Si ya tenés otra versión de Java instalada** (por ejemplo, Java 25): `JAVA_HOME` tiene que apuntar al JDK 21, porque es el que usa Maven.

- **Windows:** en *Variables de entorno*, que `JAVA_HOME` apunte a la carpeta del JDK 21 y que `%JAVA_HOME%\bin` esté primero en el `Path`.
- **macOS:** agregá a `~/.zshrc` la línea `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`.
- **Linux:** `sudo update-alternatives --config java` y ajustá `JAVA_HOME` en tu perfil.

Verificar en una terminal nueva:

```
java -version
```

Tiene que decir `21`.

---

## 2. Node.js

- Descarga (todos los sistemas): https://nodejs.org/en/download

Elegí una versión LTS que cumpla el mínimo de la tabla.

Verificar:

```
node -v
```

---

## 3. Docker

| Sistema | Qué instalar |
| --- | --- |
| Windows y macOS | Docker Desktop: https://www.docker.com/products/docker-desktop/ |
| Linux | Docker Engine con el plugin Compose: https://docs.docker.com/engine/install/ |

Tiene que estar **corriendo** cuando trabajes.

Verificar:

```
docker --version
docker compose version
```

---

## 4. Git

- Descarga (todos los sistemas): https://git-scm.com/downloads

En **Windows**, instalá Git for Windows: incluye **Git Bash**, que Claude Code usa para ejecutar comandos.

Verificar:

```
git --version
```

---

## 5. Claude Code

- Guía oficial: https://code.claude.com/docs/en/setup

| Sistema | Comando |
| --- | --- |
| macOS / Linux / WSL | `curl -fsSL https://claude.ai/install.sh \| bash` |
| Windows (PowerShell) | `irm https://claude.ai/install.ps1 \| iex` |

La primera vez que ejecutes `claude` te pide iniciar sesión.

Verificar:

```
claude --version
```

Si ya estaba instalado, actualizalo con `claude update`.

---

## Verificación final

Abrí una terminal nueva y corré:

```
java -version
node -v
docker --version
git --version
claude --version
```

Si los cinco comandos responden con una versión (Java 21 y Node dentro del mínimo), está todo listo.