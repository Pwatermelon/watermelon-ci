package io.watermelon.ci.domain.secret;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/** Metadata only — values live in Vault/OpenBao. */
@Entity
@Table(name = "project_secrets", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "environment", "name"}))
public class ProjectSecretMeta {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 64)
    private String environment;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 500)
    private String vaultPath;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected ProjectSecretMeta() {}

    public ProjectSecretMeta(
            UUID id, UUID projectId, String environment, String name, String vaultPath, String description, Instant createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.environment = environment;
        this.name = name;
        this.vaultPath = vaultPath;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public String getEnvironment() { return environment; }
    public String getName() { return name; }
    public String getVaultPath() { return vaultPath; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void touch(Instant now) {
        this.updatedAt = now;
    }
}
