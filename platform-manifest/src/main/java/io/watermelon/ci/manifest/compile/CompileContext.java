package io.watermelon.ci.manifest.compile;

public record CompileContext(
        String projectSlug,
        String organizationSlug,
        String registryHost,
        String mavenRepositoryUrl,
        String pipelineRunId,
        String credentialsId,
        String projectId,
        String platformUrl
) {
    public CompileContext(
            String projectSlug,
            String organizationSlug,
            String registryHost,
            String mavenRepositoryUrl,
            String pipelineRunId,
            String credentialsId) {
        this(projectSlug, organizationSlug, registryHost, mavenRepositoryUrl, pipelineRunId, credentialsId, "", "http://localhost:8088");
    }
}
