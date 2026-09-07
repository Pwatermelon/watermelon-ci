package io.watermelon.ci.runtime.model;

import io.watermelon.ci.domain.deployment.DeploymentStatus;

public record AgentHeartbeatRequest(
        String pipelineRunId,
        String externalId,
        String environment,
        String imageRef,
        String releaseName,
        DeploymentStatus status,
        String message
) {}
