package io.watermelon.ci.manifest.template;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ManifestTemplates {

    public Map<String, String> all() {
        Map<String, String> templates = new LinkedHashMap<>();
        templates.put("storefront", STOREFRONT);
        templates.put("karbyz-storefront", KARBYZ_STOREFRONT);
        templates.put("java-maven", JAVA_MAVEN);
        templates.put("node-docker", NODE_DOCKER);
        templates.put("ansible-deploy", ANSIBLE_DEPLOY);
        templates.put("argocd-app", ARGOCD_APP);
        return templates;
    }

    public static final String JAVA_MAVEN = """
            name: java-maven
            stages: [build, test, publish, deploy]
            jobs:
              build:
                stage: build
                image: eclipse-temurin:21
                script:
                  - ./mvnw -B -DskipTests package
              test:
                stage: test
                image: eclipse-temurin:21
                script:
                  - ./mvnw -B test
              deploy-staging:
                stage: deploy
                when: manual
                environment: staging
                image: docker:27
                script:
                  - echo deploying staging
                deploy:
                  runtime: docker
                  track: true
                  release: app-staging
                  image: registry.local/demo:latest
                  ports: ["8080:8080"]
            publish:
              docker:
                - image: demo
                  tags: ["latest"]
              maven:
                deploy: true
            """;

    public static final String NODE_DOCKER = """
            name: node-docker
            stages: [build, test, publish]
            jobs:
              build:
                stage: build
                image: node:22
                script:
                  - npm ci
                  - npm run build
              test:
                stage: test
                image: node:22
                script:
                  - npm test
            publish:
              docker:
                - image: demo
                  tags: ["latest"]
            """;

    public static final String ANSIBLE_DEPLOY = """
            name: ansible-deploy
            stages: [lint, deploy]
            jobs:
              lint:
                stage: lint
                image: cytopia/ansible:latest
                script:
                  - ansible-lint playbook.yml
              deploy:
                stage: deploy
                when: manual
                environment: production
                image: cytopia/ansible:latest
                script:
                  - ansible-playbook -i inventories/prod playbook.yml
            """;

    public static final String ARGOCD_APP = """
            name: argocd-app
            stages: [build, publish, deploy]
            jobs:
              build:
                stage: build
                image: eclipse-temurin:21
                script:
                  - ./mvnw -B -DskipTests package
              deploy-prod:
                stage: deploy
                when: manual
                environment: production
                image: curlimages/curl:8.10.1
                script:
                  - echo "GitOps deploy via Watermelon → ArgoCD"
                deploy:
                  runtime: argocd
                  track: true
                  release: demo-api
                  namespace: demo-prod
                  replicas: 2
                  image: registry.local/watermelon/demo:latest
                  ports: ["8080"]
                  secrets: ["DATABASE_URL", "API_TOKEN"]
            publish:
              docker:
                - image: demo
                  tags: ["latest"]
            """;

    /** Полный цикл Storefront: git → build → test → registry → secrets → deploy. */
    public static final String STOREFRONT = """
            name: storefront
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
            """;

    /** Тот же Storefront-пайплайн на Карбызе. */
    public static final String KARBYZ_STOREFRONT = """
            жыелма storefront ::

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
            """;
}
