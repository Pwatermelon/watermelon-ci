/** Демо Storefront: build → registry → deploy на demo-vm-a */

export const STOREFRONT_PROJECT = {
  name: 'Storefront',
  slug: 'storefront',
  org: 'watermelon-demo',
  image: 'registry:5000/watermelon/storefront:latest',
  env: 'staging',
}

export const STOREFRONT_YAML = `name: storefront
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
      - echo "Deploy storefront to demo-vm-a"
    deploy:
      runtime: docker
      host: tcp://demo-vm-a:2375
      track: true
      release: storefront
      image: registry:5000/watermelon/storefront:latest
      ports: ["8080:80"]
`

export const STOREFRONT_KARBYZ = `жьелма storefront ::

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
`
