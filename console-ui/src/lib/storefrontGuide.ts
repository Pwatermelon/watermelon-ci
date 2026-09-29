/** Готовый демо-проект Storefront: git → build → pipeline → registry → secrets → deploy. */

export type GuideDialect = {
  title: string
  note?: string
  code: string
}

export type GuideStep = {
  id: string
  title: string
  eyebrow: string
  summary: string
  bullets: string[]
  where: string
  yaml?: GuideDialect
  karbyz?: GuideDialect
  tip?: string
}

export const STOREFRONT_PROJECT = {
  name: 'Storefront API',
  slug: 'storefront',
  org: 'watermelon-demo',
  gitUrl: 'git@forgejo.local:watermelon-demo/storefront.git',
  branch: 'main',
  image: 'registry.local/watermelon/storefront:1.0.0',
  env: 'production',
  secrets: ['DATABASE_URL', 'API_TOKEN'],
  tree: `storefront/
  src/main/java/…/StorefrontApp.java
  pom.xml
  Dockerfile
  watermelon-ci.kbz      ← манифест (Карбыз)
  watermelon-ci.yml      ← тот же манифест (YAML)
  README.md`,
}

/** Полный пайплайн на языке Watermelon (YAML). */
export const STOREFRONT_YAML = `name: storefront
variables:
  APP: storefront
  REGISTRY: registry.local/watermelon
stages: [checkout, build, test, publish, deploy]
jobs:
  checkout:
    stage: checkout
    image: alpine/git:2.45.2
    script:
      - git clone --branch main git@forgejo.local:watermelon-demo/storefront.git .
      - git rev-parse --short HEAD
  build:
    stage: build
    image: eclipse-temurin:21
    needs: [checkout]
    script:
      - ./mvnw -B -DskipTests package
  test:
    stage: test
    image: eclipse-temurin:21
    needs: [build]
    script:
      - ./mvnw -B test
  publish-image:
    stage: publish
    image: docker:27
    needs: [test]
    script:
      - docker build -t $REGISTRY/$APP:1.0.0 -t $REGISTRY/$APP:latest .
      - docker push $REGISTRY/$APP:1.0.0
      - docker push $REGISTRY/$APP:latest
  deploy-prod:
    stage: deploy
    when: manual
    environment: production
    image: curlimages/curl:8.10.1
    needs: [publish-image]
    script:
      - echo "GitOps sync Storefront → ArgoCD"
    deploy:
      runtime: argocd
      track: true
      release: storefront
      namespace: storefront-prod
      replicas: 2
      image: registry.local/watermelon/storefront:1.0.0
      ports: ["8080"]
      secrets: ["DATABASE_URL", "API_TOKEN"]
publish:
  docker:
    - image: storefront
      tags: ["1.0.0", "latest"]
  maven:
    deploy: true
`

/** Тот же пайплайн на Карбызе (манифест Watermelon, татарский синтаксис). */
export const STOREFRONT_KARBYZ = `жыелма storefront ::

  суз app <- "storefront"
  суз registry <- "registry.local/watermelon"

  этап "checkout" ::
    башкар "git clone --branch main git@forgejo.local:watermelon-demo/storefront.git ."
    башкар "git rev-parse --short HEAD"
  ахыр

  этап "build" ::
    башкар "./mvnw -B -DskipTests package"
    чыгар ("собрали", app)
  ахыр

  этап "test" ::
    башкар "./mvnw -B test"
  ахыр

  этап "publish" ::
    башкар "docker build -t registry.local/watermelon/storefront:1.0.0 ."
    башкар "docker push registry.local/watermelon/storefront:1.0.0"
    башкар "docker push registry.local/watermelon/storefront:latest"
    чыгар ("образ в registry:", registry)
  ахыр

  сервер "prod-node" ::
    башкар "echo inject Vault secrets DATABASE_URL API_TOKEN"
    башкар "echo ArgoCD sync storefront-prod"
  ахыр

  этап "deploy" ::
    башкар "echo GitOps deploy storefront"
    чыгар ("раскатка", app)
  ахыр

ахыр жыелма.
`

export const STOREFRONT_GUIDE: GuideStep[] = [
  {
    id: 'git',
    title: 'Код в Git',
    eyebrow: '1 · Репозиторий',
    summary:
      'Демо-проект Storefront лежит в org watermelon-demo. Платформа клонирует репозиторий на стадии checkout — это источник истины для сборки и раскатки.',
    bullets: [
      'Org: watermelon-demo · проект: storefront',
      'Ветка main, короткий SHA попадает в pipeline run',
      'В репо рядом лежат watermelon-ci.yml и watermelon-ci.kbz — один манифест, два синтаксиса',
    ],
    where: 'Демо → Проекты → Storefront API',
    tip: 'В MVP Git может быть Forgejo; для локального демо checkout уже записан в манифесте командами git clone.',
    yaml: {
      title: 'Фрагмент стадии checkout (YAML)',
      code: `checkout:
  stage: checkout
  image: alpine/git:2.45.2
  script:
    - git clone --branch main git@forgejo.local:watermelon-demo/storefront.git .
    - git rev-parse --short HEAD`,
    },
    karbyz: {
      title: 'Та же стадия (Карбыз)',
      code: `этап "checkout" ::
  башкар "git clone --branch main git@forgejo.local:watermelon-demo/storefront.git ."
  башкар "git rev-parse --short HEAD"
ахыр`,
    },
  },
  {
    id: 'build',
    title: 'Сборка и тесты',
    eyebrow: '2 · Build / Test',
    summary:
      'После checkout Maven собирает JAR и гоняет тесты. Образы агентов задаются в манифесте (eclipse-temurin:21) — пользователю не нужен Groovy.',
    bullets: [
      './mvnw -B -DskipTests package — артефакт приложения',
      './mvnw -B test — обязательный quality gate',
      'needs связывает стадии: test ждёт build',
    ],
    where: 'Проект → Pipelines · или Манифесты → Compile',
    yaml: {
      title: 'build + test (YAML)',
      code: `build:
  stage: build
  image: eclipse-temurin:21
  needs: [checkout]
  script:
    - ./mvnw -B -DskipTests package
test:
  stage: test
  image: eclipse-temurin:21
  needs: [build]
  script:
    - ./mvnw -B test`,
    },
    karbyz: {
      title: 'build + test (Карбыз)',
      code: `этап "build" ::
  башкар "./mvnw -B -DskipTests package"
  чыгар ("собрали", app)
ахыр

этап "test" ::
  башкар "./mvnw -B test"
ахыр`,
    },
  },
  {
    id: 'registry',
    title: 'Registry: образ приложения',
    eyebrow: '3 · Publish',
    summary:
      'Успешные тесты → docker build/push в registry.local/watermelon. В каталоге артефактов демо уже есть storefront:1.0.0.',
    bullets: [
      'Теги: 1.0.0 и latest',
      'publish.docker в YAML — декларация для компилятора Jenkinsfile',
      'Карбыз пишет те же команды docker build/push в этапе publish',
    ],
    where: 'Проект → Artifacts · Compose: registry :5001',
    tip: 'Локально registry проброшен на порт 5001 (docker compose).',
    yaml: {
      title: 'publish (YAML)',
      note: 'Блок publish + job publish-image',
      code: `publish-image:
  stage: publish
  image: docker:27
  needs: [test]
  script:
    - docker build -t $REGISTRY/$APP:1.0.0 .
    - docker push $REGISTRY/$APP:1.0.0
    - docker push $REGISTRY/$APP:latest
publish:
  docker:
    - image: storefront
      tags: ["1.0.0", "latest"]`,
    },
    karbyz: {
      title: 'publish (Карбыз)',
      code: `этап "publish" ::
  башкар "docker build -t registry.local/watermelon/storefront:1.0.0 ."
  башкар "docker push registry.local/watermelon/storefront:1.0.0"
  башкар "docker push registry.local/watermelon/storefront:latest"
  чыгар ("образ в registry:", registry)
ахыр`,
    },
  },
  {
    id: 'secrets',
    title: 'Секреты (Vault)',
    eyebrow: '4 · Secrets',
    summary:
      'Секреты не кладут в Git. В демо для production уже заведены DATABASE_URL и API_TOKEN. Раскатка подтягивает их по именам из манифеста.',
    bullets: [
      'Environment: production',
      'Ключи: DATABASE_URL, API_TOKEN',
      'В YAML: deploy.secrets: […] — инъекция в рантайм',
      'В Карбызе: явная команда на узле + тот же смысл на этапе deploy',
    ],
    where: 'Проект → Secrets',
    tip: 'Если Vault недоступен, платформа пишет метаданные через memory-fallback — демо всё равно показывает путь.',
    yaml: {
      title: 'Секреты в deploy (YAML)',
      code: `deploy:
  runtime: argocd
  environment wiring via job:
  secrets: ["DATABASE_URL", "API_TOKEN"]
# значения живут в Vault / demo seed, не в репозитории`,
    },
    karbyz: {
      title: 'Секреты на узле (Карбыз)',
      code: `сервер "prod-node" ::
  башкар "echo inject Vault secrets DATABASE_URL API_TOKEN"
  башкар "echo ArgoCD sync storefront-prod"
ахыр`,
    },
  },
  {
    id: 'deploy',
    title: 'Раскатка (GitOps / ArgoCD)',
    eyebrow: '5 · Deploy',
    summary:
      'Финальная стадия — ручная (when: manual). Watermelon отдаёт образ и секреты в ArgoCD Application без Helm. В UI — вкладка GitOps.',
    bullets: [
      'runtime: argocd · release: storefront · namespace: storefront-prod',
      'image: registry.local/watermelon/storefront:1.0.0',
      'replicas: 2 · port 8080',
      'Карбыз: этап deploy + сервер prod-node — тот же смысл пайплайна',
    ],
    where: 'Проект → GitOps → Deploy via ArgoCD · Флот для наблюдения',
    yaml: {
      title: 'deploy-prod (YAML)',
      code: `deploy-prod:
  stage: deploy
  when: manual
  environment: production
  needs: [publish-image]
  script:
    - echo "GitOps sync Storefront → ArgoCD"
  deploy:
    runtime: argocd
    track: true
    release: storefront
    namespace: storefront-prod
    replicas: 2
    image: registry.local/watermelon/storefront:1.0.0
    ports: ["8080"]
    secrets: ["DATABASE_URL", "API_TOKEN"]`,
    },
    karbyz: {
      title: 'deploy (Карбыз)',
      code: `этап "deploy" ::
  башкар "echo GitOps deploy storefront"
  чыгар ("раскатка", app)
ахыр`,
    },
  },
  {
    id: 'full',
    title: 'Полные манифесты рядом',
    eyebrow: '6 · Карбыз ≡ YAML',
    summary:
      'Ниже — целиком один и тот же пайплайн Storefront. Сначала Карбыз (основной синтаксис демо), рядом YAML. Compile даёт Jenkinsfile для обоих.',
    bullets: [
      'Карбыз — тот же манифест стадиями этап/башкар/сервер',
      'YAML — полная модель (needs, publish, deploy.argocd, secrets)',
      'Не «генерация YAML из Карбыза»: обе формы — Watermelon CI',
    ],
    where: 'Storefront → Гайд / Карбыз · или Карбыз IDE → Compile',
    karbyz: {
      title: 'watermelon-ci.kbz',
      code: STOREFRONT_KARBYZ,
    },
    yaml: {
      title: 'watermelon-ci.yml',
      code: STOREFRONT_YAML,
    },
  },
]
