package io.watermelon.ci.manifest.template;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ManifestTemplates {

    public Map<String, String> all() {
        Map<String, String> templates = new LinkedHashMap<>();
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
}
