FROM jenkins/jenkins:lts-jdk21
USER root
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl ca-certificates \
  && ARCH="$(dpkg --print-architecture)" \
  && case "$ARCH" in amd64) DOCKER_ARCH=x86_64 ;; arm64) DOCKER_ARCH=aarch64 ;; *) DOCKER_ARCH=x86_64 ;; esac \
  && curl -fsSL "https://download.docker.com/linux/static/stable/${DOCKER_ARCH}/docker-27.3.1.tgz" \
    | tar -xz -C /usr/local/bin --strip-components=1 docker/docker \
  && chmod +x /usr/local/bin/docker \
  && docker --version \
  && rm -rf /var/lib/apt/lists/*
USER jenkins
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
