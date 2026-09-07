
package io.watermelon.ci.domain.deployment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deployments")
public class Deployment {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "pipeline_run_id")
    private UUID pipelineRunId;

    @Column(nullable = false, length = 64)
    private String environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RuntimeTarget runtime;

    @Column(nullable = false, length = 500)
    private String imageRef;

    @Column(length = 200)
    private String releaseName;

    @Column(length = 200)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeploymentStatus status;

    @Column(length = 2000)
    private String statusMessage;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant lastSeenAt;

    @Column(name = "cluster_id")
    private UUID clusterId;

    @Column(name = "node_id")
    private UUID nodeId;

    protected Deployment() {}

    public Deployment(
            UUID id,
            UUID projectId,
            UUID pipelineRunId,
            String environment,
            RuntimeTarget runtime,
            String imageRef,
            String releaseName,
            DeploymentStatus status,
            Instant createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.pipelineRunId = pipelineRunId;
        this.environment = environment;
        this.runtime = runtime;
        this.imageRef = imageRef;
        this.releaseName = releaseName;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public UUID getPipelineRunId() { return pipelineRunId; }
    public String getEnvironment() { return environment; }
    public RuntimeTarget getRuntime() { return runtime; }
    public String getImageRef() { return imageRef; }
    public String getReleaseName() { return releaseName; }
    public String getExternalId() { return externalId; }
    public DeploymentStatus getStatus() { return status; }
    public String getStatusMessage() { return statusMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public UUID getClusterId() { return clusterId; }
    public UUID getNodeId() { return nodeId; }

    public void bindExternal(String externalId) {
        this.externalId = externalId;
    }

    public void bindFleet(UUID clusterId, UUID nodeId) {
        this.clusterId = clusterId;
        this.nodeId = nodeId;
    }

    public void observe(DeploymentStatus status, String message, Instant seenAt) {
        this.status = status;
        this.statusMessage = message;
        this.lastSeenAt = seenAt;
    }
}
