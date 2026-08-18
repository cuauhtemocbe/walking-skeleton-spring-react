# Plan: Evolucionar walking-skeleton → Gestión de pedidos Truper (MCP Server + chat propio)

## Contexto

El walking skeleton (`api/` Spring Boot 4.1.0/Java 25, `web/` React 19/pnpm, Postgres vía
docker-compose) ya cumple sus 5 criterios de "terminado" — M0 a M5 están cerrados en GitHub salvo
el issue #19 (E2E Playwright, manual a propósito). La entidad `Nota` fue siempre "deliberadamente
trivial y desechable", pensada para sustituirse "recién cuando los cinco criterios... pasen" — ya
pasaron.

El examen técnico (`Examen_Desarrollador_Lider_IA.pdf`) pide construir la **gestión de pedidos de
Truper** en 2 partes:

- **Parte 1**: un **MCP Server** (Spring Boot + Spring AI) que expone CRUD de Cliente/Producto/
  Pedido como 14 herramientas MCP sobre transporte Streamable HTTP, consumido directamente por
  Claude Code (que actúa de agente entrevistador).
- **Parte 2**: una app de chat propia — React (nueva UI) + un backend **FastAPI** en Python con un
  agente OpenAI que usa el mismo MCP Server como cliente MCP, con la API key solo en backend.

**Decisiones ya tomadas:**

1. **Postgres real** (no H2 en memoria) — se mantiene Flyway + Testcontainers, la filosofía actual
   del repo. Esto es una **desviación deliberada** del diagrama de referencia del examen (que pide
   H2 + verificación en `/h2-console`); se documenta explícitamente en el README y se reemplaza esa
   verificación por dos mecanismos: las propias tools MCP de listado, y `psql` contra el contenedor
   de Postgres.
2. **Alcance: ambas partes**, en milestones nuevos que continúan la numeración M6+.
3. El dominio Truper se expone **solo como herramientas MCP, no como REST** — así lo pide el
   enunciado. Eso deja sin sujeto al flujo M4 actual (`pnpm gen:api` / `web/src/api/schema.d.ts` /
   `EsquemaOpenApiTests`), que existía únicamente para tipar el CRUD REST de `Nota`. Se **retiran**
   en vez de dejarlos apuntando solo a `/api/health`.
4. **`Pedido.total` no es una columna persistida.** El esquema de referencia del examen para la
   tabla `PEDIDO` solo tiene `id`, `cliente_id`, `fecha` — sin `total`. El enunciado dice "calcula
   su total" (verbo), y el diagrama de secuencia muestra `Pedido { total: 700 }` como la
   **respuesta** de `crearPedido`, no como algo guardado. El total se calcula al vuelo
   (`Σ cantidad × precio_unitario` sobre `PEDIDO_DETALLE`) en el service y solo aparece en
   `PedidoResponse`, nunca en la entidad JPA ni en la migración Flyway.
5. **Parte 1 avanza en vertical slices, por entidad, no por capa.** En vez de modelar las 4
   entidades completas y recién después exponer las 14 tools MCP (dos milestones horizontales sin
   nada demostrable hasta que ambos terminan), cada milestone de Parte 1 corta **una entidad
   completa de punta a punta**: migración → entidad → repo → service → excepciones → tools MCP →
   verificación con un cliente MCP real contra Postgres. Así hay un incremento demostrable después
   de cada milestone, no solo al final de M8. Orden de slices: Cliente → Producto → Pedido (depende
   de los dos anteriores, cierra el ciclo).

**Investigación técnica ya verificada** (contra documentación oficial, agosto 2026):

- Spring AI **2.0.0 GA** (12 jun 2026) está alineado con Spring Boot 4.1.0 — mismo que ya usa el
  proyecto, sin fricción de versión.
- Starter correcto: `org.springframework.ai:spring-ai-starter-mcp-server-webmvc` (coexiste con
  `spring-boot-starter-webmvc`), BOM `spring-ai-bom:2.0.0`.
- Tools se definen con `@McpTool`/`@McpToolParam` sobre métodos de un `@Component` normal.
- Transporte Streamable HTTP se activa con `spring.ai.mcp.server.protocol: STREAMABLE` (no `SSE`,
  deprecado). Path por defecto `/mcp` → `http://localhost:8080/mcp`.
- Registro en Claude Code: `claude mcp add --transport http truper-pedidos http://localhost:8080/mcp`.
- Python: paquete `mcp` 2.0.0 (`mcp.Client`), `openai` 3.3.0, `fastapi` 0.141.1, `sse-starlette`
  3.4.8. El `inputSchema` de una tool MCP ya es JSON Schema — se mapea directo a formato
  function-calling de OpenAI sin conversor especial.
- Bug conocido: si una excepción de dominio se deja propagar tal cual desde un método `@McpTool`,
  Spring AI la envuelve en un mensaje genérico e inútil para el agente
  ([spring-ai-community/mcp-annotations#52](https://github.com/spring-ai-community/mcp-annotations/issues/52)).
  Hay que atraparla explícitamente en la capa de tools y relanzar `McpError` con mensaje legible.

### Fuera de alcance

Heredado de `CLAUDE.md`, reafirmado acá porque este plan reemplaza el dominio entero y no debería
quedar ambiguo leyendo solo este archivo: autenticación/Spring Security, paginación, filtros,
roles, multi-tenancy, caché, colas, despliegue remoto. Tampoco entra precarga/seed del catálogo de
productos (el examen exige alta solo vía MCP) ni tests E2E automatizados (Playwright) para el
dominio Truper — la verificación end-to-end es manual, igual que se decidió para `Nota` en #19.

---

## Patrón a replicar (ya establecido por `nota/`)

Paquete por feature en `api/src/main/java/com/miapp/<feature>/`: entidad JPA package-private sin
setters (constructor vacío `protected` para JPA + constructor de dominio package-private), DTOs
`record` públicos en `dto/` con validación Bean Validation, `Repository` vacío extendiendo
`JpaRepository`, `Service` con mapeo entidad→DTO como método estático privado (sin clase Mapper
aparte), constructor injection sin `@Autowired`. Ver `api/src/main/java/com/miapp/nota/NotaService.java`
como referencia exacta y `api/src/test/java/com/miapp/nota/NotaIT.java` como referencia del patrón
de test de integración con Testcontainers (`@SpringBootTest(RANDOM_PORT)` + `@Testcontainers` +
`PostgreSQLContainer` real, cero mocks de BD). `ManejadorErrores` (`api/src/main/java/com/miapp/config/ManejadorErrores.java`)
se mantiene sin cambios — sigue cubriendo el único endpoint REST real que queda, `/api/health`.

---

## Milestones (continúan M0–M5 ya cerrados)

### Estimado de esfuerzo

Tallas orientativas por milestone, en sesiones/días de trabajo (no full-time), para decidir qué
cabe en la ventana de 2h del examen vs. qué queda para después. Con vertical slicing, **M6 por sí
solo ya es una entrega parcial demostrable** (una entidad completa, de punta a punta, verificada
con un cliente MCP real) — si el tiempo se agota a mitad de Parte 1, hay algo funcionando en vez de
tres capas a medio terminar.

| Milestone | Talla | Estimado |
|---|---|---|
| M6 — Slice Cliente (retira Nota + Cliente end-to-end) | M | 0.5–0.75 día |
| M7 — Slice Producto | S | 0.3–0.5 día |
| M8 — Slice Pedido + PedidoDetalle | M | 0.5–0.75 día |
| M9 — Verificación con Claude Code | S | 0.25–0.5 día |
| M10 — Backend `agent/` | L | 1.5–2 días |
| M11 — Frontend de chat | M | 1–1.5 días |
| M12 — Gates end-to-end | S | 0.5 día |

**Total**: ~5–7 días de trabajo en sesiones cortas. M6–M9 (Parte 1 completa) es lo mínimo
razonable para una entrega parcial si el tiempo se agota — el propio examen dice que no es
obligatorio completar todo. Si incluso eso es mucho, **M6 solo** ya demuestra el patrón end-to-end
completo (migración → entidad → MCP → verificación) sobre una entidad real.

### M6 — Slice Cliente (retira Nota, primera entidad end-to-end)

Primer slice vertical: agrega toda la infraestructura MCP y la recorre con una sola entidad.

1. Borra `com.miapp.nota` (main y test) y agrega `V2__eliminar_nota.sql` (`DROP TABLE nota;` —
   nunca se edita `V1__crear_nota.sql`, ya aplicada).
2. Agrega `V3__crear_cliente.sql`, entidad `Cliente` + `ClienteRepository` + `ClienteService`
   (patrón `nota/`), excepciones de dominio `ClienteNoEncontradoException` y
   `RfcDuplicadoException` (RFC único).
3. Agrega `spring-ai-starter-mcp-server-webmvc` + BOM `spring-ai-bom:2.0.0` a
   `build.gradle.kts`, config `spring.ai.mcp.server.protocol: STREAMABLE` en `application.yaml`
   (`name`, `version`, `type: SYNC`, `instructions`) — se hace acá, la primera vez que se necesita,
   no en un milestone aparte.
4. Crea `cliente/mcp/ClienteMcpTools.java` con las 5 tools (crear/listar/buscar/actualizar/
   eliminar), cada una atrapando las excepciones de dominio y relanzando `McpError` con mensaje
   legible en español.
5. Crea `McpServerIT` (Testcontainers + Postgres real + `io.modelcontextprotocol.sdk:mcp` como
   cliente de test) cubriendo el flujo feliz y un flujo de error de Cliente.

**Cierra cuando**: un cliente MCP contra `http://localhost:8080/mcp` lista exactamente 5 tools y
ejecuta crear/listar/buscar/actualizar/eliminar Cliente contra Postgres real; `./gradlew check`
verde (JaCoCo ≥90%); `grep -ri nota api/src web/src README.md` no devuelve nada.

### M7 — Slice Producto

Mismo corte completo que M6, repitiendo el patrón ya establecido (infraestructura MCP ya existe,
este slice es más rápido). Agrega `V4__crear_producto.sql`, entidad `Producto` + repo + service
(código único), excepciones `ProductoNoEncontradoException` y `CodigoProductoDuplicadoException`,
y `producto/mcp/ProductoMcpTools.java` (5 tools, mismo patrón que Cliente). Extiende `McpServerIT`
con el flujo feliz y un flujo de error de Producto.

**Cierra cuando**: el cliente MCP lista 10 tools totales (5 Cliente + 5 Producto) y el flujo
completo de Producto pasa igual que el de Cliente; `./gradlew check` verde.

### M8 — Slice Pedido + PedidoDetalle

Cierra Parte 1: depende de Cliente y Producto ya existentes. Agrega `V5__crear_pedido.sql` (solo
`id`/`cliente_id`/`fecha`, **sin** `total`) y `V6__crear_pedido_detalle.sql`, entidades `Pedido`
(`id`, `cliente` `@ManyToOne`, `fecha`, `detalles` `@OneToMany(cascade=ALL, orphanRemoval=true)` —
sin campo `total`) y `PedidoDetalle` + repo + service, excepciones `PedidoNoEncontradoException` y
`CantidadInvalidaException`. `PedidoService.crear` valida cliente+producto+cantidad>0 y calcula el
total al vuelo (`Σ cantidad × precioUnitario`, nunca persistido). Crea `pedido/mcp/PedidoMcpTools.java`
(4 tools: crear/listar/obtener/eliminar — sin actualizar, igual que el diagrama de referencia del
examen). Completa `McpServerIT` con el flujo feliz de Pedido (incluye verificar el total calculado
en la respuesta) y sus flujos de error.

**Cierra cuando**: el cliente MCP lista exactamente 14 tools totales; `McpServerIT` cubre las 3
entidades con flujo feliz + error cada una; `./gradlew check` verde (JaCoCo ≥90% sobre todo lo
nuevo).

### Paralelización

M6–M8 son estrictamente secuenciales: M8 (Pedido) depende de que Cliente y Producto ya existan
(FK en `pedido`/`pedido_detalle`), y los tres slices tocan los mismos archivos compartidos
(`build.gradle.kts`, `application.yaml`, `McpServerIT`) — paralelizarlos generaría conflictos de
merge por un ahorro mínimo, dado que cada slice ya es chico (0.3–0.75 día).

**M9 y M10 sí se pueden paralelizar** una vez cerrado M8: M9 es demo manual + documentación en
README (no toca código de `api/` más allá de lo ya cerrado), M10 es un módulo Python nuevo e
independiente (`agent/`) que solo necesita el MCP server ya funcionando, no el resultado de M9. No
comparten archivos ni se bloquean entre sí. M11 (frontend) sí depende de que M10 tenga el endpoint
`/chat` funcionando, así que ese no se adelanta.

### M9 — Verificación con Claude Code

`claude mcp add --transport http truper-pedidos http://localhost:8080/mcp`, registrar productos por
lote, y correr el prompt "Crea un pedido: agrégame tres martillos y cuatro serruchos" verificando la
entrevista (cliente nuevo → pide RFC/razón social → alta → pedido con total correcto). **Cierra
cuando**: el flujo queda documentado en el README con la secuencia exacta de comandos y el mecanismo
de verificación alternativo a `/h2-console` (tools de listado + `psql`).

### M10 — Backend `agent/` (FastAPI + OpenAI + cliente MCP)

Scaffolding Poetry en `agent/` (estructura: `agent/main.py`, `config.py`, `mcp_client.py`,
`openai_agent.py`, `chat.py`, `schemas.py`, `tests/`). `mcp_client.py` envuelve `mcp.Client` y
convierte `list_tools()` a formato function-calling de OpenAI. `openai_agent.py` implementa el loop
manual (Chat Completions + `tool_calls` → ejecutar contra MCP → agregar `role="tool"` → repetir).
Una sesión `mcp.Client` por request de chat (no global compartida). `POST /chat` en FastAPI streamea
por SSE. API key de OpenAI solo por variable de entorno, nunca en el cliente. **Cierra cuando**:
`POST /chat` reproduce vía HTTP el mismo flujo de M9 contra el MCP server real, con tests unitarios
mockeando `openai` y `mcp.Client`.

### M11 — Frontend de chat en React

Retira `FormularioNota`/`ListaNotas`/`useNotas`/`web/src/api/*` (cliente.ts, schema.d.ts, tests) y
el script `gen:api` — el dominio Truper no tiene REST que tipar. Nueva UI de chat: `useChatStream`
(fetch + `ReadableStream` parseando SSE — `EventSource` no sirve porque el request es POST),
componentes de lista de mensajes y campo de chat, con indicación visual de llamadas a tools
("consultando productos…"). Proxy `/chat` → `http://localhost:8000` en `vite.config.ts`, mismo
patrón que el proxy `/api` actual. **Cierra cuando**: la demo end-to-end de M9 se reproduce desde la
UI propia, y `pnpm test:coverage` pasa el umbral no negociable del repo (80/80/80/80
statements/branches/functions/lines) sobre los componentes nuevos de chat — el umbral existente no
se relaja solo porque `Nota` se retiró.

### M12 — Gates end-to-end

Nuevos targets de Makefile: `agent-install`, `agent-run`, `test-agent` (pytest + `--cov-fail-under=80`),
`lint-agent` (ruff). `validate` pasa a incluir `test-agent`+`lint-agent`. `.gitignore` +=
sección Python (`agent/.venv/`, `**/__pycache__/`, `agent/.pytest_cache/`, etc. — `.env` ya cubre
`agent/.env`). `pre-commit` extiende el bloque de Biome con uno análogo para `agent/**/*.py` (ruff
check --fix + format sobre staged). El `README.md` del repo se actualiza con los nuevos "criterios
de que el skeleton camina" para el dominio Truper, y lleva la **nota breve de cómo correrlo y qué se
logró** que pide la entrega del examen (el `CLAUDE.md` del repo está gitignored — no viaja en el
código entregado, así que esa nota no puede vivir ahí). **Cierra cuando**: `make validate` desde
clon limpio corre `api`+`web`+`agent` (typecheck/lint/tests/cobertura) en verde.

---

## Archivos/paquetes Java concretos (M6–M8)

```
api/src/main/java/com/miapp/
  cliente/{Cliente,ClienteRepository,ClienteService}.java        # M6
          dto/{CrearCliente,ActualizarCliente,ClienteResponse}.java
          excepciones/{ClienteNoEncontradoException,RfcDuplicadoException}.java
          mcp/ClienteMcpTools.java
  producto/{Producto,ProductoRepository,ProductoService}.java    # M7
           dto/{CrearProducto,ActualizarProducto,ProductoResponse}.java
           excepciones/{ProductoNoEncontradoException,CodigoProductoDuplicadoException}.java
           mcp/ProductoMcpTools.java
  pedido/{Pedido,PedidoDetalle,PedidoRepository,PedidoService}.java   # M8
         dto/{CrearPedido,LineaPedidoInput,PedidoResponse}.java
         excepciones/{PedidoNoEncontradoException,CantidadInvalidaException}.java
         mcp/PedidoMcpTools.java
```

`Pedido` (entidad JPA): `id`, `cliente` (`@ManyToOne`), `fecha`, `detalles`
(`@OneToMany(cascade=ALL, orphanRemoval=true)`) — **sin** campo `total`. `PedidoResponse` (DTO) sí
tiene `total`, calculado en `PedidoService` sumando `cantidad × precioUnitario` de los detalles.

`build.gradle.kts` agrega BOM `spring-ai-bom:2.0.0` + `implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")`
+ `testImplementation("io.modelcontextprotocol.sdk:mcp")` — en M6, la primera vez que se necesita.
`application.yaml` agrega bloque `spring.ai.mcp.server` (`name`, `version`, `protocol: STREAMABLE`,
`type: SYNC`, `instructions`), también en M6.

Migraciones, en orden: `V1__crear_nota.sql` (ya aplicada) → `V2__eliminar_nota.sql` (M6) →
`V3__crear_cliente.sql` (M6) → `V4__crear_producto.sql` (M7) → `V5__crear_pedido.sql` (M8) →
`V6__crear_pedido_detalle.sql` (M8).

## Estructura `agent/` (Poetry, ya instalado en el host)

```
agent/
  pyproject.toml        # fastapi 0.141.1, uvicorn, openai 3.3.0, mcp 2.0.0, sse-starlette 3.4.8
  poetry.lock            # commiteado
  .env.example
  agent/{main,config,mcp_client,openai_agent,chat,schemas}.py
  tests/{conftest,test_mcp_client,test_openai_agent,test_chat_endpoint}.py
```

Sin `Dockerfile` — corre en host (`poetry run uvicorn agent.main:app --reload --port 8000`),
consistente con la fila ya documentada del README ("sin Dockerfile de producción, prioridad a
velocidad de iteración local").

---

## Definición de terminado (ambas partes)

Checklist a nivel feature, más allá del "cierra cuando" de cada milestone individual — es lo que
se revisa antes de dar por entregado el examen completo:

- [ ] `grep -ri nota api/ web/ README.md` no devuelve nada — cero rastro del dominio anterior.
- [ ] Las 14 herramientas MCP responden con descripciones claras de tool y de cada parámetro,
      verificado manualmente listándolas desde un cliente MCP.
- [ ] Cada caso de error (cliente inexistente, producto no encontrado, cantidad inválida) devuelve
      un mensaje legible al agente, no la envoltura genérica del bug conocido de `mcp-annotations#52`.
- [ ] Demo de aceptación de la Parte 1 corrida de punta a punta desde Claude Code.
- [ ] Demo de aceptación de la Parte 2 corrida de punta a punta desde el chat propio, con la misma
      conversación.
- [ ] La API key de OpenAI nunca aparece en requests/responses hacia el navegador (verificado en
      la pestaña Network).
- [ ] `make validate` desde un clon limpio pasa en verde para `api` + `web` + `agent` (incluye
      JaCoCo ≥90%, Vitest 80/80/80/80, pytest ≥80%).
- [ ] README actualizado con la nota breve de cómo correrlo y qué se logró, que pide la entrega
      del examen (recordar: `CLAUDE.md` está gitignored, no sirve como lugar para esa nota).

---

## Verificación end-to-end

1. **M6–M8**: después de cada slice, `make test-api` verde con JaCoCo ≥90%; `McpClient` de prueba
   (Java SDK, en el IT) lista las tools de esa entidad y ejecuta su flujo feliz + un flujo de error
   contra Postgres real (Testcontainers). Al cerrar M8, las 14 tools están verificadas en conjunto.
2. **M9**: con `make up` + `make api-run` corriendo, `claude mcp add --transport http truper-pedidos
   http://localhost:8080/mcp`, registrar productos, correr el prompt de la demo del examen, y
   verificar persistencia con `docker compose exec postgres psql -U notas -d notas -c "select * from
   pedido;"`.
3. **M10–M11**: con `agent-run` (:8000) y `web-dev` (:5173) corriendo, reproducir la misma
   conversación desde el chat propio; confirmar que la API key de OpenAI nunca aparece en
   requests/responses hacia el navegador (inspeccionar Network tab).
4. **M12**: `make validate` desde un clon limpio (sin Postgres corriendo de antes) pasa en verde
   para los 3 módulos.
