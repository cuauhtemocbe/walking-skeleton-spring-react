# web

Frontend del walking skeleton: React + TypeScript (`strict`) sobre Vite, formateado y linteado
con Biome. Ver el `CLAUDE.md` de la raíz del repo para el contexto completo del proyecto.

## Comandos

Desde la raíz del repo (`make web-dev`, `make typecheck`, `make lint`) o directamente en `web/`:

- `pnpm dev` — servidor de desarrollo con HMR.
- `pnpm build` — build de producción.
- `pnpm typecheck` — `tsc --noEmit` sobre `tsconfig.json` y `tsconfig.test.json`.
- `pnpm lint` — `biome check .`.
- `pnpm format` — `biome format --write .`.
