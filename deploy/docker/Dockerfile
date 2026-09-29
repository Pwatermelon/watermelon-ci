FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY platform-common/pom.xml platform-common/pom.xml
COPY platform-domain/pom.xml platform-domain/pom.xml
COPY platform-manifest/pom.xml platform-manifest/pom.xml
COPY platform-jenkins/pom.xml platform-jenkins/pom.xml
COPY platform-git/pom.xml platform-git/pom.xml
COPY platform-identity/pom.xml platform-identity/pom.xml
COPY platform-issues/pom.xml platform-issues/pom.xml
COPY platform-registry/pom.xml platform-registry/pom.xml
COPY platform-runtime/pom.xml platform-runtime/pom.xml
COPY platform-fleet/pom.xml platform-fleet/pom.xml
COPY platform-secrets/pom.xml platform-secrets/pom.xml
COPY platform-gitops/pom.xml platform-gitops/pom.xml
COPY platform-orchestration/pom.xml platform-orchestration/pom.xml
COPY platform-api/pom.xml platform-api/pom.xml
COPY platform-app/pom.xml platform-app/pom.xml
RUN mvn -B -q -DskipTests dependency:go-offline || true
COPY . .
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update \
  && apt-get install -y --no-install-recommends python3 \
  && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/platform-app/target/platform-app-*.jar /app/app.jar
COPY --from=build /workspace/karbyz /opt/karbyz
ENV WATERMELON_KARBYZ_HOME=/opt/karbyz
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
