
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
@Table(name = "access_groups", uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "slug"}))
public class AccessGroup {

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
    private PlatformRole defaultRole;

    @Column(nullable = false)
    private Instant createdAt;

    protected AccessGroup() {}

    public AccessGroup(UUID id, UUID organizationId, String slug, String name, PlatformRole defaultRole, Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.slug = slug;
        this.name = name;
        this.defaultRole = defaultRole;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public PlatformRole getDefaultRole() { return defaultRole; }
    public Instant getCreatedAt() { return createdAt; }
}
