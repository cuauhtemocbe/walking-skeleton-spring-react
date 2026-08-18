# Implementation Plan: Gates end-to-end (M12)

**Spec**: `specs/truper-gates-end-to-end.md`
**Created**: 2026-08-18
**Status**: approved

## Components

### 1. Targets de Makefile para `agent/`
- **Purpose**: `agent-install`, `agent-run`, `test-agent`, `lint-agent`, autodocumentados.
- **Files**: `Makefile`.
- **Effort**: S

### 2. `validate` extendido
- **Purpose**: Incluir `test-agent` + `lint-agent` en el pipeline único ya existente.
- **Files**: `Makefile`.
- **Effort**: XS

### 3. `.gitignore` + `pre-commit` para Python
- **Purpose**: Ignorar artefactos de Poetry/pytest/ruff; auto-corregir lint/format de
  `agent/**/*.py` en staged, igual que ya pasa con Biome para `web/`.
- **Files**: `.gitignore`, `.githooks/pre-commit`.
- **Effort**: S

### 4. README final
- **Purpose**: Criterios de "skeleton camina" del dominio Truper + nota de entrega del examen
  (cómo correrlo, qué se logró) — no puede vivir en `CLAUDE.md` porque está gitignored.
- **Files**: `README.md`.
- **Effort**: M

## Dependencies

### Build Order
1. Targets de Makefile para `agent/` (componente 1) — depende de que M10 (`agent/`) ya exista con
   `pyproject.toml` y tests reales que invocar.
2. `validate` extendido (componente 2) — depende de 1.
3. `.gitignore` + `pre-commit` (componente 3) — independiente de 1/2, pero se verifica junto con
   ellos al final.
4. README final (componente 4) — depende de que M6-M11 estén cerrados, para documentar el estado
   real y no un plan a futuro.

### External Dependencies
`ruff`: se agrega a `agent/pyproject.toml` (dev dependency) si M10 no lo dejó ya declarado.

## Risks & Assumptions

### Risks
- **`make validate` desde clon limpio sin Poetry inicializado**: `agent-install` debe ser
  idempotente y ejecutarse antes de `test-agent`/`lint-agent` dentro de `validate`, no asumir que
  el entorno ya existe. Mitigación: `validate` depende explícitamente de `agent-install` como
  prerequisito de Makefile, mismo patrón que ya usa para `web` (`pnpm install` antes de test).
- **`.gitignore` incompleto la primera vez**: artefactos de Poetry/ruff pueden variar según
  versión. Mitigación: correr los comandos reales (`agent-install`, `test-agent`, `lint-agent`) en
  un clon limpio antes de dar el milestone por cerrado, y agregar lo que aparezca sin ignorar.

### Assumptions
- El umbral de `test-agent` (`--cov-fail-under=80`) ya quedó fijado como decisión en M10 — este
  milestone solo lo conecta al pipeline, no lo redefine.
- El README final reemplaza (no solo complementa) los 5 criterios originales de "el skeleton está
  terminado" que hablaban de `Nota`, dado que esa entidad ya no existe desde M6.

## Milestones

- [ ] `make help` lista `agent-install`/`agent-run`/`test-agent`/`lint-agent`, cada uno con su
      comentario `##`.
- [ ] `make validate` corre `test-agent`+`lint-agent` junto con los targets existentes.
- [ ] `.gitignore`/`pre-commit` cubren Python sin ensuciar `git status` después de correr los
      comandos reales.
- [ ] README con los criterios nuevos y la nota de entrega, revisado una última vez contra el
      enunciado del examen.

## Tasks

**Slicing strategy**: Horizontal (layered) — este milestone es puro trabajo de infraestructura
(Makefile → gitignore/hooks → documentación), sin escenarios de negocio que cortar verticalmente;
se ordena por lo que cada pieza necesita tener ya resuelto antes de la siguiente.

### Foundation (Build First)

- [ ] **Targets de Makefile para `agent/`**
  - **Acceptance**: `make agent-install`, `make agent-run`, `make test-agent`, `make lint-agent`
    funcionan desde un clon limpio; `make help` muestra los cuatro con su comentario `##`.
  - **Files**: `Makefile`.
  - **Tests**: n/a — se verifica corriendo los comandos.
  - **Effort**: S

- [ ] **`validate` extendido**
  - **Acceptance**: `make validate` corre `test-agent` + `lint-agent` además de los targets
    existentes de `api`/`web`; falla si cualquiera de los tres módulos falla.
  - **Files**: `Makefile`.
  - **Tests**: n/a — se verifica corriendo `make validate`.
  - **Effort**: XS

- [ ] **`.gitignore` + `pre-commit` para Python**
  - **Acceptance**: `git status` queda limpio después de correr `agent-install`/`test-agent`/
    `lint-agent` en un clon limpio; un archivo `.py` en `agent/` con problemas de lint se corrige
    automáticamente al hacer commit.
  - **Files**: `.gitignore`, `.githooks/pre-commit`.
  - **Tests**: n/a — se verifica con un commit de prueba.
  - **Effort**: S

- [ ] **README final**
  - **Acceptance**: sección de criterios de "skeleton camina" actualizada para el dominio Truper;
    nota breve de cómo correr el proyecto y qué se logró, presente y no en `CLAUDE.md`.
  - **Files**: `README.md`.
  - **Tests**: n/a.
  - **Effort**: M

- [ ] **Verificación final del examen completo**
  - **Acceptance**: `make validate` desde un clon limpio (sin Postgres corriendo de antes, sin
    entorno Python preparado) pasa en verde para `api`+`web`+`agent`.
  - **Files**: n/a (checklist de cierre del examen).
  - **Tests**: n/a — corre la suite completa de los tres módulos.
  - **Effort**: XS

## Effort Estimate

**Total Estimated Days**: 0.5 día (consistente con la tabla de estimados de `plan-mcp.md`).

| Phase | Effort |
|-------|--------|
| Foundation (targets de Makefile) | ~0.1 día |
| Features (validate extendido + gitignore/hooks) | ~0.15 día |
| Integration (verificación desde clon limpio) | ~0.1 día |
| Testing & Polish (README final) | ~0.15 día |
