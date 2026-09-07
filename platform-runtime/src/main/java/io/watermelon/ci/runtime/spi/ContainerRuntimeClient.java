package io.watermelon.ci.runtime.spi;

import io.watermelon.ci.domain.deployment.DeploymentStatus;
import java.util.List;
import java.util.Optional;

public interface ContainerRuntimeClient {

    List<ObservedContainer> listWatermelonContainers();

    Optional<ObservedContainer> inspect(String externalId);

    record ObservedContainer(
            String externalId,
            String name,
            String image,
            String state,
            DeploymentStatus mappedStatus,
            String pipelineRunId,
            String projectSlug,
            String environment
    ) {}
}
