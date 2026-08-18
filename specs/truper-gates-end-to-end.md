---
title: Gates end-to-end (M12)
status: approved
created: 2026-08-18
updated: 2026-08-18
issue: "#34"
---

# Gates end-to-end (M12)

## Objective

Cerrar el examen completo integrando `agent/` (M10) al pipeline único del repo (`make validate`),
extendiendo los git hooks y `.gitignore` a Python, y dejando el README con la nota de entrega que
pide el examen — de forma que `make validate` desde un clon limpio verifique `api`+`web`+`agent`
en un solo comando, igual que ya hace hoy para `api`+`web`.

## Context

M6-M11 dejaron el dominio Truper completo (MCP Server + chat propio) funcionando, pero
`make validate` todavía no conoce `agent/`. Este repo trata `make validate` (gate del hook
`pre-push` hacia `main`) como la única interfaz de verificación — extenderlo es lo que cierra el
examen de forma consistente con el resto del repo, no un paso opcional. Ver `plan-mcp.md` (raíz
del repo) para el plan completo M6-M12; este spec cubre únicamente M12, el último milestone.

Decisiones ya tomadas y heredadas de `plan-mcp.md`/`CLAUDE.md` (no se re-discuten acá):
- El `CLAUDE.md` del repo está gitignored — no viaja en el código entregado, así que la nota breve
  de "cómo correrlo y qué se logró" que pide la entrega del examen tiene que vivir en el
  `README.md`, no en `CLAUDE.md`.
- Sin `Dockerfile` de producción, sin GitHub Actions — el gate sigue siendo `make validate` +
  `pre-push`, consistente con las decisiones deliberadas ya documentadas para `api`/`web`.

## Requirements

### Functional Requirements

- [ ] Nuevos targets de `Makefile`: `agent-install` (`poetry install`), `agent-run`
      (`poetry run uvicorn agent.main:app --reload --port 8000`), `test-agent` (`pytest` con
      `--cov-fail-under=80`), `lint-agent` (`ruff check` + `ruff format --check`) — cada uno con
      su comentario `##` autodocumentado, igual que el resto de `make help`.
- [ ] `validate` pasa a incluir `test-agent` + `lint-agent` junto con los targets de `api`/`web`
      que ya corre.
- [ ] `.gitignore` agrega sección Python: `agent/.venv/`, `**/__pycache__/`,
      `agent/.pytest_cache/`, y cualquier otro artefacto de build de Poetry/pytest/ruff que
      aparezca al correr los comandos reales (`.env` ya cubre `agent/.env`, no se duplica).
- [ ] `pre-commit` (`.githooks/pre-commit`) extiende el bloque existente de Biome con uno análogo
      para `agent/**/*.py`: `ruff check --fix` + `ruff format` sobre archivos staged.
- [ ] `README.md` actualizado con los nuevos "criterios de que el skeleton camina" para el dominio
      Truper (reemplazando o complementando los 5 criterios originales de `Nota`, que ya no
      aplican al dominio actual) y la nota breve de cómo correrlo y qué se logró, que pide la
      entrega del examen.

### Non-Functional Requirements

- `make validate` debe correr en verde desde un clon limpio, sin Postgres ni el entorno Python
  preparados de antes — igual que ya se exige para `api`/`web` desde M5.
- No se relaja ningún umbral existente (JaCoCo ≥90%, Vitest 80/80/80/80) para lograr que `agent/`
  encaje — `test-agent` usa su propio umbral (≥80%, consistente con lo ya fijado en M10).

## Architecture

### Components

Cambios de infraestructura, no de dominio:
```
Makefile                # nuevos targets agent-install/agent-run/test-agent/lint-agent
.gitignore               # sección Python
.githooks/pre-commit      # bloque ruff análogo al de Biome
README.md                  # criterios de "skeleton camina" + nota de entrega del examen
```

### Data Model

N/A.

### External Dependencies

`ruff` (lint + format de Python) — se agrega como dependencia de desarrollo en
`agent/pyproject.toml` si no quedó ya declarada en M10.

## User Stories

- Como examinador, quiero correr `make validate` una sola vez desde un clon limpio y ver
  `api`+`web`+`agent` verificados en verde, para no tener que ejecutar tres pipelines por separado.
- Como colaborador, quiero que un commit que toca `agent/**/*.py` con problemas de lint se corrija
  automáticamente antes de completarse, igual que ya pasa hoy con el código de `web/`.
- Como examinador, quiero encontrar en el README una nota breve de cómo correr el proyecto y qué
  se logró, sin tener que inferirlo del historial de commits.

## Testing Strategy

### Unit Tests
N/A — este milestone no agrega lógica de dominio, solo integra el pipeline ya existente de M10
(`test-agent` invoca los tests que M10 ya escribió).

### Integration Tests
`make validate` en sí mismo es la prueba de integración de este milestone: corre `api`+`web`+
`agent` end-to-end.

### E2E Tests
Ninguno nuevo — reutiliza la verificación manual ya hecha en M9/M11.

## Boundaries & Constraints

### In Scope
Targets de Makefile para `agent/`, extensión de `.gitignore` y `pre-commit`, actualización final
del README (criterios + nota de entrega).

### Out of Scope
Cualquier cambio de código de dominio en `api/`, `web/` o `agent/` (M6-M11 ya cerrados), CI
hosteado, Dockerfile de producción — decisiones deliberadas ya documentadas en `CLAUDE.md`.

### Technical Constraints
`ruff` como única herramienta de lint/format para Python (consistente con "un Makefile
autodocumentado como única interfaz de comandos" de `CLAUDE.md`).

## Success Criteria

- [ ] `make validate` desde un clon limpio (sin Postgres corriendo de antes, sin entorno Python
      preparado) corre `api`+`web`+`agent` (typecheck/lint/tests/cobertura) en verde.
- [ ] Ningún target nuevo de `make help` queda sin su comentario `##`.
- [ ] README actualizado con los criterios de "skeleton camina" del dominio Truper y la nota de
      entrega del examen.

## Implementation Plan

Ver `specs/truper-gates-end-to-end-plan.md`.

## Changelog

<!-- Vacío: este spec no ha llegado a `completed` todavía. -->
