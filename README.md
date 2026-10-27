# Watermelon CI

Корпоративная CI/CD-платформа: манифесты (YAML / **Карбыз**), registry, fleet Docker-VM, Keycloak.

## Полное демо (Docker VM + пайплайн)

Поднимает DinD-«виртуалки» `demo-vm-a` / `demo-vm-b`, registry, Jenkins, консоль. Seed создаёт репозитории `storefront` и `payments`, регистрирует VM как хосты и раскатывает контейнеры.

```bash
# 1) UI
cd console-ui && npm ci && npm run build
cp -R dist/. ../platform-app/src/main/resources/static/

# 2) Стек (нужен запущенный Docker Desktop)
cd ..
chmod +x scripts/demo-up.sh
./scripts/demo-up.sh
```

Или вручную:

```bash
docker compose up -d --build
curl -X POST http://localhost:8088/api/v1/demo/seed
```

| Что | URL |
|-----|-----|
| Консоль | http://localhost:8088 → **Открыть демо** |
| Хосты | `/app/hosts` — demo-vm-a / demo-vm-b |
| Сервисы | `/app/services` — контейнеры на VM |
| Registry | `/app/registry` — образы/теги |
| Пайплайны | `/app/pipelines` — YAML/Карбыз build→push→deploy |
| Jenkins | http://localhost:8080 (admin/admin) |
| Registry API | http://localhost:5001/v2/_catalog |

### Сценарий

1. **Репозитории** — `watermelon-demo/storefront`, `payments` (Dockerfile + `watermelon-ci.yml` + `.kbz`)
2. **Пайплайн** — сборка на DinD (`DOCKER_HOST=tcp://demo-vm-*`), push в `registry:5000`, deploy с labels
3. **Хосты** — WorkerNode с `engineUrl=http://demo-vm-*:2375`
4. **Сервисы** — live-список контейнеров, restart/stop/logs
5. **Registry** — артефакты проекта + live catalog

## Локальный jar (без compose)

```bash
cd console-ui && npm ci && npm run build
cp -R dist/. ../platform-app/src/main/resources/static/
cd ..
mvn -q -pl platform-app -am package -DskipTests
java -jar platform-app/target/platform-app-0.1.0-SNAPSHOT.jar
```

Auth по умолчанию выключен (`WATERMELON_AUTH_ENABLED=false`).

## Консоль

| Раздел | URL |
|--------|-----|
| Обзор | `/app` |
| Репозитории | `/app/repos` |
| Пайплайны | `/app/pipelines` |
| Хосты | `/app/hosts` |
| Сервисы | `/app/services` |
| Registry | `/app/registry` |
| Карбыз IDE | `/app/ide` |
| Документация | `/app/docs` |

## Карбыз

```bash
python3 karbyz/run.py test
python3 karbyz/run.py karbyz/examples/devops_demo.kbz --as-yaml
```

## Стек

Java 21 · Spring Boot · React · Postgres · Jenkins · Docker Registry · DinD fleet · Vault · Keycloak · Карбыз
