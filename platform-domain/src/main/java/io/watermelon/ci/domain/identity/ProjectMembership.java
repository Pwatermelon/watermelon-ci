
package io.watermelon.ci.domain.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "subject"}))
public class ProjectMembership {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    /** User subject (OIDC sub) or group:<uuid> */
    @Column(nullable = false, length = 200)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PlatformRole role;

    @Column(nullable = false)
    private Instant createdAt;

    protected ProjectMembership() {}

    public ProjectMembership(UUID id, UUID projectId, String subject, PlatformRole role, Instant createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.subject = subject;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public String getSubject() { return subject; }
    public PlatformRole getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
}
