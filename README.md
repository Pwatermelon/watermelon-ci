# Watermelon CI

Корпоративная CI/CD-платформа на Java 21 / Spring Boot 3.5:

- Git (Forgejo/Gitea)
- Issues и RBAC
- YAML-манифесты в стиле GitLab CI → безопасная компиляция в Jenkinsfile
- Реестры артефактов: **Docker Registry** + **Maven (Nexus)**
- Наблюдение за **задеплоенными контейнерами** прямо из CI (labels + heartbeat + reconcile)

## Архитектура модулей

| Модуль | Назначение |
|--------|------------|
| `platform-common` | ошибки, id, clock, slug |
| `platform-domain` | JPA-модель + Flyway |
| `platform-manifest` | schema / parser / validator / Jenkinsfile compiler |
| `platform-jenkins` | Jenkins API + allowlist плагинов |
| `platform-git` | Forgejo client + editor API backend |
| `platform-identity` | группы доступа, membership, Security |
| `platform-issues` | задачи |
| `platform-registry` | каталог Docker/Maven артефактов |
| `platform-runtime` | tracking деплоев / Docker Engine reconcile |
| `platform-fleet` | **Fleet admin**: clusters, worker nodes, container control |
| `platform-orchestration` | сценарии org → project → pipeline |
| `platform-api` | REST + OpenAPI |
| `platform-app` | bootable приложение + встроенная консоль |
| `console-ui` | React админ-панель флота (как k8s web UI внутри CI) |

```
UI / Monaco editor
        │
   platform-api
        │
 orchestration ──► manifest ──► jenkins
        │              │
        ├─ registry (Docker + Maven)
        ├─ runtime  (deployed containers)
        ├─ git      (Forgejo)
        └─ issues / identity
```

## Манифест (пример)

```yaml
name: java-maven
stages: [build, test, publish, deploy]
jobs:
  build:
    stage: build
    image: eclipse-temurin:21
    script:
      - ./mvnw -B -DskipTests package
  deploy-staging:
    stage: deploy
    when: manual
    environment: staging
    image: docker:27
    script:
      - echo deploying
    deploy:
      runtime: docker
      track: true
      release: app-staging
      image: registry.local/watermelon/demo:latest
      ports: ["8080:8080"]
publish:
  docker:
    - image: demo
      tags: ["latest"]
  maven:
    deploy: true
```

`deploy.track: true` вешает labels `watermelon.ci/*`, шлёт heartbeat в `/api/v1/runtime/agent/heartbeat`, а control plane периодически сверяет Docker Engine.

## Secrets (Vault) + GitOps (ArgoCD)

Секреты проекта живут в **HashiCorp Vault / OpenBao**, метаданные — в БД.
Деплой — **надстройка над ArgoCD**: из короткого YAML платформа сама рендерит Deployment/Service/Secret и Application, без ручного Helm.

```yaml
deploy:
  runtime: argocd
  release: demo-api
  namespace: demo-prod
  replicas: 2
  image: registry.local/watermelon/demo:latest
  ports: ["8080"]
  secrets: ["DATABASE_URL", "API_TOKEN"]
```

API секретов:
- `PUT /api/v1/projects/{id}/secrets`
- `GET /api/v1/projects/{id}/secrets?environment=production`

API GitOps:
- `POST /api/v1/projects/{id}/gitops/deploy`
- `GET  /api/v1/projects/{id}/gitops/applications`
- `POST /api/v1/projects/{id}/gitops/applications/{appId}/sync`

Путь в Vault: `secret/data/watermelon/{org}/{project}/{env}`

По умолчанию `watermelon.gitops.dry-run=true` — рендер и запись файлов локально, без обязательного живого Argo CD.

## Fleet Admin Console (in-platform)

После деплоя контейнеры управляются **внутри Watermelon**, без Portainer/Lens/kubectl UI:

1. Откройте `http://localhost:8088/` — React Fleet Console
2. Создайте organization → cluster
3. **Add worker node** → получите join token
4. На сервере воркера:

```bash
export WM_API_URL=http://<ci-host>:8088
export WM_CLUSTER=<cluster-id>
export WM_NODE_ID=<node-id>
export WM_JOIN_TOKEN=<token>
export WM_ENGINE_URL=http://127.0.0.1:2375
bash deploy/agent/join-node.sh
```

5. В консоли: start/stop/restart/logs/remove контейнеров, drain/activate нод

API: `/api/v1/fleet/...`

## Быстрый запуск

```bash
# сборка
mvn -q verify

# локально (H2)
mvn -pl platform-app spring-boot:run

# полный стек: Postgres + Forgejo + Jenkins + Registry + Nexus + CI
docker compose up -d postgres registry nexus
```

API: `http://localhost:8088/swagger-ui.html`

## Ключевые API

- `POST /api/v1/organizations`
- `POST /api/v1/organizations/{id}/projects`
- `POST /api/v1/projects/{id}/pipelines` — validate → compile → Jenkins
- `POST /api/v1/projects/{id}/manifests/preview`
- `GET  /api/v1/projects/{id}/artifacts`
- `GET  /api/v1/projects/{id}/deployments`
- `POST /api/v1/runtime/agent/heartbeat`
- `GET  /api/v1/editor/repos/{owner}/{repo}/file`
- `GET  /api/v1/manifest-templates`
- `GET  /api/v1/registry/endpoints`

## Безопасность Jenkins

Пользователи **не пишут Groovy**. Платформа генерирует Declarative Pipeline в sandbox. Плагины — только из allowlist (`AllowedJenkinsPlugins`). Jenkins не торчит наружу без reverse-proxy/ACL.

## Linux / on-prem

- `docker compose` — основной путь
- `deploy/ansible/playbooks/site.yml` — установка на хосты
- Профиль `postgres` для production БД

## Дальше

1. Harbor вместо raw registry (CVE scan, retention)
2. OIDC/Keycloak вместо permit-all security
3. Monaco UI + wizard манифестов
4. Kubernetes runtime client рядом с Docker Engine
