# Watermelon CI

Корпоративная CI/CD-платформа: манифесты (YAML или **Карбыз**), Vault-секреты, GitOps без Helm, fleet-консоль.

## Быстрый старт

```bash
# UI → static
cd console-ui && npm install && npm run build
cp -R dist/. ../platform-app/src/main/resources/static/
cp -R public/docs-static ../platform-app/src/main/resources/static/

# Backend
cd ..
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home   # macOS arm, при необходимости
mvn -q -pl platform-app -am package -DskipTests
java -jar platform-app/target/platform-app-0.1.0-SNAPSHOT.jar
```

Открой [http://localhost:8088](http://localhost:8088) → **Демо**.

Либо полный стек:

```bash
docker compose up -d --build watermelon-ci postgres vault
```

Консоль: `:8088`. Демо создаёт org `watermelon-demo`, проект `storefront`. Jenkins/Vault не обязательны (`soft-fail`, `memory-fallback`).

## Консоль

| Раздел | URL | Назначение |
|--------|-----|------------|
| Storefront | `/app/studio` | гайд git→build→registry→secrets→deploy; YAML и Карбыз |
| Карбыз IDE | `/app/ide` | редактор `.kbz`, Run / IR / Compile → Jenkinsfile |
| Спецификация | `/app/docs` | нормативный текст языка (1.3) |
| Проекты / Флот | `/app/projects`, `/app/fleet` | пайплайны, секреты, GitOps, контейнеры |

## Карбыз

Татарский синтаксис того же манифеста Watermelon CI, что YAML. Исходники и спека: [`karbyz/`](karbyz/).

```bash
python3 karbyz/run.py test
python3 karbyz/run.py karbyz/examples/hello.kbz
python3 karbyz/run.py karbyz/examples/devops_demo.kbz --as-yaml
```

API рантайма IDE: `POST /api/v1/karbyz/run`, `POST /api/v1/karbyz/compile`.

## Стек

Java 21 · Spring Boot 3.5 · React (Vite) · H2/Postgres · Flyway · OpenAPI · Python 3 (транслятор Карбыз в образе)

## Compose

```bash
docker compose up -d postgres vault registry
# опционально: jenkins forgejo nexus
# полный профиль: docker compose --profile full up -d
```

Образ `watermelon-ci` включает JRE + `python3` + `/opt/karbyz`.

## Конфиг демо

```yaml
watermelon:
  demo:
    enabled: true
    seed-on-startup: true
  karbyz:
    home: karbyz          # в Docker: /opt/karbyz
    python: python3
  jenkins.soft-fail: true
  secrets.memory-fallback: true
  gitops.dry-run: true
```

Архитектура: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).
