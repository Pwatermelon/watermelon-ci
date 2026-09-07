package io.watermelon.ci.runtime.model;

import io.watermelon.ci.domain.deployment.DeploymentStatus;
import io.watermelon.ci.domain.deployment.RuntimeTarget;
import java.time.Instant;
import java.util.UUID;

public record DeploymentView(
        UUID id,
        UUID projectId,
        UUID pipelineRunId,
        String environment,
        RuntimeTarget runtime,
        String imageRef,
        String releaseName,
        String externalId,
        DeploymentStatus status,
        String statusMessage,
        Instant createdAt,
        Instant lastSeenAt
) {}
