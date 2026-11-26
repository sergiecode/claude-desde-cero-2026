#!/usr/bin/env node
// Hook Stop: cuando Claude termina de responder, corre los tests UNITARIOS
// de las partes con cambios sin commitear (backend y/o frontend).
// Si fallan, devuelve el resumen a Claude (exit 2) para que siga trabajando.
// Los tests de integración (mvnw verify) no se corren acá porque son lentos.
// Funciona en Windows, macOS y Linux. Necesita Node.js y Git.

const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const isWindows = process.platform === "win32";
const projectDir = path.resolve(process.env.CLAUDE_PROJECT_DIR || process.cwd());

function readInput() {
  try {
    return JSON.parse(fs.readFileSync(0, "utf8") || "{}");
  } catch {
    return {};
  }
}

function run(command, args, cwd, useShell) {
  return spawnSync(command, args, { cwd, encoding: "utf8", shell: useShell });
}

function hasChanges(folder) {
  const result = run("git", ["status", "--porcelain", "--", folder], projectDir, false);
  return result.status === 0 && result.stdout.trim() !== "";
}

function lastLines(result, filter, max) {
  const lines = `${result.stdout || ""}\n${result.stderr || ""}`.split(/\r?\n/);
  const selected = filter ? lines.filter(filter) : lines.filter((line) => line.trim() !== "");
  return selected.slice(-max).join("\n");
}

const input = readInput();

// Evita el bucle infinito: si este hook ya hizo que Claude siga trabajando, no vuelve a bloquear.
if (input.stop_hook_active === true) process.exit(0);

// Sin repositorio git no se puede saber qué cambió.
if (run("git", ["rev-parse", "--is-inside-work-tree"], projectDir, false).status !== 0) process.exit(0);

const report = [];

// Backend: solo si existe el proyecto Maven y hay cambios sin commitear en backend/.
const backendDir = path.join(projectDir, "backend");
const hasWrapper = fs.existsSync(path.join(backendDir, isWindows ? "mvnw.cmd" : "mvnw"));
if (hasWrapper && hasChanges("backend")) {
  const result = run(isWindows ? "mvnw.cmd" : "./mvnw", ["-q", "test"], backendDir, isWindows);
  if (result.status !== 0) {
    report.push("--- Backend (mvnw test) ---");
    report.push(lastLines(result, (line) => /FAIL|ERROR|Tests run/.test(line), 40));
  }
}

// Frontend: solo si existe el proyecto Angular y hay cambios sin commitear en frontend/.
// Nota: si el runner por defecto de Angular 22 fuera Karma, agregar "--browsers=ChromeHeadless".
const frontendDir = path.join(projectDir, "frontend");
if (fs.existsSync(path.join(frontendDir, "package.json")) && hasChanges("frontend")) {
  const result = run("npx", ["ng", "test", "--watch=false"], frontendDir, true);
  if (result.status !== 0) {
    report.push("--- Frontend (ng test) ---");
    report.push(lastLines(result, null, 40));
  }
}

if (report.length > 0) {
  console.error("Hay tests unitarios fallando. Corregilos antes de terminar:\n" + report.join("\n"));
  process.exit(2);
}

process.exit(0);