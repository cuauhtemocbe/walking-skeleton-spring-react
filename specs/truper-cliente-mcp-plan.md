# Implementation Plan: Slice Cliente — MCP Server Truper (M6)

**Spec**: `specs/truper-cliente-mcp.md`
**Created**: 2026-08-18
**Status**: in-progress

## Components

### 1. Retiro de `Nota`
- **Purpose**: Liberar el dominio y el paquete `com.miapp.nota` para que `Cliente` ocupe su lugar
  como primera entidad real del repo.
- **Files**: elimina `api/src/main/java/com/miapp/nota/**`, `api/src/test/java/com/miapp/nota/**`;
  agrega `V2__eliminar_nota.sql`.
- **Effort**: XS

### 2. Migración + entidad `Cliente`
- **Purpose**: Esquema y modelo de dominio de Cliente, siguiendo el patrón package-private de
  `nota/Nota.java`.
- **Files**: `V3__crear_cliente.sql`, `cliente/Cliente.java`.
- **Effort**: S

### 3. Repositorio, servicio, excepciones y DTOs de Cliente
- **Purpose**: CRUD completo con validación de RFC único, mapeo entidad→DTO como método estático
  privado en el servicio (sin clase Mapper aparte).
- **Files**: `cliente/ClienteRepository.java`, `cliente/ClienteService.java`,
  `cliente/dto/{CrearCliente,ActualizarCliente,ClienteResponse}.java`,
  `cliente/excepciones/{ClienteNoEncontradoException,RfcDuplicadoException}.java`.
- **Effort**: M

### 4. Infraestructura MCP compartida
- **Purpose**: Habilitar Spring AI MCP Server sobre Streamable HTTP. Se agrega una sola vez en
  este slice; M7 y M8 la reutilizan sin tocarla.
- **Files**: `build.gradle.kts` (BOM `spring-ai-bom:2.0.0` + starter
  `spring-ai-starter-mcp-server-webmvc`), `application.yaml` (bloque `spring.ai.mcp.server`).
- **Effort**: S

### 5. `ClienteMcpTools`
- **Purpose**: Exponer las 5 operaciones de Cliente como tools MCP, con manejo explícito de
  excepciones de dominio → `McpError` legible (evita el bug de `mcp-annotations#52`).
- **Files**: `cliente/mcp/ClienteMcpTools.java`.
- **Effort**: M

### 6. `McpServerIT`
- **Purpose**: Verificar el servidor MCP end-to-end contra Postgres real, con un cliente MCP Java
  SDK real — no un mock del protocolo. Sienta la base que M7/M8 extenderán con sus propias tools.
- **Files**: `api/src/test/java/com/miapp/cliente/McpServerIT.java`.
- **Effort**: M

## Dependencies

### Build Order
1. Retiro de `Nota` (componente 1) — libera el paquete, sin dependencias previas.
2. Migración + entidad `Cliente` (componente 2) — depende de que `V2` ya haya corrido.
3. Repositorio/servicio/excepciones/DTOs (componente 3) — depende de la entidad.
4. Infraestructura MCP (componente 4) — independiente de Cliente en sí, pero solo tiene sentido
   agregarla cuando ya hay algo que exponer.
5. `ClienteMcpTools` (componente 5) — depende de 3 y 4.
6. `McpServerIT` (componente 6) — depende de 5, es la verificación final.

### External Dependencies
- `org.springframework.ai:spring-ai-starter-mcp-server-webmvc` + BOM `spring-ai-bom:2.0.0`: se
  integra en el paso 4, antes de escribir la primera tool.
- `io.modelcontextprotocol.sdk:mcp` (test scope): se integra en el paso 6, para el cliente MCP de
  `McpServerIT`.

## Risks & Assumptions

### Risks
- **Primera integración real de Spring AI MCP Server con Boot 4.1.0**: la compatibilidad de
  versiones ya se verificó contra documentación oficial (`plan-mcp.md`), pero es la primera vez
  que se prueba en este repo — puede haber fricción de configuración no documentada (paths,
  serialización del `inputSchema`). Mitigación: `McpServerIT` es el gate antes de dar M6 por
  cerrado; si algo no encaja, se resuelve acá antes de que M7/M8 repitan el problema.
- **Bug conocido `mcp-annotations#52`** (excepción de dominio envuelta en mensaje genérico):
  mitigado por diseño — cada tool atrapa explícitamente sus excepciones y relanza `McpError`. El
  test de flujo de error en `McpServerIT` verifica que el mensaje llega legible, no la envoltura.

### Assumptions
- Coordenadas exactas de artefacto y versión del cliente Java `io.modelcontextprotocol.sdk:mcp`
  para test scope se confirman al implementar (la doc verificada da el nombre del paquete, no el
  artifact ID exacto de Maven Central) — validar contra Maven Central al agregar la dependencia.
- El RFC solo se valida por unicidad, no por formato (el enunciado del examen no exige formato
  específico) — si aparece un requisito de formato más adelante, se agrega como entrada nueva al
  Changelog del spec en vez de asumirlo ahora.

## Milestones

- [x] `Nota` retirada — `./gradlew check` sigue verde sin ella, `grep -ri nota` limpio.
- [x] `Cliente` con CRUD cubierto por tests unitarios (sin MCP todavía) — RFC duplicado, no
      encontrado, y el caso "Many" preservando orden.
- [x] Servidor levanta con `spring.ai.mcp.server` configurado y expone `/mcp`.
- [x] `McpServerIT` verde: 5 tools listadas, flujo feliz completo, flujo de error con mensaje
      legible.

## Tasks

**Slicing strategy**: Horizontal (layered) — M6 es en sí mismo un único slice vertical dentro del
plan más grande (`plan-mcp.md`); dentro de este spec no hay escenarios independientes que cortar
verticalmente, solo una cadena de dependencias (esquema → dominio → infraestructura MCP → tools →
verificación). Se ordenan por esa cadena, no por prioridad/riesgo.

### Foundation (Build First)

- [x] **Retirar `Nota`**
  - **Acceptance**: `com.miapp.nota` no existe (main ni test); `V2__eliminar_nota.sql` aplica
    `DROP TABLE nota;`; `./gradlew check` pasa sin el paquete.
  - **Files**: elimina `api/src/main/java/com/miapp/nota/**`,
    `api/src/test/java/com/miapp/nota/**`; agrega
    `api/src/main/resources/db/migration/V2__eliminar_nota.sql`.
  - **Tests**: ninguno nuevo — se elimina `NotaIT` junto con el paquete.
  - **Effort**: XS

- [x] **Migración y entidad `Cliente`**
  - **Acceptance**: `V3__crear_cliente.sql` aplica sobre Postgres real; `Cliente.java` compila
    con constructor `protected` vacío (JPA) + constructor de dominio package-private, sin setters.
  - **Files**: `api/src/main/resources/db/migration/V3__crear_cliente.sql`,
    `api/src/main/java/com/miapp/cliente/Cliente.java`.
  - **Tests**: cubierto indirectamente por los tests de servicio de la siguiente tarea.
  - **Effort**: S

- [x] **Repositorio, servicio, excepciones y DTOs**
  - **Acceptance**: `ClienteService` implementa crear/listar/buscar/actualizar/eliminar; RFC
    duplicado lanza `RfcDuplicadoException`; id inexistente lanza `ClienteNoEncontradoException`;
    DTOs `record` con Bean Validation.
  - **Files**: `cliente/ClienteRepository.java`, `cliente/ClienteService.java`,
    `cliente/dto/{CrearCliente,ActualizarCliente,ClienteResponse}.java`,
    `cliente/excepciones/{ClienteNoEncontradoException,RfcDuplicadoException}.java`.
  - **Tests**: `ClienteServiceTest` (unit) — casos happy por operación, RFC duplicado, no
    encontrado, y el caso "Many" (listar varios clientes preserva orden).
  - **Effort**: M

- [x] **Infraestructura MCP**
  - **Acceptance**: `./gradlew bootRun` levanta el servidor con `/mcp` respondiendo (verificable
    con `curl` o un cliente MCP mínimo); config `protocol: STREAMABLE`.
  - **Files**: `build.gradle.kts`, `application.yaml`.
  - **Tests**: smoke manual en esta tarea; cobertura real llega con `McpServerIT`.
  - **Effort**: S

- [x] **`ClienteMcpTools`**
  - **Acceptance**: 5 tools (`crearCliente`, `listarClientes`, `buscarCliente`,
    `actualizarCliente`, `eliminarCliente`) registradas; cada una atrapa las excepciones de
    dominio y relanza `McpError` con mensaje legible en español.
  - **Files**: `cliente/mcp/ClienteMcpTools.java`.
  - **Tests**: cubierto por `McpServerIT`.
  - **Effort**: M

- [x] **`McpServerIT`**
  - **Acceptance**: Testcontainers + Postgres real; cliente MCP (Java SDK) lista exactamente 5
    tools; flujo feliz crear→listar→buscar→actualizar→eliminar pasa; un flujo de error (RFC
    duplicado o no encontrado) devuelve `McpError` con mensaje legible, no la envoltura genérica.
  - **Files**: `api/src/test/java/com/miapp/cliente/McpServerIT.java`.
  - **Tests**: es el test de integración.
  - **Effort**: M

- [x] **Verificación final del slice**
  - **Acceptance**: `./gradlew check` verde con JaCoCo ≥90% sobre el código nuevo;
    `grep -ril nota api/src/main/java api/src/test/java` no devuelve nada salvo falsos positivos de
    "annotation" y `V1__crear_nota.sql` (nunca se edita). `web/src`/`README.md` quedan fuera de este
    chequeo — su limpieza es de M10/M11, no de este slice.
  - **Files**: n/a (checklist de cierre).
  - **Tests**: n/a — corre la suite completa.
  - **Effort**: XS

## Effort Estimate

**Total Estimated Days**: 0.5–0.75 día (consistente con la tabla de estimados de `plan-mcp.md`)

| Phase | Effort |
|-------|--------|
| Foundation (retiro Nota + migración/entidad Cliente) | ~0.15 día |
| Features (repo/service/excepciones/DTOs) | ~0.2 día |
| Integration (infra MCP + tools + `McpServerIT`) | ~0.3 día |
| Testing & Polish (verificación final, gates) | ~0.1 día |
