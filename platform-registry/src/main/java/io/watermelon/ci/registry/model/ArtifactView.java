package io.watermelon.ci.registry.model;

import io.watermelon.ci.domain.artifact.ArtifactKind;
import java.time.Instant;
import java.util.UUID;

public record ArtifactView(
        UUID id,
        UUID projectId,
        UUID pipelineRunId,
        ArtifactKind kind,
        String name,
        String version,
        String locator,
        String digest,
        Instant createdAt
) {}
