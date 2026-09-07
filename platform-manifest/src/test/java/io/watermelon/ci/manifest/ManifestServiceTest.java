package io.watermelon.ci.manifest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.watermelon.ci.manifest.compile.CompileContext;
import io.watermelon.ci.manifest.compile.CompiledPipeline;
import io.watermelon.ci.manifest.compile.JenkinsfileCompiler;
import io.watermelon.ci.manifest.parse.ManifestParser;
import io.watermelon.ci.manifest.template.ManifestTemplates;
import io.watermelon.ci.manifest.validate.ManifestValidator;
import org.junit.jupiter.api.Test;

class ManifestServiceTest {

    private final ManifestService service =
            new ManifestService(new ManifestParser(), new ManifestValidator(), new JenkinsfileCompiler());

    @Test
    void compilesJavaMavenTemplateWithRegistryAndDeployTracking() {
        CompiledPipeline compiled = service.compile(
                ManifestTemplates.JAVA_MAVEN,
                new CompileContext(
                        "demo",
                        "acme",
                        "registry.local/watermelon",
                        "http://nexus.local/repository/maven-releases/",
                        "11111111-1111-1111-1111-111111111111",
                        "wm-creds"));

        assertTrue(compiled.jobName().contains("demo"));
        assertTrue(compiled.jenkinsfile().contains("pipeline {"));
        assertTrue(compiled.jenkinsfile().contains("stage('publish')"));
        assertTrue(compiled.jenkinsfile().contains("watermelon.ci/pipeline-run"));
        assertTrue(compiled.jenkinsfile().contains("docker push"));
        assertTrue(compiled.jenkinsfile().contains("WM_MAVEN_REPO"));
    }
}
