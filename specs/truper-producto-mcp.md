---
title: Slice Producto — MCP Server Truper (M7)
status: approved
created: 2026-08-18
updated: 2026-08-18
issue: "#29"
---

# Slice Producto — MCP Server Truper (M7)

## Objective

Segundo slice vertical del dominio Truper: la entidad `Producto` expuesta como 5 herramientas MCP,
repitiendo el corte completo (migración→entidad→repo→service→excepciones→MCP tools→`McpServerIT`)
ya establecido en M6, reutilizando la infraestructura MCP compartida sin tocarla.

## Context

M6 (Cliente) cerró con la infraestructura MCP compartida (`spring-ai-starter-mcp-server-webmvc`,
config `spring.ai.mcp.server`) ya en pie y el patrón de slice vertical demostrado. M7 aplica el
mismo patrón a `Producto`, la segunda de las tres entidades del dominio (Cliente → Producto →
Pedido), sin infraestructura nueva que agregar — solo repite el corte por capa sobre una entidad
distinta. Ver `plan-mcp.md` (raíz del repo) para el plan completo M6-M12; este spec cubre
únicamente M7.

Decisiones ya tomadas y heredadas de `plan-mcp.md` (no se re-discuten acá):
- Postgres real (Flyway + Testcontainers), no H2.
- Dominio expuesto solo vía MCP, no REST.
- Patrón package-by-feature ya establecido por `nota/` y confirmado por `cliente/` en M6.
- `Producto.precioUnitario` es el campo que M8 (Pedido) necesitará leer para calcular el total al
  vuelo (`Σ cantidad × precioUnitario`) — se modela en este slice aunque su consumidor llegue
  después.

## Requirements

### Functional Requirements

- [ ] `V4__crear_producto.sql`: tabla `producto` (`id`, `codigo` único, `nombre`, `precio_unitario`).
- [ ] Entidad JPA `Producto` package-private (constructor `protected` vacío para JPA + constructor
      de dominio package-private, sin setters), mismo patrón que `cliente/Cliente.java`.
- [ ] `ProductoRepository extends JpaRepository<Producto, Long>` vacío.
- [ ] `ProductoService`: crear, listar, buscar por id, actualizar, eliminar. Valida código único al
      crear/actualizar y `precioUnitario > 0`.
- [ ] Excepciones de dominio: `ProductoNoEncontradoException`, `CodigoProductoDuplicadoException`.
- [ ] DTOs `record` en `producto/dto/`: `CrearProducto`, `ActualizarProducto`, `ProductoResponse`,
      con Bean Validation (`@NotBlank` código/nombre, `@Positive` precioUnitario).
- [ ] `producto/mcp/ProductoMcpTools.java`: 5 tools (`crearProducto`, `listarProductos`,
      `buscarProducto`, `actualizarProducto`, `eliminarProducto`) vía `@McpTool`/`@McpToolParam`,
      cada una atrapando las excepciones de dominio y relanzando `McpError` con mensaje legible en
      español (mismo patrón anti-`mcp-annotations#52` que `ClienteMcpTools`).
- [ ] Extender `McpServerIT` existente (no crear uno nuevo) con el flujo feliz completo de Producto
      (crear→listar→buscar→actualizar→eliminar) y un flujo de error (código duplicado o producto no
      encontrado).

### Non-Functional Requirements

- Cobertura: JaCoCo ≥90% línea sobre el código nuevo (umbral no negociable del repo).
- Errores: todo error de dominio expuesto vía MCP debe traer un mensaje legible para el agente, no
  la envoltura genérica de Spring AI.

## Architecture

### Components

```
api/src/main/java/com/miapp/producto/
  Producto.java
  ProductoRepository.java
  ProductoService.java
  dto/{CrearProducto,ActualizarProducto,ProductoResponse}.java
  excepciones/{ProductoNoEncontradoException,CodigoProductoDuplicadoException}.java
  mcp/ProductoMcpTools.java
```

Sin infraestructura nueva: `build.gradle.kts` y `application.yaml` ya quedaron listos en M6.

### Data Model

Tabla `producto`: `id` (PK, identity), `codigo` (not null, unique), `nombre` (not null),
`precio_unitario` (not null, `numeric`).

### External Dependencies

Ninguna nueva — reutiliza `spring-ai-starter-mcp-server-webmvc` e
`io.modelcontextprotocol.sdk:mcp` (test) ya agregados en M6.

## User Stories

- Como agente MCP, quiero crear un Producto con código único y precio para poder referenciarlo
  luego en las líneas de un Pedido (M8).
- Como agente MCP, quiero listar y buscar Productos para verificar catálogo antes de armar un
  pedido (flujo de entrevista de M9).
- Como agente MCP, quiero recibir un mensaje de error legible si intento crear un Producto con
  código duplicado o buscar uno que no existe, en vez de una excepción genérica.

## Testing Strategy

### Unit Tests
`ProductoService`: casos happy de cada operación + código duplicado + producto no encontrado +
precio inválido (≤0) + caso "Many" (listar varios productos preserva orden, patrón ya establecido
en `NotaService`/`ClienteService`).

### Integration Tests
Extensión de `McpServerIT`: mismo cliente MCP (Java SDK) contra el servidor ya levantado, flujo
feliz completo de las 5 tools de Producto + un flujo de error con `McpError` legible. Al cerrar
este slice el test lista 10 tools totales (5 Cliente + 5 Producto).

### E2E Tests
Ninguno automatizado — verificación manual con `claude mcp add` queda para M9, fuera de este spec.

## Boundaries & Constraints

### In Scope
Entidad Producto completa (capas + MCP tools), extensión de `McpServerIT`.

### Out of Scope
Cliente (ya cerrado en M6), Pedido/PedidoDetalle (M8), verificación con Claude Code (M9), cualquier
endpoint REST para Producto (el dominio se expone solo vía MCP), infraestructura MCP nueva (ya
existe desde M6).

### Technical Constraints
Java 25, Spring Boot 4.1.0, Spring AI 2.0.0. Postgres real vía Testcontainers, cero mocks de BD.
`ddl-auto: validate` — el esquema lo gobierna la migración Flyway, no Hibernate.

## Success Criteria

- [ ] Un cliente MCP real (Java SDK) contra `http://localhost:8080/mcp` lista exactamente 10 tools
      (5 Cliente + 5 Producto).
- [ ] El flujo crear→listar→buscar→actualizar→eliminar Producto pasa contra Postgres real
      (Testcontainers), igual que el de Cliente.
- [ ] `./gradlew check` verde, JaCoCo ≥90% sobre el código nuevo.

## Implementation Plan

Ver `specs/truper-producto-mcp-plan.md`.

## Changelog

<!-- Vacío: este spec no ha llegado a `completed` todavía. -->
