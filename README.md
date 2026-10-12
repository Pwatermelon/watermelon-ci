# Watermelon CI

Корпоративная CI/CD-платформа: манифесты (YAML или **Карбыз** 1.4), Vault-секреты, GitOps, fleet-хосты, Keycloak.

## Быстрый старт

```bash
# UI → static
cd console-ui && npm install && npm run build
cp -R dist/. ../platform-app/src/main/resources/static/
mkdir -p ../platform-app/src/main/resources/static/docs-static/karbyz
cp -R public/docs-static/karbyz/. ../platform-app/src/main/resources/static/docs-static/karbyz/
# синк нормативной спеки из исходников
cp ../karbyz/docs/SPECIFICATION.md ../karbyz/docs/KEYWORDS.md public/docs-static/karbyz/
cp ../karbyz/docs/SPECIFICATION.md ../karbyz/docs/KEYWORDS.md ../platform-app/src/main/resources/static/docs-static/karbyz/
cp ../karbyz/docs/karbyz.ebnf public/docs-static/karbyz/ 2>/dev/null || cp ../karbyz/*.ebnf public/docs-static/karbyz/ 2>/dev/null || true

# Backend (auth выключен по умолчанию)
cd ..
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home   # macOS arm, при необходимости
mvn -q -pl platform-app -am package -DskipTests
java -jar platform-app/target/platform-app-0.1.0-SNAPSHOT.jar
```

Открой [http://localhost:8088](http://localhost:8088) → **Открыть демо**.

## Compose (Keycloak + консоль)

```bash
docker compose up -d --build postgres vault keycloak watermelon-ci
```

| Сервис | URL |
|--------|-----|
| Консоль | http://localhost:8088 |
| Keycloak | http://localhost:8083 (admin/admin) |
| Realm / client | `watermelon` / `watermelon-console` |
| Тестовый user | `demo` / `demo` |

В compose `WATERMELON_AUTH_ENABLED=true`: API `/api/**` требует JWT. SPA: authorization code + PKCE → `/auth/callback`.

Локальный jar без Keycloak: `watermelon.auth.enabled=false` (дефолт) — вход через «Открыть демо».

## Консоль

Верхняя навигация (без левого сайдбара):

| Раздел | URL |
|--------|-----|
| Обзор | `/app` |
| Репозитории | `/app/repos`, `/app/repos/:owner/:repo` |
| Пайплайны | `/app/pipelines` (runs · манифест · песочница) |
| Хосты | `/app/hosts` |
| Сервисы | `/app/services` |
| Документация | `/app/docs` (Карбыз SPEC 1.4) |

Демо создаёт org `watermelon-demo`, проект/репо `storefront`.

## Карбыз 1.4

Татарский синтаксис манифеста Watermelon CI. Тип **`логик`** (`дорес`/`ялган`). Исходники: [`karbyz/`](karbyz/).

```bash
python3 karbyz/run.py test
python3 karbyz/run.py karbyz/examples/hello.kbz
python3 karbyz/run.py karbyz/examples/devops_demo.kbz --as-yaml
```

## Стек

Java 21 · Spring Boot 3.5 · React (Vite) · H2/Postgres · Flyway · Keycloak · OpenAPI · Python 3 (Карбыз)

## Конфиг

```yaml
watermelon:
  auth.enabled: false          # true + KEYCLOAK_ISSUER_URI / KEYCLOAK_JWK_SET_URI
  demo.seed-on-startup: true
  git.provider: local          # или forgejo
  jenkins.soft-fail: true
  secrets.memory-fallback: true
  gitops.dry-run: true
```

Архитектура: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).
