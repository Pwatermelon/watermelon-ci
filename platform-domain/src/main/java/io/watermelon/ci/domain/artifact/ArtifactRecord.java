
package io.watermelon.ci.domain.artifact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "artifacts")
public class ArtifactRecord {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "pipeline_run_id")
    private UUID pipelineRunId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ArtifactKind kind;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(nullable = false, length = 200)
    private String version;

    @Column(nullable = false, length = 1000)
    private String locator;

    @Column(length = 128)
    private String digest;

    @Column(nullable = false)
    private Instant createdAt;

    protected ArtifactRecord() {}

    public ArtifactRecord(
            UUID id,
            UUID projectId,
            UUID pipelineRunId,
            ArtifactKind kind,
            String name,
            String version,
            String locator,
            String digest,
            Instant createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.pipelineRunId = pipelineRunId;
        this.kind = kind;
        this.name = name;
        this.version = version;
        this.locator = locator;
        this.digest = digest;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public UUID getPipelineRunId() { return pipelineRunId; }
    public ArtifactKind getKind() { return kind; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getLocator() { return locator; }
    public String getDigest() { return digest; }
    public Instant getCreatedAt() { return createdAt; }
}
