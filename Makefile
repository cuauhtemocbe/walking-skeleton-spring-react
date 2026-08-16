.DEFAULT_GOAL := help

.PHONY: help up down api-run web-dev test-api test-web gen-api typecheck lint validate install-hooks e2e

help: ## Lista los targets disponibles con su descripción
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-16s\033[0m %s\n", $$1, $$2}'

up: ## Levanta Postgres en segundo plano (único servicio de compose)
	docker compose up -d

down: ## Apaga y elimina los contenedores de compose
	docker compose down

api-run: ## Corre el backend en el host (Spring Boot, ./gradlew bootRun)
	cd api && ./gradlew bootRun

web-dev: ## Corre el frontend en modo dev (pnpm dev)
	cd web && pnpm dev

test-api: up ## Corre los tests del backend (Testcontainers necesita Postgres)
	cd api && ./gradlew test

test-web: ## Corre los tests del frontend
	cd web && pnpm test

gen-api: ## Genera los tipos de TypeScript desde el esquema OpenAPI del backend
	cd web && pnpm gen:api

typecheck: ## Chequea tipos de TypeScript sin emitir (tsc --noEmit)
	@if [ -d web ]; then \
		cd web && pnpm typecheck; \
	else \
		echo "web/ no existe todavía (llega en M2) — omitiendo typecheck."; \
	fi

lint: ## Corre el linter (Biome) sobre el frontend
	cd web && pnpm lint

e2e: up ## Corre los tests end-to-end con Playwright
	cd web && pnpm e2e

validate: up ## Pipeline completo: typecheck + lint + tests + e2e. Gate del pre-push hacia main
	$(MAKE) typecheck
	$(MAKE) lint
	$(MAKE) test-api
	$(MAKE) test-web
	$(MAKE) e2e

install-hooks: ## Activa los git hooks versionados en .githooks/
	git config core.hooksPath .githooks
	chmod +x .githooks/*
