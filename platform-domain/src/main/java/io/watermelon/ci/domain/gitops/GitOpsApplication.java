package io.watermelon.ci.domain.gitops;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gitops_applications")
public class GitOpsApplication {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 64)
    private String environment;

    @Column(nullable = false, length = 200)
    private String releaseName;

    @Column(nullable = false, length = 200)
    private String argoAppName;

    @Column(nullable = false, length = 100)
    private String destinationNamespace;

    @Column(nullable = false, length = 500)
    private String repoPath;

    @Column(length = 500)
    private String imageRef;

    @Column(nullable = false, length = 32)
    private String syncStatus;

    @Column(nullable = false, length = 32)
    private String healthStatus;

    @Lob
    private String lastRenderedYaml;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected GitOpsApplication() {}

    public GitOpsApplication(
            UUID id,
            UUID projectId,
            String environment,
            String releaseName,
            String argoAppName,
            String destinationNamespace,
            String repoPath,
            String imageRef,
            Instant createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.environment = environment;
        this.releaseName = releaseName;
        this.argoAppName = argoAppName;
        this.destinationNamespace = destinationNamespace;
        this.repoPath = repoPath;
        this.imageRef = imageRef;
        this.syncStatus = "Unknown";
        this.healthStatus = "Unknown";
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public String getEnvironment() { return environment; }
    public String getReleaseName() { return releaseName; }
    public String getArgoAppName() { return argoAppName; }
    public String getDestinationNamespace() { return destinationNamespace; }
    public String getRepoPath() { return repoPath; }
    public String getImageRef() { return imageRef; }
    public String getSyncStatus() { return syncStatus; }
    public String getHealthStatus() { return healthStatus; }
    public String getLastRenderedYaml() { return lastRenderedYaml; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void markRendered(String imageRef, String yaml, Instant now) {
        this.imageRef = imageRef;
        this.lastRenderedYaml = yaml;
        this.updatedAt = now;
    }

    public void markSync(String syncStatus, String healthStatus, Instant now) {
        this.syncStatus = syncStatus;
        this.healthStatus = healthStatus;
        this.updatedAt = now;
    }
}
