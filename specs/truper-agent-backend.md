---
title: Backend agent — FastAPI + OpenAI + cliente MCP (M10)
status: approved
created: 2026-08-18
updated: 2026-08-18
issue: "#32"
---

# Backend `agent` — FastAPI + OpenAI + cliente MCP (M10)

## Objective

Arrancar la Parte 2 del examen: un backend Python (`agent/`, FastAPI + Poetry) que expone
`POST /chat`, usa un agente OpenAI con tool-calling, y consume el mismo MCP Server de Truper
(M6-M8) como cliente MCP — reproduciendo por HTTP el mismo flujo de venta ya verificado
manualmente en M9, pero ahora vía API propia y con la API key de OpenAI solo en el servidor.

## Context

M6-M9 cerraron la Parte 1 (MCP Server) y demostraron el flujo de venta con Claude Code como
cliente MCP. El examen pide una segunda parte: una app de chat propia donde el "cerebro"
conversacional es un agente OpenAI (no Claude Code) hablando con el mismo MCP Server. M10 es el
backend de ese chat; M11 (frontend) consume `POST /chat` una vez este milestone cierre. Ver
`plan-mcp.md` (raíz del repo) para el plan completo M6-M12; este spec cubre únicamente M10.

Decisiones ya tomadas y heredadas de `plan-mcp.md` (no se re-discuten acá):
- Paquetes verificados: `mcp` 2.0.0 (`mcp.Client`), `openai` 3.3.0, `fastapi` 0.141.1,
  `sse-starlette` 3.4.8. El `inputSchema` de una tool MCP ya es JSON Schema — se mapea directo a
  formato function-calling de OpenAI sin conversor especial.
- Poetry ya está instalado en el host — sin `Dockerfile` (corre en host, mismo criterio que
  `api`/`web`: prioridad a velocidad de iteración local).
- Una sesión `mcp.Client` por request de chat, no una conexión global compartida — evita estado
  cruzado entre conversaciones concurrentes.

## Requirements

### Functional Requirements

- [ ] Scaffolding Poetry en `agent/`: `pyproject.toml` (fastapi 0.141.1, uvicorn, openai 3.3.0,
      mcp 2.0.0, sse-starlette 3.4.8), `poetry.lock` commiteado, `.env.example`.
- [ ] `agent/config.py`: carga configuración desde variables de entorno (API key de OpenAI, URL
      del MCP server), nunca hardcodeada.
- [ ] `agent/mcp_client.py`: envuelve `mcp.Client`, expone `list_tools()` convertido a formato
      function-calling de OpenAI, y un método para ejecutar una tool call contra el MCP server.
- [ ] `agent/openai_agent.py`: implementa el loop manual de tool-calling — Chat Completions →
      inspeccionar `tool_calls` → ejecutar cada uno contra `mcp_client` → agregar mensaje
      `role="tool"` con el resultado → repetir hasta que el modelo responda sin más tool calls.
- [ ] `agent/chat.py` + `agent/schemas.py`: define el contrato de `POST /chat` (mensaje de
      usuario, historial de conversación) y la respuesta.
- [ ] `agent/main.py`: app FastAPI, expone `POST /chat` streameando la respuesta por SSE
      (`sse-starlette`).
- [ ] Una sesión `mcp.Client` por request de `/chat` (no una instancia global compartida entre
      requests).
- [ ] API key de OpenAI leída solo de variable de entorno — nunca aparece en el request/response
      hacia el cliente HTTP.

### Non-Functional Requirements

- Cobertura: pytest con `--cov-fail-under=80` (consistente con el umbral que M12 fija en el
  Makefile), sobre `agent/` excluyendo `main.py` (bootstrap) si aplica el mismo criterio que
  `main.tsx` en el frontend.
- Seguridad: la API key de OpenAI no debe aparecer en logs de request/response ni en el body
  devuelto al cliente.

## Architecture

### Components

```
agent/
  pyproject.toml
  poetry.lock
  .env.example
  agent/
    main.py          # app FastAPI, monta POST /chat
    config.py         # variables de entorno (API key, URL del MCP server)
    mcp_client.py      # wrapper de mcp.Client, list_tools() → formato OpenAI
    openai_agent.py    # loop manual de tool-calling
    chat.py            # contrato de POST /chat, streaming SSE
    schemas.py          # modelos Pydantic de request/response
  tests/
    conftest.py
    test_mcp_client.py
    test_openai_agent.py
    test_chat_endpoint.py
```

### Data Model

Sin persistencia propia — `agent/` es stateless entre requests; el estado de la conversación
(historial de mensajes) lo mantiene el cliente (M11) y lo reenvía en cada request a `/chat`.

### External Dependencies

- `openai` 3.3.0: cliente de Chat Completions con tool-calling.
- `mcp` 2.0.0 (`mcp.Client`): cliente MCP contra el servidor Truper de M6-M8.
- `fastapi` 0.141.1 + `sse-starlette` 3.4.8: servidor HTTP y streaming SSE de `/chat`.

## User Stories

- Como usuario del chat, quiero mandar un mensaje a `POST /chat` y recibir una respuesta que
  refleje el resultado de las tools MCP ejecutadas (ej. un pedido creado con su total), para no
  tener que usar Claude Code directamente.
- Como responsable de seguridad del examen, quiero que la API key de OpenAI nunca viaje hacia el
  navegador, para que la demo no filtre credenciales.

## Testing Strategy

### Unit Tests
`test_mcp_client.py`: conversión de `list_tools()` a formato OpenAI, mockeando `mcp.Client`.
`test_openai_agent.py`: el loop de tool-calling, mockeando tanto `openai` como `mcp.Client` —
casos: respuesta sin tool calls, una tool call, múltiples tool calls encadenadas.
`test_chat_endpoint.py`: `POST /chat` con `TestClient` de FastAPI, mockeando el agente completo.

### Integration Tests
`POST /chat` reproduce vía HTTP el mismo flujo de M9 (crear cliente si no existe → crear pedido →
total correcto) contra el MCP server real de M6-M8 corriendo en local — al menos una corrida
manual/documentada, ya que un test de integración automatizado exigiría mockear o gastar una
llamada real a OpenAI (fuera de alcance para CI, dado que no hay CI hosteado en este repo).

### E2E Tests
Ninguno automatizado — la demo end-to-end completa (frontend incluido) queda para M11.

## Boundaries & Constraints

### In Scope
Backend `agent/` completo: scaffolding, cliente MCP, agente OpenAI, endpoint `/chat` con SSE,
tests unitarios mockeados.

### Out of Scope
Frontend de chat (M11), Dockerfile de producción para `agent/` (decisión ya tomada: corre en
host), persistencia de historial de conversación (el cliente lo reenvía en cada request), gates de
Makefile para `agent/` (`agent-install`/`test-agent`/`lint-agent` son de M12).

### Technical Constraints
Python + Poetry (ya instalado en el host). API key de OpenAI solo por variable de entorno. Sin
`Dockerfile` de producción, consistente con la fila ya documentada en el README del repo.

## Success Criteria

- [ ] `POST /chat` reproduce vía HTTP el mismo flujo de M9 contra el MCP server real.
- [ ] Tests unitarios mockeando `openai` y `mcp.Client` pasan, con cobertura ≥80%.
- [ ] La API key de OpenAI nunca aparece en el body de request/response de `/chat`.

## Implementation Plan

Ver `specs/truper-agent-backend-plan.md`.

## Changelog

<!-- Vacío: este spec no ha llegado a `completed` todavía. -->
