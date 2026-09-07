package io.watermelon.ci.registry.spi;

import io.watermelon.ci.domain.artifact.ArtifactKind;
import java.util.List;

public interface ArtifactRegistryClient {

    ArtifactKind kind();

    List<RemoteArtifact> list(String projectKey);

    record RemoteArtifact(String name, String version, String locator, String digest) {}
}
