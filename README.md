# walking-skeleton-spring-react

Walking skeleton: React/TypeScript → Spring Boot 4 → Postgres, y de vuelta.

Cero funcionalidad de negocio. El valor está en que **cada frontera del sistema esté
conectada, verificada y reproducible desde un clon limpio**. La entidad del dominio
(`Nota`) es deliberadamente trivial y desechable: se sustituye por la entidad real recién
cuando los cinco criterios de abajo pasen.

## El skeleton está terminado cuando

1. Un usuario escribe texto en un input de React, pulsa un botón y el dato queda persistido en Postgres.
2. Al recargar la página, ese dato se lee de la base de datos y se muestra.
3. Los tipos de TypeScript del cliente están **generados** desde el esquema OpenAPI del backend, no escritos a mano.
4. Existe al menos un test de integración que arranca Postgres real vía Testcontainers y pasa en verde.
5. `docker compose up` + dos comandos levantan el entorno completo desde un clon limpio.

Si cualquiera de los cinco falla, el skeleton no camina.

## Estado

En construcción. El backlog vive en
[GitHub Issues](https://github.com/cuauhtemocbe/walking-skeleton-spring-react/issues),
organizado en milestones **M0 → M12**. No se avanza de hito sin que el anterior pase su
criterio de aceptación: ese es el punto entero del skeleton — cuando algo falla, que haya
una sola cosa nueva que pueda ser la causa.

M0–M5 cerraron el walking skeleton original (`Nota` end-to-end, ver arriba). A partir de M6
el repo evoluciona hacia la gestión de pedidos Truper de un examen técnico: la entidad
`Nota` se retiró y el dominio (Cliente/Producto/Pedido) se expone **solo como herramientas
MCP, no como REST** — ver la sección [Gestión de pedidos Truper (MCP Server)](#gestión-de-pedidos-truper-mcp-server)
más abajo y `plan-mcp.md` para el plan completo M6–M12.

| Milestone | Qué hace | Estado |
|---|---|---|
| M6 — Slice Cliente | Retira `Nota`, agrega infraestructura MCP + 5 tools de Cliente | Cerrado (#28) |
| M7 — Slice Producto | 5 tools de Producto | Cerrado (#29) |
| M8 — Slice Pedido + PedidoDetalle | 4 tools de Pedido (total calculado al vuelo) | Cerrado (#30) |
| M9 — Verificación con Claude Code | Demo de aceptación de la Parte 1 contra Claude Code real | En curso (#31) |
| M10 — Backend `agent/` (FastAPI + OpenAI) | Parte 2: agente de chat propio, mismo MCP Server | Pendiente |
| M11 — Frontend de chat en React | Reemplaza la UI de `Nota` por un chat | Pendiente |
| M12 — Gates end-to-end | `make validate` cubre `api`+`web`+`agent` | Pendiente |

## Alcance

**Dentro:** un endpoint de escritura y uno de lectura, una entidad, una migración Flyway,
un formulario y una lista en React, generación de tipos desde OpenAPI, un test de
integración con Testcontainers, manejo de error global con cuerpo consistente.

**Fuera, a propósito:** autenticación y Spring Security, paginación, filtros, roles,
multi-tenancy, caché, colas, despliegue remoto, y estilos más allá de HTML legible.
Cualquiera de estos se añade **después** de los cinco criterios, y en ese orden:
CRUD completo → TanStack Query → Spring Security → CI.

La autenticación va después del CRUD por una razón concreta: cuando algo falle en Security,
conviene que el resto del sistema ya sea territorio conocido y descartable como causa.

## Este repo NO es de grado productivo — y es a propósito

Es un banco de pruebas para desarrollo local y prueba de concepto. Las siguientes
ausencias son **decisiones documentadas, no descuidos**. No las "arregles" sin contexto:

| Ausencia | Por qué |
|---|---|
| Sin GitHub Actions | El gate es el hook de `pre-push` hacia `main`, que corre `make validate`. Un repo de un solo colaborador y sin usuarios reales no necesita CI hosteado. |
| Sin `Dockerfile` de producción | Compose levanta **solo Postgres**. `api` y `web` corren en el host (`./gradlew bootRun`, `pnpm dev`) porque el objetivo es velocidad de iteración local, no paridad con producción. |
| Sin `LICENSE`, Dependabot, Trivy, branch protection | Repo desechable, sin superficie de ataque real ni consumidores externos. |
| Sin `CHANGELOG.md` | El historial de commits alcanza para un repo de vida corta. |

Lo que **sí** entra, porque suma calidad sin costar tiempo de setup: tests de integración
reales (Postgres vía Testcontainers, cero mocks de la BD), umbrales de cobertura que fallan
el build, git hooks versionados con profundidad graduada, Conventional Commits validados por
hook, y un `Makefile` autodocumentado como única interfaz de comandos.

Si este repo alguna vez se gradúa a producto, el estándar completo está en
`meta-projects/docs/development-standards.md`.

## Requisitos

- Java 25 (`sdk env install` lee `.sdkmanrc`)
- Node 22+ y pnpm (`nvm use` lee `.nvmrc`)
- Docker con el demonio corriendo

## Uso

Desde un clon limpio, en un directorio vacío:

```bash
git clone https://github.com/cuauhtemocbe/walking-skeleton-spring-react.git
cd walking-skeleton-spring-react

sdk env install       # activa Java 25, lee .sdkmanrc
nvm use                # activa Node, lee .nvmrc

make install-hooks     # activa los git hooks versionados en .githooks/ — no se activan solos al clonar
make up                 # levanta Postgres en Docker y espera a que esté healthy

(make api-run) &
(cd web && pnpm install && pnpm dev)
```

Abrí `http://localhost:5173`: la página muestra el estado de `/api/health` y, debajo, el
formulario y la lista de notas. `make help` lista el resto de los targets disponibles.

### Verificación final (criterios 1 y 2)

Con el stack de arriba corriendo:

1. Escribí un texto en el campo **Nueva nota** y pulsá **Guardar**.
2. La nota aparece en la lista.
3. Recargá la página.
4. La nota sigue apareciendo — se leyó de Postgres, no de estado en memoria.

Esta verificación es **manual a propósito**: el issue #19 (E2E con Playwright que automatiza
este mismo recorrido) no se implementó. `make e2e` existe como target del `Makefile` pero no
tiene ningún test detrás todavía — correrlo no hace nada útil.

## Gestión de pedidos Truper (MCP Server)

A partir de M6 el repo suma un segundo dominio, en paralelo al walking skeleton original: un
**servidor MCP** (Spring AI, sobre el mismo `api/`) que expone Cliente, Producto y Pedido como
14 herramientas MCP vía transporte Streamable HTTP, pensado para ser consumido por un agente
conversacional (Claude Code, u otro cliente MCP) en vez de por REST.

**Decisión deliberada**: este dominio usa Postgres real (Flyway + Testcontainers, la misma
filosofía del resto del repo) en vez de una base en memoria — por lo tanto no hay `/h2-console`
para inspeccionar el estado. La sección [Verificación alternativa](#verificación-alternativa-sin-h2-console)
más abajo documenta el reemplazo: las propias tools de listado y `psql` directo contra el
contenedor.

### Cómo levantar el servidor MCP

Con Java 25 y Docker activos (ver [Requisitos](#requisitos)):

```bash
make up          # levanta Postgres y espera a que esté healthy
make api-run      # corre el backend — expone el servidor MCP en http://localhost:8080/mcp
```

Registrá Claude Code como cliente MCP (una sola vez; queda guardado entre sesiones):

```bash
claude mcp add --transport http truper-pedidos http://localhost:8080/mcp
claude mcp list   # confirma "truper-pedidos: http://localhost:8080/mcp (HTTP) - ✔ Connected"
```

Con eso, cualquier sesión de Claude Code en este entorno puede invocar las 14 tools
directamente en la conversación.

### Tools MCP generadas

Las 14 tools están definidas con `@McpTool`/`@McpToolParam` sobre `@Component`s planos en
`cliente/mcp/ClienteMcpTools.java`, `producto/mcp/ProductoMcpTools.java` y
`pedido/mcp/PedidoMcpTools.java`. Las excepciones de dominio (cliente/producto/pedido no
encontrado, RFC o código duplicado, cantidad inválida) se atrapan en la tool y se relanzan como
`McpError` con mensaje legible — Spring AI envuelve cualquier excepción no atrapada en un
mensaje genérico inútil para el agente ([mcp-annotations#52](https://github.com/spring-ai-community/mcp-annotations/issues/52)).

**Cliente** (5 tools)

| Tool | Descripción |
|---|---|
| `crearCliente` | Da de alta un cliente nuevo con su RFC y razón social |
| `listarClientes` | Lista todos los clientes registrados |
| `buscarCliente` | Busca un cliente por su RFC; devuelve `null` si no existe |
| `actualizarCliente` | Actualiza los datos de un cliente existente |
| `eliminarCliente` | Elimina un cliente por su id |

**Producto** (5 tools)

| Tool | Descripción |
|---|---|
| `crearProducto` | Da de alta un producto nuevo con su código único y precio |
| `listarProductos` | Lista todos los productos registrados |
| `buscarProducto` | Busca un producto por su código; devuelve `null` si no existe |
| `actualizarProducto` | Actualiza los datos de un producto existente |
| `eliminarProducto` | Elimina un producto por su id |

**Pedido** (4 tools — sin `actualizar`, igual que el diagrama de referencia del examen)

| Tool | Descripción |
|---|---|
| `crearPedido` | Crea un pedido para un cliente existente con una o más líneas de producto y devuelve el total calculado |
| `listarPedidos` | Lista todos los pedidos registrados, incluyendo su total calculado |
| `obtenerPedido` | Obtiene un pedido por su id, incluyendo su total calculado; devuelve `null` si no existe |
| `eliminarPedido` | Elimina un pedido por su id |

`Pedido.total` **no es una columna persistida**: la tabla `pedido` solo tiene `id`,
`cliente_id`, `fecha`. El total se calcula al vuelo en `PedidoService`
(`Σ cantidad × precioUnitario` sobre `pedido_detalle`) y solo aparece en la respuesta de las
tools, nunca en la entidad JPA ni en la migración Flyway.

### Escenario de prueba

Con el servidor arriba y `truper-pedidos` registrado, cargá un catálogo mínimo (una vez) y
corré el prompt de la demo del examen:

```
Da de alta el producto MART-001 "Martillo" a $150.00 y SERR-001 "Serrucho" a $220.00.
```

```
Crea un pedido: agrégame tres martillos y cuatro serruchos.
```

Comportamiento esperado (verificado end-to-end contra Postgres real):

1. Si el cliente todavía no existe, Claude Code entrevista antes de continuar — pide RFC y
   razón social y da de alta con `crearCliente` — antes de armar el pedido.
2. Arma las líneas correctas (3× Martillo, 4× Serrucho) y llama a `crearPedido`.
3. El total devuelto coincide con `3×150.00 + 4×220.00 = 1330.00`:

```json
{
  "id": 2,
  "clienteId": 1,
  "fecha": "2026-08-18",
  "lineas": [
    { "productoId": 1, "cantidad": 3, "precioUnitario": 150.00, "subtotal": 450.00 },
    { "productoId": 2, "cantidad": 4, "precioUnitario": 220.00, "subtotal": 880.00 }
  ],
  "total": 1330.00
}
```

### Verificación alternativa (sin `/h2-console`)

Como no hay base en memoria, el estado se inspecciona con dos mecanismos:

**1. Las propias tools de listado**, desde la conversación con Claude Code: `listarClientes`,
`listarProductos`, `listarPedidos`.

**2. `psql` directo contra el contenedor de Postgres** (con `make up` corriendo):

```bash
docker compose exec postgres psql -U notas -d notas -c "select * from pedido;"
docker compose exec postgres psql -U notas -d notas -c "select * from pedido_detalle;"
```

Nótese que `pedido` no tiene columna `total` (se calcula al vuelo, ver arriba) — el total se
verifica cruzando `pedido_detalle` contra `producto.precio_unitario`, o simplemente confiando
en la respuesta ya verificada de `crearPedido`/`obtenerPedido`/`listarPedidos`.

### Validación

`make validate` es el pipeline único de este repo (no hay CI hosteado, ver la tabla de más
abajo): typecheck + lint + tests de backend con el gate de cobertura de JaCoCo (#17) + tests
de frontend con el gate de cobertura de Vitest (#18). Es lo mismo que corre el hook de
`pre-push` hacia `main`, y corre igual desde la terminal o desde el hook. Deliberadamente no
incluye el E2E — ver la sección anterior.

### Tests de integración del backend

`./gradlew test` levanta su propio Postgres real vía Testcontainers para los tests de
integración (`McpServerIT`, cliente MCP real contra las 14 tools) — no hace falta
`docker compose up` antes. Para desarrollo local, reusar el contenedor entre corridas
baja el arranque a casi cero: creá `~/.testcontainers.properties` (de tu máquina, no del repo)
con `testcontainers.reuse.enable=true`.

### Cobertura de tests del backend

`./gradlew check` corre los tests y verifica cobertura en el mismo comando: JaCoCo hace fallar
el build si la cobertura de línea baja de **90%** sobre el código fuente de `api/`. Excluidas:
`ApiApplication` (solo tiene el `main`) y `config/**`, salvo `ManejadorErrores` — ese sí tiene
lógica (arma el `ProblemDetail`) y debe estar cubierto. El reporte HTML navegable queda en
`api/build/reports/jacoco/test/html/index.html`.

### Cobertura de tests del frontend

`pnpm test:coverage` (o `make test-web`) corre la suite de Vitest y hace fallar el comando si
la cobertura baja de **80%** en cualquiera de las cuatro métricas (statements, branches,
functions, lines). Excluidos: `src/api/schema.d.ts` (generado), `src/main.tsx` (bootstrap) y
los archivos `*.config.*`. Los tests conviven con el código que cubren:
`Componente.tsx` + `Componente.test.tsx` en el mismo directorio. El reporte HTML navegable
queda en `web/coverage/index.html`.

### Generar los tipos del cliente

`make gen-api` (o `pnpm gen:api` desde `web/`) regenera `web/src/api/schema.d.ts` leyendo
`/v3/api-docs` del backend. **El backend tiene que estar corriendo** (`make api-run`): el
script lee del endpoint HTTP, no de un archivo. El resultado se commitea — así el diff
muestra los cambios de contrato en la revisión del PR.

Con el backend corriendo, `http://localhost:8080/swagger-ui/index.html` sirve la UI
interactiva de Swagger para explorar y probar los endpoints.
