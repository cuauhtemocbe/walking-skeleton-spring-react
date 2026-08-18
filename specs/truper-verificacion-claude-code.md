---
title: Verificación con Claude Code (M9)
status: approved
created: 2026-08-18
updated: 2026-08-18
issue: "#31"
---

# Verificación con Claude Code (M9)

## Objective

Cerrar la Parte 1 del examen (MCP Server) con una demo de aceptación real: registrar Claude Code
como cliente MCP contra el servidor Truper y correr el prompt de la entrevista de venta que pide
el enunciado, documentando la secuencia exacta en el README para que sea reproducible desde un
clon limpio.

## Context

M6-M8 dejaron las 14 tools MCP verificadas por `McpServerIT` (cliente MCP de test, Java SDK), pero
eso no es lo mismo que la demo de aceptación que pide el examen: un agente conversacional real
(Claude Code) actuando de "entrevistador" contra el servidor corriendo. Ver `plan-mcp.md` (raíz
del repo) para el plan completo M6-M12; este spec cubre únicamente M9.

Decisión ya tomada y heredada de `plan-mcp.md` (no se re-discute acá): como el repo usa Postgres
real en vez de H2, no existe `/h2-console` para verificar visualmente el estado de la base — se
reemplaza por dos mecanismos: las propias tools MCP de listado (`listarClientes`,
`listarProductos`, `listarPedidos`) y `psql` directo contra el contenedor de Postgres.

## Requirements

### Functional Requirements

- [ ] Documentar en el README el comando exacto de registro:
      `claude mcp add --transport http truper-pedidos http://localhost:8080/mcp`.
- [ ] Documentar cómo registrar productos por lote antes de la demo (vía tools `crearProducto`,
      con al menos "martillo" y "serrucho" con precio, porque el prompt de la demo los referencia).
- [ ] Correr el prompt de aceptación del examen: *"Crea un pedido: agrégame tres martillos y
      cuatro serruchos"*, y verificar que la conversación con Claude Code sigue el flujo de
      entrevista esperado:
      1. Si el cliente no existe todavía, Claude Code pregunta por RFC/razón social antes de
         continuar (usa `crearCliente`).
      2. Arma el pedido con las líneas correctas (3× martillo, 4× serrucho) vía `crearPedido`.
      3. El total devuelto coincide con `3×precio_martillo + 4×precio_serrucho`.
- [ ] Documentar en el README el mecanismo de verificación alternativo a `/h2-console`: tools MCP
      de listado (`listarClientes`/`listarProductos`/`listarPedidos`) y
      `docker compose exec postgres psql -U notas -d notas -c "select * from pedido;"`.

### Non-Functional Requirements

- La demo debe ser reproducible desde un clon limpio siguiendo solo los pasos documentados en el
  README — sin conocimiento tácito de la sesión en la que se hizo por primera vez.

## Architecture

### Components

Ninguno nuevo — este milestone no toca código de `api/`, solo documentación (`README.md`) y una
sesión de verificación manual con Claude Code como cliente MCP.

### Data Model

N/A — reutiliza el esquema ya cerrado en M6-M8.

### External Dependencies

Ninguna nueva — usa el CLI de Claude Code ya disponible en el entorno del examinador.

## User Stories

- Como examinador, quiero seguir una secuencia de comandos documentada en el README para
  reproducir la demo de aceptación de la Parte 1 sin depender de contexto no escrito.
- Como examinador, quiero un mecanismo alternativo a `/h2-console` para verificar el estado real de
  la base de datos después de la demo, dado que este repo usa Postgres real en vez de H2.

## Testing Strategy

### Unit Tests
N/A — este milestone es verificación manual, no código nuevo.

### Integration Tests
N/A — `McpServerIT` (M6-M8) ya cubre el protocolo MCP con un cliente de test; M9 verifica la
experiencia con un cliente conversacional real, que no es automatizable de forma determinista.

### E2E Tests
Ninguno automatizado, a propósito (heredado de `plan-mcp.md`: "Fuera de alcance... tests E2E
automatizados (Playwright) para el dominio Truper — la verificación end-to-end es manual"). La
verificación es la sesión documentada con Claude Code.

## Boundaries & Constraints

### In Scope
Registro de Claude Code como cliente MCP, carga de productos de prueba, ejecución y verificación
del prompt de la demo, documentación en README del flujo y del mecanismo de verificación
alternativo a `/h2-console`.

### Out of Scope
Cualquier cambio de código en `api/` (M6-M8 ya cerrados), la app de chat propia (M10-M11), tests
automatizados de este flujo.

### Technical Constraints
Requiere `make up` (Postgres) + `make api-run` (o equivalente) corriendo antes de registrar el
cliente MCP. El servidor MCP debe estar accesible en `http://localhost:8080/mcp`.

## Success Criteria

- [ ] El flujo queda documentado en el README con la secuencia exacta de comandos.
- [ ] El mecanismo de verificación alternativo a `/h2-console` (tools de listado + `psql`) está
      documentado y fue probado al menos una vez.
- [ ] La demo del prompt de la entrevista corrió de punta a punta contra Postgres real, con el
      total del pedido verificado manualmente.

## Implementation Plan

Ver `specs/truper-verificacion-claude-code-plan.md`.

## Changelog

<!-- Vacío: este spec no ha llegado a `completed` todavía. -->
