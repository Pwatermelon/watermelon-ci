package io.watermelon.ci.runtime.service;

import io.watermelon.ci.common.error.NotFoundException;
import io.watermelon.ci.common.ids.Ids;
import io.watermelon.ci.common.time.Clock;
import io.watermelon.ci.domain.deployment.Deployment;
import io.watermelon.ci.domain.deployment.DeploymentRepository;
import io.watermelon.ci.domain.deployment.DeploymentStatus;
import io.watermelon.ci.domain.deployment.RuntimeTarget;
import io.watermelon.ci.domain.pipeline.PipelineRun;
import io.watermelon.ci.domain.pipeline.PipelineRunRepository;
import io.watermelon.ci.runtime.model.AgentHeartbeatRequest;
import io.watermelon.ci.runtime.model.DeploymentView;
import io.watermelon.ci.runtime.spi.ContainerRuntimeClient;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeploymentTrackingService {

    private static final Logger log = LoggerFactory.getLogger(DeploymentTrackingService.class);

    private final DeploymentRepository deploymentRepository;
    private final PipelineRunRepository pipelineRunRepository;
    private final ContainerRuntimeClient runtimeClient;
    private final Clock clock;

    public DeploymentTrackingService(
            DeploymentRepository deploymentRepository,
            PipelineRunRepository pipelineRunRepository,
            ContainerRuntimeClient runtimeClient) {
        this.deploymentRepository = deploymentRepository;
        this.pipelineRunRepository = pipelineRunRepository;
        this.runtimeClient = runtimeClient;
        this.clock = Clock.system();
    }

    @Transactional
    public DeploymentView registerFromHeartbeat(UUID projectId, AgentHeartbeatRequest request) {
        UUID pipelineRunId = request.pipelineRunId() != null ? UUID.fromString(request.pipelineRunId()) : null;
        Deployment deployment = deploymentRepository.findByExternalId(request.externalId()).orElseGet(() ->
                new Deployment(
                        Ids.newId(),
                        projectId,
                        pipelineRunId,
                        request.environment(),
                        RuntimeTarget.DOCKER,
                        request.imageRef(),
                        request.releaseName(),
                        request.status() != null ? request.status() : DeploymentStatus.DEPLOYING,
                        clock.now()));
        deployment.bindExternal(request.externalId());
        deployment.observe(
                request.status() != null ? request.status() : DeploymentStatus.HEALTHY,
                request.message(),
                clock.now());
        if (deployment.getPipelineRunId() == null && pipelineRunId != null) {
            // keep existing binding when already set
        }
        deploymentRepository.save(deployment);
        return toView(deployment);
    }

    @Transactional
    public DeploymentView upsertTracked(
            UUID projectId,
            UUID pipelineRunId,
            String environment,
            RuntimeTarget runtime,
            String imageRef,
            String releaseName,
            String externalId) {
        Deployment deployment = new Deployment(
                Ids.newId(),
                projectId,
                pipelineRunId,
                environment,
                runtime,
                imageRef,
                releaseName,
                DeploymentStatus.DEPLOYING,
                clock.now());
        if (externalId != null) {
            deployment.bindExternal(externalId);
        }
        deploymentRepository.save(deployment);
        return toView(deployment);
    }

    @Transactional(readOnly = true)
    public List<DeploymentView> listByProject(UUID projectId) {
        return deploymentRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<DeploymentView> listByEnvironment(UUID projectId, String environment) {
        return deploymentRepository
                .findByProjectIdAndEnvironmentOrderByCreatedAtDesc(projectId, environment)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeploymentView get(UUID deploymentId) {
        return deploymentRepository
                .findById(deploymentId)
                .map(this::toView)
                .orElseThrow(() -> new NotFoundException("deployment not found: " + deploymentId));
    }

    @Scheduled(fixedDelayString = "${watermelon.runtime.poll-interval-seconds:30}000")
    @Transactional
    public void reconcile() {
        List<ContainerRuntimeClient.ObservedContainer> live = runtimeClient.listWatermelonContainers();
        for (ContainerRuntimeClient.ObservedContainer container : live) {
            deploymentRepository
                    .findByExternalId(container.externalId())
                    .ifPresentOrElse(
                            existing -> {
                                existing.observe(container.mappedStatus(), "reconciled state=" + container.state(), clock.now());
                                deploymentRepository.save(existing);
                            },
                            () -> {
                                UUID projectId = resolveProjectId(container);
                                if (projectId == null) {
                                    return;
                                }
                                UUID runId = null;
                                if (container.pipelineRunId() != null) {
                                    try {
                                        runId = UUID.fromString(container.pipelineRunId());
                                    } catch (IllegalArgumentException ignored) {
                                        runId = null;
                                    }
                                }
                                Deployment created = new Deployment(
                                        Ids.newId(),
                                        projectId,
                                        runId,
                                        container.environment() != null ? container.environment() : "unknown",
                                        RuntimeTarget.DOCKER,
                                        container.image(),
                                        container.name(),
                                        container.mappedStatus(),
                                        clock.now());
                                created.bindExternal(container.externalId());
                                created.observe(container.mappedStatus(), "discovered by reconciler", clock.now());
                                deploymentRepository.save(created);
                                log.info("Discovered unmanaged watermelon container {}", container.externalId());
                            });
        }

        for (Deployment deployment : deploymentRepository.findByStatusIn(
                List.of(DeploymentStatus.HEALTHY, DeploymentStatus.DEPLOYING, DeploymentStatus.DEGRADED))) {
            if (deployment.getExternalId() == null) {
                continue;
            }
            runtimeClient
                    .inspect(deployment.getExternalId())
                    .ifPresentOrElse(
                            observed -> {
                                deployment.observe(observed.mappedStatus(), "inspect=" + observed.state(), clock.now());
                                deploymentRepository.save(deployment);
                            },
                            () -> {
                                deployment.observe(DeploymentStatus.STOPPED, "container missing on host", clock.now());
                                deploymentRepository.save(deployment);
                            });
        }
    }

    private UUID resolveProjectId(ContainerRuntimeClient.ObservedContainer container) {
        if (container.pipelineRunId() == null) {
            return null;
        }
        try {
            UUID runId = UUID.fromString(container.pipelineRunId());
            return pipelineRunRepository.findById(runId).map(PipelineRun::getProjectId).orElse(null);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private DeploymentView toView(Deployment d) {
        return new DeploymentView(
                d.getId(),
                d.getProjectId(),
                d.getPipelineRunId(),
                d.getEnvironment(),
                d.getRuntime(),
                d.getImageRef(),
                d.getReleaseName(),
                d.getExternalId(),
                d.getStatus(),
                d.getStatusMessage(),
                d.getCreatedAt(),
                d.getLastSeenAt());
    }
}
