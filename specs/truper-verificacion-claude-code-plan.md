# Implementation Plan: Verificación con Claude Code (M9)

**Spec**: `specs/truper-verificacion-claude-code.md`
**Created**: 2026-08-18
**Status**: approved

## Components

### 1. Registro del cliente MCP
- **Purpose**: Conectar Claude Code al servidor Truper corriendo en local.
- **Files**: ninguno (comando de CLI, no código).
- **Effort**: XS

### 2. Carga de productos de prueba
- **Purpose**: Poblar catálogo mínimo (martillo, serrucho, con precio) necesario para que el
  prompt de la demo tenga sentido — el examen excluye precarga/seed automatizado, así que se hace
  vía las propias tools MCP (`crearProducto`), no un script.
- **Files**: ninguno.
- **Effort**: XS

### 3. Demo de la entrevista de venta
- **Purpose**: Ejecutar y verificar el prompt de aceptación del examen contra el servidor real.
- **Files**: ninguno (sesión conversacional).
- **Effort**: S

### 4. Documentación en README
- **Purpose**: Dejar la secuencia reproducible desde un clon limpio: registro del cliente MCP,
  carga de productos, prompt de la demo, y el mecanismo de verificación alternativo a
  `/h2-console`.
- **Files**: `README.md`.
- **Effort**: S

## Dependencies

### Build Order
1. Registro del cliente MCP (componente 1) — requiere `make up` + `make api-run` corriendo con
   las 14 tools de M6-M8 ya disponibles.
2. Carga de productos de prueba (componente 2) — depende de 1.
3. Demo de la entrevista (componente 3) — depende de 2.
4. Documentación en README (componente 4) — se escribe en paralelo mientras se corre 1-3, y se
   cierra al final con la secuencia ya verificada.

### External Dependencies
Ninguna nueva — CLI de Claude Code ya disponible en el entorno.

## Risks & Assumptions

### Risks
- **No determinismo del agente conversacional**: a diferencia de `McpServerIT` (determinista),
  Claude Code puede formular la entrevista con palabras distintas entre corridas. Mitigación: el
  criterio de éxito se centra en el resultado verificable (RFC pedido antes de crear cliente
  nuevo, total correcto), no en el texto exacto de la conversación.
- **Servidor no corriendo al momento de registrar el cliente MCP**: `claude mcp add` no valida
  conectividad al momento del registro. Mitigación: documentar explícitamente en el README el
  orden `make up` → `make api-run` → `claude mcp add`, no al revés.

### Assumptions
- El precio de "martillo" y "serrucho" para la demo es arbitrario (no viene especificado en el
  examen) — se documenta el valor usado en el README para que el total esperado sea verificable
  por quien reproduzca la demo.

## Milestones

- [ ] Cliente MCP registrado y visible con `claude mcp list`.
- [ ] Catálogo mínimo cargado (martillo + serrucho con precio).
- [ ] Demo corrida de punta a punta con total verificado.
- [ ] README actualizado con la secuencia completa y el mecanismo alternativo a `/h2-console`.

## Tasks

**Slicing strategy**: Horizontal (layered) — es una secuencia lineal de pasos manuales (registrar
→ cargar → demo → documentar), no hay escenarios independientes que cortar verticalmente.

### Foundation (Build First)

- [ ] **Registrar Claude Code como cliente MCP**
  - **Acceptance**: `claude mcp add --transport http truper-pedidos http://localhost:8080/mcp`
    corre sin error con el servidor ya arriba; `claude mcp list` muestra `truper-pedidos`.
  - **Files**: n/a.
  - **Tests**: n/a.
  - **Effort**: XS

- [ ] **Cargar catálogo mínimo de productos**
  - **Acceptance**: al menos "martillo" y "serrucho" existen vía `crearProducto`, con precio
    documentado.
  - **Files**: n/a.
  - **Tests**: n/a.
  - **Effort**: XS

- [ ] **Correr y verificar la demo de la entrevista**
  - **Acceptance**: el prompt "Crea un pedido: agrégame tres martillos y cuatro serruchos"
    dispara la entrevista (RFC/razón social si el cliente es nuevo), crea el pedido, y el total
    devuelto coincide con `3×precio_martillo + 4×precio_serrucho`.
  - **Files**: n/a.
  - **Tests**: n/a — verificación manual, es el propio criterio de éxito del milestone.
  - **Effort**: S

- [ ] **Documentar en README**
  - **Acceptance**: sección nueva con la secuencia exacta de comandos (`make up`, `make api-run`,
    `claude mcp add`, carga de productos, prompt de la demo) y el mecanismo de verificación
    alternativo a `/h2-console` (tools de listado + `psql` contra el contenedor de Postgres).
  - **Files**: `README.md`.
  - **Tests**: n/a.
  - **Effort**: S

## Effort Estimate

**Total Estimated Days**: 0.25–0.5 día (consistente con la tabla de estimados de `plan-mcp.md`).

| Phase | Effort |
|-------|--------|
| Foundation (registro + carga de productos) | ~0.05 día |
| Features (demo de la entrevista) | ~0.15 día |
| Integration | n/a — no toca código |
| Testing & Polish (documentación README) | ~0.15 día |
