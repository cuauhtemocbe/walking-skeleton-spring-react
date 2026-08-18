---
title: Slice Pedido + PedidoDetalle — MCP Server Truper (M8)
status: approved
created: 2026-08-18
updated: 2026-08-18
issue: "#30"
---

# Slice Pedido + PedidoDetalle — MCP Server Truper (M8)

## Objective

Tercer y último slice vertical de la Parte 1: las entidades `Pedido`/`PedidoDetalle` expuestas
como 4 herramientas MCP, cerrando el ciclo del dominio Truper (depende de que `Cliente` y
`Producto` ya existan). Con este slice quedan las 14 tools MCP totales requeridas por el examen.

## Context

M6 (Cliente) y M7 (Producto) ya cerraron con la infraestructura MCP compartida en pie y las 10
primeras tools verificadas. M8 aplica el mismo patrón de corte vertical a `Pedido` +
`PedidoDetalle`, la única de las tres entidades con relaciones (FK a `cliente` y a `producto`) y
con un campo derivado no persistido. Ver `plan-mcp.md` (raíz del repo) para el plan completo
M6-M12; este spec cubre únicamente M8.

Decisiones ya tomadas y heredadas de `plan-mcp.md` (no se re-discuten acá):
- Postgres real (Flyway + Testcontainers), no H2.
- Dominio expuesto solo vía MCP, no REST.
- **`Pedido.total` no es una columna persistida.** El esquema de referencia del examen para la
  tabla `PEDIDO` solo tiene `id`, `cliente_id`, `fecha` — sin `total`. El enunciado dice "calcula
  su total" (verbo) y el diagrama de secuencia muestra `Pedido { total: 700 }` como la
  **respuesta** de `crearPedido`, no como algo guardado. El total se calcula al vuelo
  (`Σ cantidad × precio_unitario` sobre `PEDIDO_DETALLE`) en el service y solo aparece en
  `PedidoResponse`, nunca en la entidad JPA ni en la migración Flyway.
- Solo 4 tools para Pedido (crear/listar/obtener/eliminar, **sin actualizar**), igual que el
  diagrama de referencia del examen — a diferencia de Cliente/Producto que sí tienen 5.

## Requirements

### Functional Requirements

- [ ] `V5__crear_pedido.sql`: tabla `pedido` (`id`, `cliente_id` FK, `fecha`) — **sin** columna
      `total`.
- [ ] `V6__crear_pedido_detalle.sql`: tabla `pedido_detalle` (`id`, `pedido_id` FK,
      `producto_id` FK, `cantidad`).
- [ ] Entidad JPA `Pedido` package-private: `id`, `cliente` (`@ManyToOne`), `fecha`, `detalles`
      (`@OneToMany(cascade=ALL, orphanRemoval=true)`) — sin campo `total`.
- [ ] Entidad JPA `PedidoDetalle` package-private: `id`, `pedido` (`@ManyToOne`), `producto`
      (`@ManyToOne`), `cantidad`.
- [ ] `PedidoRepository extends JpaRepository<Pedido, Long>` vacío.
- [ ] `PedidoService.crear`: valida que el cliente exista, que cada producto de las líneas exista,
      y que cada `cantidad > 0`; calcula el total al vuelo
      (`Σ cantidad × producto.precioUnitario`) y lo incluye solo en `PedidoResponse`.
- [ ] `PedidoService`: listar, obtener por id, eliminar (sin actualizar).
- [ ] Excepciones de dominio: `PedidoNoEncontradoException`, `CantidadInvalidaException`.
      Reutiliza `ClienteNoEncontradoException`/`ProductoNoEncontradoException` ya existentes para
      validar las referencias.
- [ ] DTOs `record` en `pedido/dto/`: `CrearPedido` (cliente + lista de líneas), `LineaPedidoInput`
      (producto + cantidad), `PedidoResponse` (incluye `total` calculado y el detalle de líneas).
- [ ] `pedido/mcp/PedidoMcpTools.java`: 4 tools (`crearPedido`, `listarPedidos`, `obtenerPedido`,
      `eliminarPedido`) vía `@McpTool`/`@McpToolParam`, cada una atrapando las excepciones de
      dominio y relanzando `McpError` con mensaje legible en español (mismo patrón anti-
      `mcp-annotations#52` que `ClienteMcpTools`/`ProductoMcpTools`).
- [ ] Completar `McpServerIT` (extendido, no nuevo) con el flujo feliz completo de Pedido
      (crear→listar→obtener→eliminar, verificando el total calculado en la respuesta de
      `crearPedido`) y sus flujos de error (cliente inexistente, producto inexistente, cantidad
      inválida).

### Non-Functional Requirements

- Cobertura: JaCoCo ≥90% línea sobre **todo lo nuevo** de M6-M8 combinado (umbral no negociable
  del repo).
- Errores: todo error de dominio expuesto vía MCP debe traer un mensaje legible para el agente, no
  la envoltura genérica de Spring AI.

## Architecture

### Components

```
api/src/main/java/com/miapp/pedido/
  Pedido.java
  PedidoDetalle.java
  PedidoRepository.java
  PedidoService.java
  dto/{CrearPedido,LineaPedidoInput,PedidoResponse}.java
  excepciones/{PedidoNoEncontradoException,CantidadInvalidaException}.java
  mcp/PedidoMcpTools.java
```

Sin infraestructura MCP nueva: ya quedó lista en M6. `PedidoService` depende de
`ClienteRepository` y `ProductoRepository` (ya existentes) para validar las referencias del
pedido.

### Data Model

Tabla `pedido`: `id` (PK, identity), `cliente_id` (FK a `cliente`, not null), `fecha` (not null) —
**sin** `total`.
Tabla `pedido_detalle`: `id` (PK, identity), `pedido_id` (FK a `pedido`, not null), `producto_id`
(FK a `producto`, not null), `cantidad` (not null, entero positivo).

### External Dependencies

Ninguna nueva — reutiliza `spring-ai-starter-mcp-server-webmvc` e
`io.modelcontextprotocol.sdk:mcp` (test) ya agregados en M6.

## User Stories

- Como agente MCP, quiero crear un Pedido con una o más líneas (producto + cantidad) para un
  Cliente existente, y recibir de vuelta el total calculado, para completar el flujo de venta.
- Como agente MCP, quiero que crear un Pedido falle con un mensaje legible si el cliente no
  existe, si algún producto no existe, o si alguna cantidad es ≤0, en vez de una excepción
  genérica o un pedido inconsistente.
- Como agente MCP, quiero listar y obtener Pedidos (incluyendo su total calculado) para poder
  responder preguntas del usuario sobre pedidos ya creados.

## Testing Strategy

### Unit Tests
`PedidoService`: caso happy de creación con una y con varias líneas (verificando el cálculo del
total), cliente inexistente, producto inexistente, cantidad inválida (0 y negativa), listar,
obtener por id, eliminar, y el caso "Many" (listar varios pedidos preserva orden).

### Integration Tests
Extensión final de `McpServerIT`: mismo cliente MCP (Java SDK) contra el servidor ya levantado,
flujo feliz completo de las 4 tools de Pedido (incluye verificar el total en la respuesta de
`crearPedido`) + los tres flujos de error. Al cerrar este slice el test lista 14 tools totales y
cubre las 3 entidades.

### E2E Tests
Ninguno automatizado — verificación manual con `claude mcp add` queda para M9, fuera de este spec.

## Boundaries & Constraints

### In Scope
Entidades Pedido y PedidoDetalle completas (capas + MCP tools), cálculo de total al vuelo,
finalización de `McpServerIT` con las 14 tools.

### Out of Scope
Cliente y Producto (ya cerrados en M6/M7), verificación con Claude Code (M9), cualquier endpoint
REST para Pedido, tool de actualización de Pedido (fuera de alcance por diseño, igual que el
diagrama de referencia del examen), infraestructura MCP nueva (ya existe desde M6).

### Technical Constraints
Java 25, Spring Boot 4.1.0, Spring AI 2.0.0. Postgres real vía Testcontainers, cero mocks de BD.
`ddl-auto: validate` — el esquema lo gobierna la migración Flyway, no Hibernate. `Pedido.total`
nunca se persiste — vive solo en `PedidoResponse`, calculado en `PedidoService`.

## Success Criteria

- [ ] Un cliente MCP real (Java SDK) contra `http://localhost:8080/mcp` lista exactamente 14 tools
      totales.
- [ ] `McpServerIT` cubre las 3 entidades con flujo feliz + error cada una, incluyendo la
      verificación del total calculado en la respuesta de `crearPedido`.
- [ ] `./gradlew check` verde, JaCoCo ≥90% sobre todo lo nuevo (M6-M8 combinado).

## Implementation Plan

Ver `specs/truper-pedido-mcp-plan.md`.

## Changelog

<!-- Vacío: este spec no ha llegado a `completed` todavía. -->
