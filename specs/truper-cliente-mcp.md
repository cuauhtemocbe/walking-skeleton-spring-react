---
title: Slice Cliente — MCP Server Truper (M6)
status: in-progress
created: 2026-08-18
updated: 2026-08-18
issue: "#28"
---

# Slice Cliente — MCP Server Truper (M6)

## Objective

Reemplazar la entidad desechable `Nota` por el primer slice vertical del dominio Truper: la
entidad `Cliente` expuesta como 5 herramientas MCP sobre Spring AI (Streamable HTTP). Este slice
también sienta la infraestructura MCP compartida (dependencia, config) que reutilizarán los
slices de Producto (M7) y Pedido (M8).

## Context

El walking skeleton ya cumple sus 5 criterios de graduación (M0-M5 cerrados) — `Nota` está
habilitada para retirarse. El examen técnico "Desarrollador/Líder Técnico con IA" pide construir
la gestión de pedidos de Truper como MCP Server, operado primero por Claude Code. Ver
`plan-mcp.md` (raíz del repo) para el plan completo M6-M12; este spec cubre únicamente M6, el
primer slice vertical (Cliente), elegido por avanzar en incrementos demostrables en vez de
modelar las 4 entidades antes de exponer ninguna tool MCP.

Decisiones ya tomadas y heredadas de `plan-mcp.md` (no se re-discuten acá):
- Postgres real (Flyway + Testcontainers), no H2.
- Dominio expuesto solo vía MCP, no REST.
- Patrón package-by-feature ya establecido por `nota/` (ver `NotaService.java`/`NotaIT.java`).

## Requirements

### Functional Requirements

- [x] Retirar el paquete `com.miapp.nota` (main y test) y agregar `V2__eliminar_nota.sql`
      (`DROP TABLE nota;` — nunca se edita `V1__crear_nota.sql`, ya aplicada).
- [x] `V3__crear_cliente.sql`: tabla `cliente` (`id`, `nombre`, `rfc` único, `razon_social`).
- [x] Entidad JPA `Cliente` package-private (constructor `protected` vacío para JPA + constructor
      de dominio package-private, sin setters), patrón exacto de `nota/Nota.java`.
- [x] `ClienteRepository extends JpaRepository<Cliente, Long>` vacío.
- [x] `ClienteService`: crear, listar, buscar por id, actualizar, eliminar. Valida RFC único al
      crear/actualizar.
- [x] Excepciones de dominio: `ClienteNoEncontradoException`, `RfcDuplicadoException`.
- [x] DTOs `record` en `cliente/dto/`: `CrearCliente`, `ActualizarCliente`, `ClienteResponse`, con
      Bean Validation (`@NotBlank` nombre/rfc/razón social).
- [x] Agregar `spring-ai-starter-mcp-server-webmvc` + BOM `spring-ai-bom:2.0.0` a
      `build.gradle.kts` (primera vez que se necesita esta infraestructura).
- [x] Config `spring.ai.mcp.server` en `application.yaml`: `protocol: STREAMABLE`, `type: SYNC`,
      `name`, `version`, `instructions`.
- [x] `cliente/mcp/ClienteMcpTools.java`: 5 tools (`crearCliente`, `listarClientes`,
      `buscarCliente`, `actualizarCliente`, `eliminarCliente`) vía `@McpTool`/`@McpToolParam`, cada
      una atrapando las excepciones de dominio y relanzando `McpError` con mensaje legible en
      español (evita el bug conocido de `mcp-annotations#52`).
- [x] `McpServerIT`: Testcontainers + Postgres real + `io.modelcontextprotocol.sdk:mcp` como
      cliente de test, cubre el flujo feliz completo (crear→listar→buscar→actualizar→eliminar) y
      un flujo de error (RFC duplicado o cliente no encontrado).

### Non-Functional Requirements

- Cobertura: JaCoCo ≥90% línea sobre el código nuevo (umbral no negociable del repo, ver
  `CLAUDE.md`).
- Errores: todo error de dominio expuesto vía MCP debe traer un mensaje legible para el agente, no
  la envoltura genérica de Spring AI.

## Architecture

### Components

```
api/src/main/java/com/miapp/cliente/
  Cliente.java
  ClienteRepository.java
  ClienteService.java
  dto/{CrearCliente,ActualizarCliente,ClienteResponse}.java
  excepciones/{ClienteNoEncontradoException,RfcDuplicadoException}.java
  mcp/ClienteMcpTools.java
```

Infra MCP compartida (se agrega en este slice, la reutilizan M7/M8 sin volver a tocarla):
`build.gradle.kts` (BOM + starter), `application.yaml` (bloque `spring.ai.mcp.server`).

### Data Model

Tabla `cliente`: `id` (PK, identity), `nombre` (not null), `rfc` (not null, unique), `razon_social`
(not null).

### External Dependencies

- `org.springframework.ai:spring-ai-starter-mcp-server-webmvc` (BOM `spring-ai-bom:2.0.0`):
  expone las tools MCP sobre Streamable HTTP.
- `io.modelcontextprotocol.sdk:mcp` (test only): cliente MCP real para `McpServerIT`.

## User Stories

- Como agente MCP, quiero crear un Cliente con RFC único para poder asociarlo luego a un Pedido.
- Como agente MCP, quiero listar y buscar Clientes para verificar si uno ya existe antes de darlo
  de alta (flujo de entrevista de M9).
- Como agente MCP, quiero recibir un mensaje de error legible si intento crear un Cliente con RFC
  duplicado o buscar uno que no existe, en vez de una excepción genérica.

## Testing Strategy

### Unit Tests
`ClienteService`: casos happy de cada operación + RFC duplicado + cliente no encontrado + caso
"Many" (listar varios clientes preserva orden, patrón ya establecido en `NotaService`).

### Integration Tests
`McpServerIT`: Testcontainers con Postgres real, cliente MCP (Java SDK) contra el servidor
levantado, flujo feliz completo de las 5 tools + un flujo de error con `McpError` legible.

### E2E Tests
Ninguno automatizado — verificación manual con `claude mcp add` queda para M9, fuera de este spec.

## Boundaries & Constraints

### In Scope
Entidad Cliente completa (capas + MCP tools), retiro de `Nota`, infraestructura MCP compartida
(dependencia + config).

### Out of Scope
Producto (M7), Pedido/PedidoDetalle (M8), verificación con Claude Code (M9), cualquier endpoint
REST para Cliente (el dominio se expone solo vía MCP, decisión ya tomada en `plan-mcp.md`).

### Technical Constraints
Java 25, Spring Boot 4.1.0, Spring AI 2.0.0. Postgres real vía Testcontainers, cero mocks de BD.
`ddl-auto: validate` — el esquema lo gobierna la migración Flyway, no Hibernate.

## Success Criteria

- [x] Un cliente MCP real (Java SDK) contra `http://localhost:8080/mcp` lista exactamente 5 tools.
- [x] El flujo crear→listar→buscar→actualizar→eliminar Cliente pasa contra Postgres real
      (Testcontainers).
- [x] `./gradlew check` verde, JaCoCo ≥90% sobre el código nuevo.
- [x] `grep -ril nota api/src/main/java api/src/test/java` no devuelve nada, salvo falsos positivos
      de la palabra "annotation" y el propio `V1__crear_nota.sql` (migración ya aplicada, nunca se
      edita — ver `V2__eliminar_nota.sql`, que la revierte). `web/src` y `README.md` quedan **fuera**
      de este chequeo: retirar la UI de Nota es explícitamente de M10/M11 (Out of Scope arriba), no
      de este slice.

## Implementation Plan

Ver `specs/truper-cliente-mcp-plan.md`.

## Changelog

<!-- Vacío: este spec no ha llegado a `completed` todavía. -->
