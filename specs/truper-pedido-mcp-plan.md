# Implementation Plan: Slice Pedido + PedidoDetalle — MCP Server Truper (M8)

**Spec**: `specs/truper-pedido-mcp.md`
**Created**: 2026-08-18
**Status**: approved

## Components

### 1. Migraciones + entidades `Pedido`/`PedidoDetalle`
- **Purpose**: Esquema y modelo de dominio de Pedido con su relación a Cliente y sus líneas a
  Producto, sin columna `total` persistida.
- **Files**: `V5__crear_pedido.sql`, `V6__crear_pedido_detalle.sql`, `pedido/Pedido.java`,
  `pedido/PedidoDetalle.java`.
- **Effort**: M

### 2. Repositorio, servicio, excepciones y DTOs de Pedido
- **Purpose**: Creación validando cliente/producto/cantidad, cálculo de total al vuelo,
  listar/obtener/eliminar. Depende de `ClienteRepository` y `ProductoRepository` ya existentes.
- **Files**: `pedido/PedidoRepository.java`, `pedido/PedidoService.java`,
  `pedido/dto/{CrearPedido,LineaPedidoInput,PedidoResponse}.java`,
  `pedido/excepciones/{PedidoNoEncontradoException,CantidadInvalidaException}.java`.
- **Effort**: L

### 3. `PedidoMcpTools`
- **Purpose**: Exponer las 4 operaciones de Pedido (sin actualizar) como tools MCP, con el mismo
  manejo explícito de excepciones de dominio → `McpError` legible que los slices anteriores.
- **Files**: `pedido/mcp/PedidoMcpTools.java`.
- **Effort**: M

### 4. Cierre de `McpServerIT`
- **Purpose**: Verificar Pedido end-to-end contra Postgres real, completando el test que ya cubre
  Cliente y Producto — llegando a las 14 tools totales.
- **Files**: `McpServerIT.java` (extendido).
- **Effort**: M

## Dependencies

### Build Order
1. Migraciones + entidades (componente 1) — depende de que `cliente`/`producto` ya existan como
   tablas (M6/M7 ya cerrados).
2. Repositorio/servicio/excepciones/DTOs (componente 2) — depende de la entidad y de
   `ClienteRepository`/`ProductoRepository`.
3. `PedidoMcpTools` (componente 3) — depende de 2.
4. Cierre de `McpServerIT` (componente 4) — depende de 3, es la verificación final de toda la
   Parte 1.

### External Dependencies
Ninguna nueva — `spring-ai-starter-mcp-server-webmvc` e `io.modelcontextprotocol.sdk:mcp` ya están
en `build.gradle.kts` desde M6.

## Risks & Assumptions

### Risks
- **Cálculo de total con `BigDecimal`**: sumar `cantidad × precioUnitario` sobre varias líneas
  requiere cuidado con escala/redondeo. Mitigación: test unitario explícito con más de una línea y
  cantidades no triviales (ej. 3 y 4 unidades de productos con precios distintos) para blindar el
  cálculo, no solo el caso de una sola línea.
- **Validación transaccional de cliente+producto+cantidad**: si cualquiera de las líneas referencia
  un producto inexistente, no debe quedar un `Pedido` parcial persistido. Mitigación: validar todas
  las referencias antes de persistir nada (fail-fast dentro de la misma transacción de servicio).

### Assumptions
- `cantidad` es un entero (no fracciones) — consistente con el enunciado del examen ("agrégame
  tres martillos y cuatro serruchos"). Si apareciera un caso de cantidades fraccionarias, es un
  cambio de requisito para el Changelog del spec, no una suposición a resolver acá.
- `fecha` del Pedido se genera en el servidor al crear (no la envía el agente) — evita que un
  agente MCP pueda backdatear pedidos; si el examen exige lo contrario se ajusta como entrada de
  Changelog.

## Milestones

- [ ] `Pedido`/`PedidoDetalle` con CRUD (menos actualizar) cubierto por tests unitarios — cliente
      inexistente, producto inexistente, cantidad inválida, cálculo de total con varias líneas, y
      el caso "Many".
- [ ] `McpServerIT` completo y verde: 14 tools listadas en total, flujo feliz de Pedido con total
      verificado, flujos de error de las 3 entidades.

## Tasks

**Slicing strategy**: Horizontal (layered) — igual que M6/M7, este spec es un slice vertical
dentro del plan más grande (`plan-mcp.md`); dentro de él no hay escenarios independientes que
cortar, solo una cadena de dependencias (esquema → dominio → tools → verificación final).

### Foundation (Build First)

- [ ] **Migraciones y entidades `Pedido`/`PedidoDetalle`**
  - **Acceptance**: `V5__crear_pedido.sql` y `V6__crear_pedido_detalle.sql` aplican sobre Postgres
    real; `Pedido.java`/`PedidoDetalle.java` compilan con constructor `protected` vacío (JPA) +
    constructor de dominio package-private; `Pedido` no tiene campo `total`.
  - **Files**: `api/src/main/resources/db/migration/{V5__crear_pedido.sql,V6__crear_pedido_detalle.sql}`,
    `api/src/main/java/com/miapp/pedido/{Pedido,PedidoDetalle}.java`.
  - **Tests**: cubierto indirectamente por los tests de servicio de la siguiente tarea.
  - **Effort**: M

- [ ] **Repositorio, servicio, excepciones y DTOs**
  - **Acceptance**: `PedidoService.crear` valida cliente existente, cada producto existente,
    cantidad>0 antes de persistir; calcula `total` al vuelo y lo devuelve solo en
    `PedidoResponse`; listar/obtener/eliminar implementados; DTOs `record`.
  - **Files**: `pedido/PedidoRepository.java`, `pedido/PedidoService.java`,
    `pedido/dto/{CrearPedido,LineaPedidoInput,PedidoResponse}.java`,
    `pedido/excepciones/{PedidoNoEncontradoException,CantidadInvalidaException}.java`.
  - **Tests**: `PedidoServiceTest` (unit) — happy con una y varias líneas (total correcto),
    cliente inexistente, producto inexistente, cantidad inválida, listar/obtener/eliminar, y el
    caso "Many".
  - **Effort**: L

- [ ] **`PedidoMcpTools`**
  - **Acceptance**: 4 tools (`crearPedido`, `listarPedidos`, `obtenerPedido`, `eliminarPedido`)
    registradas; cada una atrapa las excepciones de dominio y relanza `McpError` con mensaje
    legible en español.
  - **Files**: `pedido/mcp/PedidoMcpTools.java`.
  - **Tests**: cubierto por la extensión final de `McpServerIT`.
  - **Effort**: M

- [ ] **Completar `McpServerIT`**
  - **Acceptance**: cliente MCP (Java SDK) lista exactamente 14 tools; flujo feliz
    crear→listar→obtener→eliminar de Pedido pasa, verificando el total calculado en la respuesta
    de `crearPedido`; flujos de error de cliente inexistente, producto inexistente y cantidad
    inválida devuelven `McpError` con mensaje legible.
  - **Files**: `McpServerIT.java` (extendido, cierre de la Parte 1).
  - **Tests**: es el test de integración final de M6-M8.
  - **Effort**: M

- [ ] **Verificación final de la Parte 1**
  - **Acceptance**: `./gradlew check` verde con JaCoCo ≥90% sobre todo lo nuevo de M6-M8
    combinado.
  - **Files**: n/a (checklist de cierre).
  - **Tests**: n/a — corre la suite completa.
  - **Effort**: XS

## Effort Estimate

**Total Estimated Days**: 0.5–0.75 día (consistente con la tabla de estimados de `plan-mcp.md`).

| Phase | Effort |
|-------|--------|
| Foundation (migraciones/entidades Pedido+PedidoDetalle) | ~0.15 día |
| Features (repo/service con cálculo de total/excepciones/DTOs) | ~0.3 día |
| Integration (tools + cierre `McpServerIT`) | ~0.2 día |
| Testing & Polish (verificación final de la Parte 1) | ~0.1 día |
