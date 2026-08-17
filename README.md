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
organizado en milestones **M0 → M5**. No se avanza de hito sin que el anterior pase su
criterio de aceptación: ese es el punto entero del skeleton — cuando algo falla, que haya
una sola cosa nueva que pueda ser la causa.

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

Pendiente: se documenta la secuencia exacta de verificación desde un clon limpio al cerrar
M5. Hasta entonces, `make help` lista los targets disponibles. Corré `make install-hooks`
una vez por clon para activar los git hooks versionados en `.githooks/`.

### Tests de integración del backend

`./gradlew test` levanta su propio Postgres real vía Testcontainers para `NotaIT` — no hace
falta `docker compose up` antes. Para desarrollo local, reusar el contenedor entre corridas
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
