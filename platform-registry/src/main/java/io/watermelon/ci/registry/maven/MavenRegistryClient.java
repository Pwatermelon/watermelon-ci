package io.watermelon.ci.registry.maven;

import io.watermelon.ci.domain.artifact.ArtifactKind;
import io.watermelon.ci.registry.config.RegistryProperties;
import io.watermelon.ci.registry.spi.ArtifactRegistryClient;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Maven registry facade over Nexus/Artifactory.
 * Catalog browsing is intentionally thin — CI records published GAVs in the platform DB.
 */
@Component
public class MavenRegistryClient implements ArtifactRegistryClient {

    private static final Logger log = LoggerFactory.getLogger(MavenRegistryClient.class);

    private final RegistryProperties properties;

    public MavenRegistryClient(RegistryProperties properties) {
        this.properties = properties;
    }

    @Override
    public ArtifactKind kind() {
        return ArtifactKind.MAVEN_PACKAGE;
    }

    @Override
    public List<RemoteArtifact> list(String projectKey) {
        log.debug("Maven remote browse delegated to platform artifact index for {}", projectKey);
        return List.of();
    }

    public String repositoryUrl() {
        return properties.getMaven().getRepositoryUrl();
    }

    public String locator(String groupId, String artifactId, String version) {
        return properties.getMaven().getRepositoryUrl() + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/";
    }
}
