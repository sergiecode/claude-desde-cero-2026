#!/usr/bin/env node
// Hook PostToolUse (Edit|Write): compila el backend cuando Claude edita un archivo Java.
// Si no compila, devuelve los errores a Claude (exit 2) para que los corrija.
// Funciona en Windows, macOS y Linux. Solo necesita Node.js.

const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const isWindows = process.platform === "win32";
const projectDir = path.resolve(process.env.CLAUDE_PROJECT_DIR || process.cwd());
const backendDir = path.join(projectDir, "backend");

function readInput() {
  try {
    return JSON.parse(fs.readFileSync(0, "utf8") || "{}");
  } catch {
    return {};
  }
}

// ¿El archivo editado es un .java dentro de backend/?
function isBackendJava(filePath) {
  if (!filePath) return false;
  let relative = path.relative(backendDir, path.resolve(projectDir, filePath));
  if (isWindows) relative = relative.toLowerCase();
  const insideBackend = relative !== "" && !relative.startsWith("..") && !path.isAbsolute(relative);
  return insideBackend && relative.endsWith(".java");
}

const input = readInput();
const filePath = input.tool_input && input.tool_input.file_path;
if (!isBackendJava(filePath)) process.exit(0);

// Si el proyecto Maven todavía no existe (etapa 1), no hace nada.
const mvnwPath = path.join(backendDir, isWindows ? "mvnw.cmd" : "mvnw");
if (!fs.existsSync(mvnwPath)) process.exit(0);

// En Windows, con shell:true el .cmd no se resuelve por nombre relativo: se pasa la ruta
// absoluta, entre comillas por si la carpeta tiene espacios.
const result = spawnSync(isWindows ? `"${mvnwPath}"` : "./mvnw", ["-q", "-DskipTests", "compile"], {
  cwd: backendDir,
  encoding: "utf8",
  shell: isWindows, // los .cmd de Windows solo se pueden ejecutar a través de una shell
});

if (result.status !== 0) {
  const output = `${result.stdout || ""}\n${result.stderr || ""}`;
  const errors = output.split(/\r?\n/).filter((line) => line.includes("ERROR")).slice(0, 30);
  console.error(`El backend no compila después de editar ${filePath}. Errores:`);
  console.error(errors.length ? errors.join("\n") : output.trim().split(/\r?\n/).slice(-30).join("\n"));
  process.exit(2);
}

process.exit(0);