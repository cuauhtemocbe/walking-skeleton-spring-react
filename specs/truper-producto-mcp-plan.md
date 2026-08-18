# Implementation Plan: Slice Producto — MCP Server Truper (M7)

**Spec**: `specs/truper-producto-mcp.md`
**Created**: 2026-08-18
**Status**: approved

## Components

### 1. Migración + entidad `Producto`
- **Purpose**: Esquema y modelo de dominio de Producto, siguiendo el patrón package-private ya
  usado por `cliente/Cliente.java`.
- **Files**: `V4__crear_producto.sql`, `producto/Producto.java`.
- **Effort**: S

### 2. Repositorio, servicio, excepciones y DTOs de Producto
- **Purpose**: CRUD completo con validación de código único y precio positivo, mapeo entidad→DTO
  como método estático privado en el servicio (sin clase Mapper aparte).
- **Files**: `producto/ProductoRepository.java`, `producto/ProductoService.java`,
  `producto/dto/{CrearProducto,ActualizarProducto,ProductoResponse}.java`,
  `producto/excepciones/{ProductoNoEncontradoException,CodigoProductoDuplicadoException}.java`.
- **Effort**: M

### 3. `ProductoMcpTools`
- **Purpose**: Exponer las 5 operaciones de Producto como tools MCP, con el mismo manejo explícito
  de excepciones de dominio → `McpError` legible que `ClienteMcpTools`.
- **Files**: `producto/mcp/ProductoMcpTools.java`.
- **Effort**: M

### 4. Extensión de `McpServerIT`
- **Purpose**: Verificar Producto end-to-end contra Postgres real, en el mismo test que ya cubre
  Cliente — no un archivo nuevo.
- **Files**: `api/src/test/java/com/miapp/cliente/McpServerIT.java` (o su ubicación movida a un
  paquete neutral si al implementar se decide que ya no pertenece a `cliente/`).
- **Effort**: M

## Dependencies

### Build Order
1. Migración + entidad `Producto` (componente 1) — sin dependencias previas, infra MCP ya existe.
2. Repositorio/servicio/excepciones/DTOs (componente 2) — depende de la entidad.
3. `ProductoMcpTools` (componente 3) — depende de 2.
4. Extensión de `McpServerIT` (componente 4) — depende de 3, es la verificación final.

### External Dependencies
Ninguna nueva — `spring-ai-starter-mcp-server-webmvc` e `io.modelcontextprotocol.sdk:mcp` ya están
en `build.gradle.kts` desde M6.

## Risks & Assumptions

### Risks
- **Ubicación de `McpServerIT`**: hoy vive en el paquete `cliente/` porque fue el primer slice. Al
  extenderlo con Producto (y luego Pedido en M8) puede convenir moverlo a un paquete neutral
  (`api/src/test/java/com/miapp/mcp/McpServerIT.java`). Mitigación: decisión de bajo riesgo, se
  toma al implementar sin bloquear el resto del slice.

### Assumptions
- El campo `precio_unitario` se modela como `numeric` en Postgres y `BigDecimal` en Java (evita
  errores de redondeo en el cálculo de total que hará M8) — si al implementar M8 esto resulta
  insuficiente, se ajusta ahí, no acá.
- No hay requisito de formato para `codigo` más allá de unicidad, igual que se decidió para `rfc`
  de Cliente en M6.

## Milestones

- [ ] `Producto` con CRUD cubierto por tests unitarios (sin MCP todavía) — código duplicado, no
      encontrado, precio inválido, y el caso "Many" preservando orden.
- [ ] `McpServerIT` extendido verde: 10 tools listadas en total, flujo feliz completo de Producto,
      flujo de error con mensaje legible.

## Tasks

**Slicing strategy**: Horizontal (layered) — igual que M6, este spec es en sí mismo un slice
vertical dentro del plan más grande (`plan-mcp.md`); dentro de él no hay escenarios independientes
que cortar, solo una cadena de dependencias (esquema → dominio → tools → verificación).

### Foundation (Build First)

- [ ] **Migración y entidad `Producto`**
  - **Acceptance**: `V4__crear_producto.sql` aplica sobre Postgres real; `Producto.java` compila
    con constructor `protected` vacío (JPA) + constructor de dominio package-private, sin setters.
  - **Files**: `api/src/main/resources/db/migration/V4__crear_producto.sql`,
    `api/src/main/java/com/miapp/producto/Producto.java`.
  - **Tests**: cubierto indirectamente por los tests de servicio de la siguiente tarea.
  - **Effort**: S

- [ ] **Repositorio, servicio, excepciones y DTOs**
  - **Acceptance**: `ProductoService` implementa crear/listar/buscar/actualizar/eliminar; código
    duplicado lanza `CodigoProductoDuplicadoException`; id inexistente lanza
    `ProductoNoEncontradoException`; precio ≤0 rechazado por Bean Validation; DTOs `record`.
  - **Files**: `producto/ProductoRepository.java`, `producto/ProductoService.java`,
    `producto/dto/{CrearProducto,ActualizarProducto,ProductoResponse}.java`,
    `producto/excepciones/{ProductoNoEncontradoException,CodigoProductoDuplicadoException}.java`.
  - **Tests**: `ProductoServiceTest` (unit) — casos happy por operación, código duplicado, no
    encontrado, precio inválido, y el caso "Many".
  - **Effort**: M

- [ ] **`ProductoMcpTools`**
  - **Acceptance**: 5 tools (`crearProducto`, `listarProductos`, `buscarProducto`,
    `actualizarProducto`, `eliminarProducto`) registradas; cada una atrapa las excepciones de
    dominio y relanza `McpError` con mensaje legible en español.
  - **Files**: `producto/mcp/ProductoMcpTools.java`.
  - **Tests**: cubierto por la extensión de `McpServerIT`.
  - **Effort**: M

- [ ] **Extender `McpServerIT`**
  - **Acceptance**: cliente MCP (Java SDK) lista exactamente 10 tools (5 Cliente + 5 Producto);
    flujo feliz crear→listar→buscar→actualizar→eliminar de Producto pasa; un flujo de error
    (código duplicado o no encontrado) devuelve `McpError` con mensaje legible.
  - **Files**: `McpServerIT.java` (extendido, no nuevo).
  - **Tests**: es el test de integración.
  - **Effort**: M

- [ ] **Verificación final del slice**
  - **Acceptance**: `./gradlew check` verde con JaCoCo ≥90% sobre el código nuevo.
  - **Files**: n/a (checklist de cierre).
  - **Tests**: n/a — corre la suite completa.
  - **Effort**: XS

## Effort Estimate

**Total Estimated Days**: 0.3–0.5 día (consistente con la tabla de estimados de `plan-mcp.md` —
más rápido que M6 porque la infraestructura MCP ya existe).

| Phase | Effort |
|-------|--------|
| Foundation (migración/entidad Producto) | ~0.1 día |
| Features (repo/service/excepciones/DTOs) | ~0.15 día |
| Integration (tools + extensión `McpServerIT`) | ~0.2 día |
| Testing & Polish (verificación final, gates) | ~0.05 día |
