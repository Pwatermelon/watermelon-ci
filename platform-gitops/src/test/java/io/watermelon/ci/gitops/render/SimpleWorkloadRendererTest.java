package io.watermelon.ci.gitops.render;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SimpleWorkloadRendererTest {

    private final SimpleWorkloadRenderer renderer = new SimpleWorkloadRenderer();

    @Test
    void rendersDeploymentServiceAndSecretPlaceholdersWithoutHelm() {
        String yaml = renderer.render(new SimpleWorkloadRenderer.WorkloadSpec(
                "demo-api",
                "demo-prod",
                "registry.local/watermelon/demo:latest",
                2,
                List.of("8080"),
                List.of("DATABASE_URL", "API_TOKEN"),
                "watermelon/acme/demo/production",
                Map.of("app", "demo-api", "watermelon.ci/project", "demo")));

        assertTrue(yaml.contains("kind: Deployment"));
        assertTrue(yaml.contains("kind: Service"));
        assertTrue(yaml.contains("kind: Secret"));
        assertTrue(yaml.contains("DATABASE_URL"));
        assertTrue(yaml.contains("replicas: 2"));
        assertTrue(!yaml.contains("helm"));
    }
}
