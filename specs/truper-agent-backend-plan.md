# Implementation Plan: Backend `agent` — FastAPI + OpenAI + cliente MCP (M10)

**Spec**: `specs/truper-agent-backend.md`
**Created**: 2026-08-18
**Status**: approved

## Components

### 1. Scaffolding Poetry
- **Purpose**: Estructura base del proyecto Python, dependencias fijadas y commiteadas.
- **Files**: `agent/pyproject.toml`, `agent/poetry.lock`, `agent/.env.example`.
- **Effort**: S

### 2. `config.py`
- **Purpose**: Única fuente de configuración desde variables de entorno (API key de OpenAI, URL
  del MCP server) — nada hardcodeado en el resto del código.
- **Files**: `agent/agent/config.py`.
- **Effort**: XS

### 3. `mcp_client.py`
- **Purpose**: Wrapper de `mcp.Client` que expone `list_tools()` ya convertido a formato
  function-calling de OpenAI, y ejecución de una tool call individual contra el MCP server.
- **Files**: `agent/agent/mcp_client.py`.
- **Effort**: M

### 4. `openai_agent.py`
- **Purpose**: Loop manual de tool-calling: Chat Completions → `tool_calls` → ejecutar contra
  `mcp_client` → agregar `role="tool"` → repetir hasta respuesta final sin tool calls.
- **Files**: `agent/agent/openai_agent.py`.
- **Effort**: L

### 5. `chat.py` + `schemas.py` + `main.py`
- **Purpose**: Contrato y endpoint HTTP: `POST /chat` streamea la respuesta del agente por SSE.
- **Files**: `agent/agent/{chat,schemas,main}.py`.
- **Effort**: M

### 6. Tests unitarios
- **Purpose**: Cobertura ≥80% mockeando `openai` y `mcp.Client` — sin llamadas reales a ninguno de
  los dos.
- **Files**: `agent/tests/{conftest,test_mcp_client,test_openai_agent,test_chat_endpoint}.py`.
- **Effort**: M

## Dependencies

### Build Order
1. Scaffolding Poetry (componente 1) — sin dependencias previas.
2. `config.py` (componente 2) — depende del scaffolding.
3. `mcp_client.py` (componente 3) — depende de `config.py` para la URL del MCP server.
4. `openai_agent.py` (componente 4) — depende de `mcp_client.py` y de `config.py` (API key).
5. `chat.py`/`schemas.py`/`main.py` (componente 5) — depende de `openai_agent.py`.
6. Tests unitarios (componente 6) — se escriben junto con cada componente (TDD), no como fase
   final aparte; se listan al final solo porque cierran la cobertura del milestone completo.

### External Dependencies
- `openai` 3.3.0: se integra en el paso 4.
- `mcp` 2.0.0: se integra en el paso 3.
- `fastapi` 0.141.1 + `sse-starlette` 3.4.8: se integran en el paso 5.

## Risks & Assumptions

### Risks
- **Primera integración real del SDK `mcp` de Python contra el servidor Spring AI de M6-M8**: la
  compatibilidad se verificó contra documentación oficial (`plan-mcp.md`), pero es la primera vez
  que se prueba en este repo desde el lado Python. Mitigación: escribir un smoke test manual
  temprano (antes de construir el loop completo de `openai_agent.py`) que solo liste tools contra
  el servidor real, para descubrir fricción de protocolo cuanto antes.
- **Streaming SSE combinado con el loop de tool-calling**: el agente puede necesitar múltiples
  round-trips a OpenAI y al MCP server antes de tener una respuesta final que streamear.
  Mitigación: `openai_agent.py` primero resuelve el loop completo de forma no-streaming
  internamente; `chat.py` decide qué streamear al cliente (ej. eventos de "ejecutando tool X" +
  la respuesta final), sin acoplar el streaming al loop de tool-calling.
- **Mockear `mcp.Client` de forma realista**: si el mock no refleja bien la forma real del
  `inputSchema`/resultado, los tests pueden pasar en falso. Mitigación: el smoke test manual del
  primer riesgo también sirve para capturar la forma real de una respuesta y usarla como fixture
  de los mocks.

### Assumptions
- El endpoint `/chat` recibe el historial completo de la conversación en cada request (stateless
  en el servidor) — si M11 necesita otro contrato (ej. sesión con id), se ajusta como entrada de
  Changelog de este spec.
- No hay requisito de autenticación en `/chat` para esta entrega (heredado de "Fuera de alcance"
  de `plan-mcp.md`: sin Spring Security ni equivalente en Python).

## Milestones

- [ ] `mcp_client.py` lista las 14 tools reales del servidor de M6-M8 y las convierte a formato
      OpenAI (smoke test manual).
- [ ] `openai_agent.py` resuelve el loop completo con `mcp.Client` y `openai` mockeados —
      incluyendo el caso de múltiples tool calls encadenadas.
- [ ] `POST /chat` streamea una respuesta completa vía SSE.
- [ ] Suite de tests verde con cobertura ≥80%.

## Tasks

**Slicing strategy**: Horizontal (layered) — hay una cadena de dependencias clara (config → cliente
MCP → agente OpenAI → endpoint HTTP) donde cada capa necesita que la anterior exista; no hay
escenarios de negocio independientes que cortar verticalmente en un backend de un solo endpoint.

### Foundation (Build First)

- [ ] **Scaffolding Poetry**
  - **Acceptance**: `poetry install` corre limpio; `poetry run python -c "import agent"` no falla.
  - **Files**: `agent/pyproject.toml`, `agent/poetry.lock`, `agent/.env.example`.
  - **Tests**: ninguno — es infraestructura.
  - **Effort**: S

- [ ] **`config.py`**
  - **Acceptance**: lee `OPENAI_API_KEY` y `MCP_SERVER_URL` (u otro nombre equivalente) desde
    entorno; falla explícitamente si falta una variable requerida.
  - **Files**: `agent/agent/config.py`.
  - **Tests**: test unitario de carga con variables presentes/ausentes.
  - **Effort**: XS

- [ ] **`mcp_client.py`**
  - **Acceptance**: `list_tools()` devuelve las tools del servidor MCP en formato function-calling
    de OpenAI; método de ejecución de tool call funciona contra el servidor real en un smoke test
    manual documentado.
  - **Files**: `agent/agent/mcp_client.py`.
  - **Tests**: `test_mcp_client.py` — conversión de formato, mockeando `mcp.Client`.
  - **Effort**: M

- [ ] **`openai_agent.py`**
  - **Acceptance**: el loop maneja respuesta sin tool calls, una tool call, y múltiples tool calls
    encadenadas; nunca deja una excepción de dominio MCP sin traducir a un mensaje que el modelo
    pueda usar.
  - **Files**: `agent/agent/openai_agent.py`.
  - **Tests**: `test_openai_agent.py` — los tres casos del loop, mockeando `openai` y
    `mcp_client`.
  - **Effort**: L

- [ ] **`chat.py` + `schemas.py` + `main.py`**
  - **Acceptance**: `POST /chat` acepta mensaje + historial, streamea la respuesta por SSE; la API
    key de OpenAI nunca aparece en el body de respuesta.
  - **Files**: `agent/agent/{chat,schemas,main}.py`.
  - **Tests**: `test_chat_endpoint.py` — `TestClient` de FastAPI con el agente mockeado.
  - **Effort**: M

- [ ] **Verificación final del backend**
  - **Acceptance**: `poetry run pytest --cov-fail-under=80` verde; `POST /chat` reproduce
    manualmente el flujo de M9 contra el MCP server real.
  - **Files**: n/a (checklist de cierre).
  - **Tests**: n/a — corre la suite completa.
  - **Effort**: XS

## Effort Estimate

**Total Estimated Days**: 1.5–2 días (consistente con la tabla de estimados de `plan-mcp.md` — el
milestone más grande de la Parte 2, primera vez que se toca Python en este repo).

| Phase | Effort |
|-------|--------|
| Foundation (scaffolding + config) | ~0.2 día |
| Features (mcp_client + openai_agent) | ~0.9 día |
| Integration (chat/schemas/main + SSE) | ~0.4 día |
| Testing & Polish (cobertura, verificación final) | ~0.3 día |
