package io.watermelon.ci.jenkins.security;

import java.util.Set;

/**
 * Hard allowlist of Jenkins plugins permitted in Watermelon-managed controllers.
 */
public final class AllowedJenkinsPlugins {

    public static final Set<String> ALLOWLIST = Set.of(
            "workflow-aggregator",
            "pipeline-model-definition",
            "pipeline-stage-view",
            "credentials-binding",
            "docker-workflow",
            "docker-plugin",
            "git",
            "configuration-as-code",
            "job-dsl",
            "timestamper",
            "ws-cleanup",
            "ansicolor",
            "kubernetes",
            "role-strategy");

    private AllowedJenkinsPlugins() {}

    public static boolean isAllowed(String pluginId) {
        return ALLOWLIST.contains(pluginId);
    }
}
