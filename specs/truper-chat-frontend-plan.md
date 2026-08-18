# Implementation Plan: Frontend de chat en React (M11)

**Spec**: `specs/truper-chat-frontend.md`
**Created**: 2026-08-18
**Status**: approved

## Components

### 1. Retiro de la UI y flujo REST de `Nota`
- **Purpose**: Liberar el frontend del dominio desechable, incluyendo el flujo `gen:api` que ya
  no tiene sujeto (Truper no expone REST).
- **Files**: elimina `FormularioNota`, `ListaNotas`, `useNotas` (y sus tests co-locados),
  `web/src/api/*` (`cliente.ts`, `schema.d.ts`, tests); retira script `gen:api` de
  `package.json`.
- **Effort**: S

### 2. `useChatStream`
- **Purpose**: Consumir `POST /chat` con `fetch` + `ReadableStream`, parseando eventos SSE a mano
  (mensaje incremental, tool-call en curso, fin de stream, error).
- **Files**: `web/src/chat/useChatStream.ts` + `useChatStream.test.ts`.
- **Effort**: M

### 3. Componentes de UI de chat
- **Purpose**: Lista de mensajes + campo de envío, con indicación visual de tool-calls en curso.
- **Files**: `web/src/chat/{ListaMensajes,CampoChat}.tsx` + tests co-locados.
- **Effort**: M

### 4. Proxy de Vite
- **Purpose**: Enrutar `/chat` hacia `http://localhost:8000` (backend `agent/`), mismo patrón que
  el proxy `/api` existente.
- **Files**: `web/vite.config.ts`.
- **Effort**: XS

## Dependencies

### Build Order
1. Retiro de `Nota` (componente 1) — libera el espacio, sin dependencias previas.
2. `useChatStream` (componente 2) — depende del proxy (componente 4) para probarse de punta a
   punta, pero puede desarrollarse con `fetch` mockeado antes de que el proxy exista.
3. Componentes de UI (componente 3) — depende de `useChatStream` para consumir su estado.
4. Proxy de Vite (componente 4) — independiente, pero necesario antes de la verificación manual
   final.

### External Dependencies
Ninguna nueva.

## Risks & Assumptions

### Risks
- **Parseo manual de SSE con `fetch`/`ReadableStream`**: no hay librería que lo resuelva (se
  descartó `EventSource` porque no soporta `POST`). Mitigación: `useChatStream.test.ts` cubre
  explícitamente streams fragmentados a mitad de un evento SSE (un chunk de red puede cortar un
  evento en dos), no solo el caso feliz de un evento completo por chunk.
- **Umbral de cobertura 80/80/80/80 sobre código de streaming**: las ramas de error de red y de
  parseo parcial son fáciles de dejar sin cubrir. Mitigación: escribir esos casos primero (TDD),
  no como añadido al final.

### Assumptions
- El formato exacto de los eventos SSE que emite `agent/chat.py` (M10) ya está definido por ese
  milestone — si al integrar aparece una discrepancia, se ajusta el parser de `useChatStream` para
  matchear el contrato real de M10, documentando el ajuste como entrada de Changelog si M10 ya
  está `completed` para entonces.

## Milestones

- [ ] `Nota` retirada del frontend — `pnpm test` sigue verde sin ella, `grep -ri nota web/src`
      limpio.
- [ ] `useChatStream` parsea correctamente eventos SSE completos y fragmentados (test unitario).
- [ ] UI de chat completa, verificada manualmente contra `agent/` (M10) real.
- [ ] `pnpm test:coverage` verde con umbral 80/80/80/80 sobre los componentes de chat.

## Tasks

**Slicing strategy**: Horizontal (layered) — hay una dependencia clara entre el hook de streaming
y los componentes que lo consumen; no hay escenarios de negocio independientes que cortar
verticalmente en una UI de un solo flujo de chat.

### Foundation (Build First)

- [ ] **Retirar UI y flujo REST de `Nota`**
  - **Acceptance**: `FormularioNota`/`ListaNotas`/`useNotas`/`web/src/api/*` no existen; script
    `gen:api` retirado de `package.json`; `pnpm test` pasa sin esos archivos.
  - **Files**: elimina los archivos listados arriba.
  - **Tests**: ninguno nuevo — se eliminan los tests co-locados junto con los componentes.
  - **Effort**: S

- [ ] **`useChatStream`**
  - **Acceptance**: parsea eventos SSE completos y fragmentados entre chunks; expone mensajes
    incrementales, estado de tool-call en curso, y error de red/stream.
  - **Files**: `web/src/chat/useChatStream.ts`.
  - **Tests**: `useChatStream.test.ts` — mockeando `fetch`, casos: stream completo, fragmentado,
    evento de tool-call, error.
  - **Effort**: M

- [ ] **Componentes de UI de chat**
  - **Acceptance**: `ListaMensajes` renderiza historial usuario/agente; `CampoChat` envía mensaje
    y se deshabilita mientras hay respuesta en curso; indicación visual de tool-call ("consultando
    productos…") visible durante ese estado.
  - **Files**: `web/src/chat/{ListaMensajes,CampoChat}.tsx`.
  - **Tests**: co-locados — render, envío de mensaje, estado deshabilitado, indicador de tool-call.
  - **Effort**: M

- [ ] **Proxy de Vite**
  - **Acceptance**: request a `/chat` desde el dev server llega a `http://localhost:8000`.
  - **Files**: `web/vite.config.ts`.
  - **Tests**: ninguno automatizado — verificación manual junto con la demo final.
  - **Effort**: XS

- [ ] **Verificación final del slice**
  - **Acceptance**: demo de M9 reproducida desde la UI propia contra `agent/` real;
    `pnpm test:coverage` verde con umbral 80/80/80/80 sobre los componentes de chat;
    `grep -ri nota web/src` no devuelve nada.
  - **Files**: n/a (checklist de cierre).
  - **Tests**: n/a — corre la suite completa.
  - **Effort**: XS

## Effort Estimate

**Total Estimated Days**: 1–1.5 días (consistente con la tabla de estimados de `plan-mcp.md`).

| Phase | Effort |
|-------|--------|
| Foundation (retiro de Nota) | ~0.15 día |
| Features (useChatStream) | ~0.5 día |
| Integration (componentes de UI + proxy) | ~0.5 día |
| Testing & Polish (cobertura, verificación final) | ~0.2 día |
