package io.watermelon.ci.domain.fleet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fleet_clusters")
public class FleetCluster {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 64)
    private String slug;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ClusterKind kind;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    protected FleetCluster() {}

    public FleetCluster(UUID id, UUID organizationId, String slug, String name, ClusterKind kind, String description, Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.slug = slug;
        this.name = name;
        this.kind = kind;
        this.description = description;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public ClusterKind getKind() { return kind; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}
