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
        templates.put("payments", PAYMENTS);
        templates.put("karbyz-payments", KARBYZ_PAYMENTS);
        templates.put("java-maven", JAVA_MAVEN);
        templates.put("node-docker", NODE_DOCKER);
        return templates;
    }

    /**
     * Build+push+deploy on demo-vm-a via DOCKER_HOST (DinD).
     * Workspace files (Dockerfile) must exist in the Jenkins job workspace —
     * for demo, scripts recreate a minimal app if checkout is empty.
     */
    public static final String STOREFRONT = """
            name: storefront
            variables:
              APP: storefront
              IMAGE: registry:5000/watermelon/storefront
              TARGET_HOST: tcp://demo-vm-a:2375
            stages: [build, publish, deploy]
            jobs:
              build:
                stage: build
                image: docker:27
                script:
                  - export DOCKER_HOST=$TARGET_HOST
                  - |
                    if [ ! -f Dockerfile ]; then
                      mkdir -p public
                      printf '%s\\n' 'FROM nginx:alpine' 'COPY public/ /usr/share/nginx/html/' 'EXPOSE 80' > Dockerfile
                      printf '%s\\n' '<!doctype html><html><body style="font-family:system-ui;background:#0b1913;color:#f3faf5;display:grid;place-items:center;min-height:100vh"><h1>Storefront</h1></body></html>' > public/index.html
                    fi
                  - docker build -t $IMAGE:latest .
              publish-image:
                stage: publish
                image: docker:27
                needs: [build]
                script:
                  - export DOCKER_HOST=$TARGET_HOST
                  - docker push $IMAGE:latest
              deploy-vm-a:
                stage: deploy
                environment: staging
                image: docker:27
                needs: [publish-image]
                script:
                  - echo "Deploy storefront → demo-vm-a"
                deploy:
                  runtime: docker
                  host: tcp://demo-vm-a:2375
                  track: true
                  release: storefront
                  image: registry:5000/watermelon/storefront:latest
                  ports: ["8080:80"]
            """;

    public static final String PAYMENTS = """
            name: payments
            variables:
              APP: payments
              IMAGE: registry:5000/watermelon/payments
              TARGET_HOST: tcp://demo-vm-b:2375
            stages: [build, publish, deploy]
            jobs:
              build:
                stage: build
                image: docker:27
                script:
                  - export DOCKER_HOST=$TARGET_HOST
                  - |
                    if [ ! -f Dockerfile ]; then
                      mkdir -p public
                      printf '%s\\n' 'FROM nginx:alpine' 'COPY public/ /usr/share/nginx/html/' 'EXPOSE 80' > Dockerfile
                      printf '%s\\n' '<!doctype html><html><body style="font-family:system-ui;background:#0b1913;color:#f3faf5;display:grid;place-items:center;min-height:100vh"><h1>Payments</h1></body></html>' > public/index.html
                    fi
                  - docker build -t $IMAGE:latest .
              publish-image:
                stage: publish
                image: docker:27
                needs: [build]
                script:
                  - export DOCKER_HOST=$TARGET_HOST
                  - docker push $IMAGE:latest
              deploy-vm-b:
                stage: deploy
                environment: staging
                image: docker:27
                needs: [publish-image]
                script:
                  - echo "Deploy payments → demo-vm-b"
                deploy:
                  runtime: docker
                  host: tcp://demo-vm-b:2375
                  track: true
                  release: payments
                  image: registry:5000/watermelon/payments:latest
                  ports: ["8081:80"]
            """;

    public static final String KARBYZ_STOREFRONT = """
            жыелма storefront ::

              суз app <- "storefront"
              суз image <- "registry:5000/watermelon/storefront"

              этап "build" ::
                башкар "export DOCKER_HOST=tcp://demo-vm-a:2375"
                башкар "docker build -t registry:5000/watermelon/storefront:latest ."
                чыгар ("собрали образ", app)
              ахыр

              этап "publish" ::
                башкар "export DOCKER_HOST=tcp://demo-vm-a:2375"
                башкар "docker push registry:5000/watermelon/storefront:latest"
                чыгар ("в registry", image)
              ахыр

              сервер "demo-vm-a" ::
                башкар "export DOCKER_HOST=tcp://demo-vm-a:2375"
                башкар "docker pull registry:5000/watermelon/storefront:latest"
                башкар "docker rm -f storefront 2>/dev/null || true"
                башкар "docker run -d --name storefront -p 8080:80 --label watermelon.ci/project=storefront --label watermelon.ci/environment=staging --label watermelon.ci/track=true registry:5000/watermelon/storefront:latest"
              ахыр

              этап "deploy" ::
                башкар "echo deploy storefront on demo-vm-a"
                чыгар ("раскатка", app)
              ахыр

            ахыр жыелма.
            """;

    public static final String KARBYZ_PAYMENTS = """
            жыелма payments ::

              суз app <- "payments"

              этап "build" ::
                башкар "export DOCKER_HOST=tcp://demo-vm-b:2375"
                башкар "docker build -t registry:5000/watermelon/payments:latest ."
              ахыр

              этап "publish" ::
                башкар "export DOCKER_HOST=tcp://demo-vm-b:2375"
                башкар "docker push registry:5000/watermelon/payments:latest"
              ахыр

              сервер "demo-vm-b" ::
                башкар "export DOCKER_HOST=tcp://demo-vm-b:2375"
                башкар "docker pull registry:5000/watermelon/payments:latest"
                башкар "docker rm -f payments 2>/dev/null || true"
                башкар "docker run -d --name payments -p 8081:80 --label watermelon.ci/project=payments --label watermelon.ci/environment=staging --label watermelon.ci/track=true registry:5000/watermelon/payments:latest"
              ахыр

              этап "deploy" ::
                башкар "echo deploy payments on demo-vm-b"
                чыгар ("раскатка", app)
              ахыр

            ахыр жыелма.
            """;

    public static final String JAVA_MAVEN = """
            name: java-maven
            stages: [build, publish, deploy]
            jobs:
              build:
                stage: build
                image: docker:27
                script:
                  - docker build -t $WM_REGISTRY/demo:latest .
              deploy-staging:
                stage: deploy
                environment: staging
                image: docker:27
                script:
                  - echo deploying staging
                deploy:
                  runtime: docker
                  host: tcp://demo-vm-a:2375
                  track: true
                  release: app-staging
                  image: registry:5000/watermelon/demo:latest
                  ports: ["8080:80"]
            publish:
              docker:
                - image: demo
                  tags: ["latest"]
              maven:
                deploy: true
            """;

    public static final String NODE_DOCKER = """
            name: node-docker
            stages: [build, publish]
            jobs:
              build:
                stage: build
                image: docker:27
                script:
                  - docker build -t $WM_REGISTRY/demo:latest .
            publish:
              docker:
                - image: demo
                  tags: ["latest"]
            """;
}
