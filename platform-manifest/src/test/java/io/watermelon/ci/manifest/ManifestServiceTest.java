package io.watermelon.ci.manifest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.watermelon.ci.manifest.compile.CompileContext;
import io.watermelon.ci.manifest.compile.CompiledPipeline;
import io.watermelon.ci.manifest.compile.JenkinsfileCompiler;
import io.watermelon.ci.manifest.parse.KarbyzManifestParser;
import io.watermelon.ci.manifest.parse.ManifestParser;
import io.watermelon.ci.manifest.template.ManifestTemplates;
import io.watermelon.ci.manifest.validate.ManifestValidator;
import org.junit.jupiter.api.Test;

class ManifestServiceTest {

    private final ManifestService service = new ManifestService(
            new ManifestParser(),
            new KarbyzManifestParser(),
            new ManifestValidator(),
            new JenkinsfileCompiler());

    private static final CompileContext CTX = new CompileContext(
            "demo",
            "acme",
            "registry.local/watermelon",
            "http://nexus.local/repository/maven-releases/",
            "11111111-1111-1111-1111-111111111111",
            "wm-creds");

    @Test
    void compilesJavaMavenTemplateWithRegistryAndDeployTracking() {
        CompiledPipeline compiled = service.compile(ManifestTemplates.JAVA_MAVEN, CTX);

        assertTrue(compiled.jobName().contains("demo"));
        assertTrue(compiled.jenkinsfile().contains("pipeline {"));
        assertTrue(compiled.jenkinsfile().contains("stage('publish')"));
        assertTrue(compiled.jenkinsfile().contains("watermelon.ci/pipeline-run"));
        assertTrue(compiled.jenkinsfile().contains("docker push"));
        assertTrue(compiled.jenkinsfile().contains("WM_MAVEN_REPO"));
    }

    @Test
    void compilesKarbyzManifestDirectlyToJenkinsfile() {
        CompiledPipeline compiled = service.compile(ManifestTemplates.KARBYZ_STOREFRONT, CTX);

        assertTrue(compiled.jobName().contains("demo"));
        assertTrue(compiled.jenkinsfile().contains("pipeline {"));
        assertTrue(compiled.jenkinsfile().contains("stage('build')"));
        assertTrue(compiled.jenkinsfile().contains("stage('test')"));
        assertTrue(compiled.jenkinsfile().contains("stage('publish')"));
        assertTrue(compiled.jenkinsfile().contains("stage('deploy')"));
        assertTrue(compiled.jenkinsfile().contains("./mvnw"));
    }

    @Test
    void compilesStorefrontYamlFullCycle() {
        CompiledPipeline compiled = service.compile(ManifestTemplates.STOREFRONT, CTX);
        assertTrue(compiled.jenkinsfile().contains("stage('checkout')"));
        assertTrue(compiled.jenkinsfile().contains("stage('publish')"));
        assertTrue(compiled.jenkinsfile().contains("docker push"));
        assertTrue(compiled.jenkinsfile().contains("argocd") || compiled.jenkinsfile().contains("storefront"));
    }
}
