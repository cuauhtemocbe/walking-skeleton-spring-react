---
title: Frontend de chat en React (M11)
status: approved
created: 2026-08-18
updated: 2026-08-18
issue: "#33"
---

# Frontend de chat en React (M11)

## Objective

Reemplazar la UI de `Nota` por una interfaz de chat que consuma `POST /chat` del backend `agent/`
(M10), reproduciendo desde el navegador la misma demo de venta ya verificada manualmente en M9 —
cerrando así la Parte 2 del examen con una app de chat propia de punta a punta.

## Context

M10 dejó `POST /chat` funcionando contra el MCP server real, con streaming SSE. El dominio Truper
se expone solo vía MCP (no REST), así que el flujo `pnpm gen:api`/`schema.d.ts` que existía
únicamente para tipar el CRUD REST de `Nota` queda sin sujeto y se retira junto con la UI vieja.
Ver `plan-mcp.md` (raíz del repo) para el plan completo M6-M12; este spec cubre únicamente M11.

Decisiones ya tomadas y heredadas de `plan-mcp.md` (no se re-discuten acá):
- `EventSource` no sirve para consumir el streaming de `/chat` porque el request es `POST` (con
  body) — se usa `fetch` + `ReadableStream` parseando eventos SSE a mano.
- El umbral de cobertura del repo (Vitest 80/80/80/80 statements/branches/functions/lines) no se
  relaja porque `Nota` se retira — aplica igual sobre los componentes nuevos de chat.

## Requirements

### Functional Requirements

- [ ] Retirar `FormularioNota`, `ListaNotas`, `useNotas`, y todo `web/src/api/*`
      (`cliente.ts`, `schema.d.ts`, sus tests) — el dominio Truper no tiene REST que tipar.
- [ ] Retirar el script `gen:api` de `package.json` y su referencia en `Makefile`/`vite.config.ts`
      si existiera.
- [ ] `useChatStream`: hook que hace `fetch` con `POST` a `/chat`, parsea el `ReadableStream` de
      la respuesta como eventos SSE, y expone el estado incremental de la conversación (mensajes,
      indicador de "cargando"/"ejecutando tool").
- [ ] Componente de lista de mensajes: renderiza el historial de la conversación (usuario/agente).
- [ ] Componente de campo de chat: input + envío de mensaje nuevo, deshabilitado mientras hay una
      respuesta en curso.
- [ ] Indicación visual de llamadas a tools en curso (ej. "consultando productos…") — mapea los
      eventos SSE de tool-call a un mensaje de estado legible, no solo el texto final del agente.
- [ ] Proxy `/chat` → `http://localhost:8000` en `vite.config.ts`, mismo patrón que el proxy `/api`
      que ya existía para el backend Java.

### Non-Functional Requirements

- Cobertura: `pnpm test:coverage` con umbral 80/80/80/80 (statements/branches/functions/lines)
  sobre los componentes nuevos de chat — el umbral no negociable del repo no se relaja.
- Accesibilidad: campo de chat y lista de mensajes usables con teclado, mensajes de estado
  perceptibles por lectores de pantalla (no solo color/animación).

## Architecture

### Components

```
web/src/
  chat/
    useChatStream.ts        # fetch + ReadableStream parseando SSE
    ListaMensajes.tsx
    CampoChat.tsx
    (tests co-locados: useChatStream.test.ts, ListaMensajes.test.tsx, CampoChat.test.tsx)
```

Retirados: `web/src/notas/` (o ubicación equivalente de `FormularioNota`/`ListaNotas`/`useNotas`),
`web/src/api/*`.

### Data Model

Sin modelo de datos propio del frontend — el contrato lo define `POST /chat` de `agent/` (M10),
consumido como JSON/SSE sin tipos generados (a diferencia del `Nota` REST original, aquí no hay
`pnpm gen:api` porque no hay OpenAPI que tipar).

### External Dependencies

Ninguna nueva — usa `fetch`/`ReadableStream` nativos del navegador, sin librería de SSE adicional.

## User Stories

- Como usuario, quiero escribir un mensaje en el chat y ver la respuesta del agente aparecer
  incrementalmente, para tener una experiencia de conversación fluida.
- Como usuario, quiero ver una indicación de que el agente está "consultando productos" o
  ejecutando otra acción, para entender qué está pasando durante una respuesta que tarda.
- Como examinador, quiero reproducir desde la UI la misma demo de venta de M9, para verificar que
  la Parte 2 completa (frontend + backend + MCP) funciona de punta a punta.

## Testing Strategy

### Unit Tests
`useChatStream.test.ts`: parseo de eventos SSE (mensaje incremental, evento de tool-call, fin de
stream, error de red), mockeando `fetch`.
`ListaMensajes.test.tsx` / `CampoChat.test.tsx`: render y comportamiento de interacción (enviar
mensaje, deshabilitado mientras carga), co-locados con su componente.

### Integration Tests
Ninguno automatizado más allá de Vitest de componente — la integración real con `agent/` (M10) se
verifica manualmente.

### E2E Tests
Ninguno automatizado, a propósito (heredado de `plan-mcp.md`). La demo end-to-end de M9 se
reproduce manualmente desde la UI como criterio de cierre de este milestone.

## Boundaries & Constraints

### In Scope
UI de chat completa (hook + componentes), retiro completo de la UI y el flujo `gen:api` de
`Nota`, proxy de `/chat` en Vite.

### Out of Scope
Cualquier cambio en `agent/` (M10 ya cerrado), autenticación en el frontend, persistencia de
historial de conversación en el navegador (recarga de página pierde el chat, consistente con
"fuera de alcance" de `plan-mcp.md`: sin caché/estado persistente para el dominio Truper).

### Technical Constraints
Node 22+, pnpm, React 19, TypeScript strict, Biome. Vitest con umbral 80/80/80/80 no negociable.

## Success Criteria

- [ ] La demo end-to-end de M9 ("Crea un pedido: agrégame tres martillos y cuatro serruchos") se
      reproduce desde la UI propia, contra `agent/` (M10) y el MCP server real.
- [ ] `pnpm test:coverage` pasa el umbral 80/80/80/80 sobre los componentes nuevos de chat.
- [ ] `grep -ri nota web/src` no devuelve nada.

## Implementation Plan

Ver `specs/truper-chat-frontend-plan.md`.

## Changelog

<!-- Vacío: este spec no ha llegado a `completed` todavía. -->
