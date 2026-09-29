# Watermelon CI

Продаваемый MVP корпоративной CI/CD-платформы: манифесты как в GitLab CI, Vault-секреты, деплой через ArgoCD без Helm, fleet-консоль и красивый product UI.

## За 60 секунд

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
mvn -q -pl platform-app -am package -DskipTests
java -jar platform-app/target/platform-app-0.1.0-SNAPSHOT.jar
```

Открой [http://localhost:8088](http://localhost:8088) → **Launch demo**.

Демо сразу поднимает org `watermelon-demo`, проект `storefront`, pipeline, secrets, fleet cluster. Jenkins/Vault не обязательны (soft-fail + memory fallback).

## Что умеет MVP

| Возможность | Как попробовать в UI |
|-------------|----------------------|
| YAML или Карбыз → Jenkinsfile | Manifest studio / Project → Pipelines |
| Запуск pipeline | Project → Run pipeline |
| Secrets (Vault) | Project → Secrets |
| GitOps без Helm | Project → GitOps → Deploy via ArgoCD |
| Issues | Project → Issues |
| Artifact catalog | Project → Artifacts |
| Fleet clusters | Fleet |

## Стек

Java 21 · Spring Boot 3.5 · React (Vite) · H2/Postgres · Flyway · OpenAPI

Модули: `platform-common` … `platform-app` + `console-ui` + `platform-secrets` + `platform-gitops` + `platform-fleet`.

## Dev UI

```bash
cd console-ui && npm install && npm run build
cp -R dist/. ../platform-app/src/main/resources/static/
```

Или `npm run dev` (proxy на `:8088`).

## Язык Карбыз

Манифест Watermelon CI на татарском синтаксисе (эквивалент YAML). Всё в [`karbyz/`](karbyz/). В UI — `/docs/karbyz` и вкладка Карбыз в студии манифестов.

```bash
python3 karbyz/run.py test
python3 karbyz/run.py examples/all_constructs.kbz
python3 karbyz/run.py examples/devops_demo.kbz --as-yaml
```

## Compose (полная инфра)

```bash
docker compose up -d postgres vault registry
# опционально: jenkins forgejo nexus
```

## Конфиг демо

```yaml
watermelon:
  demo:
    enabled: true
    seed-on-startup: true
  jenkins.soft-fail: true
  secrets.memory-fallback: true
  gitops.dry-run: true
```
