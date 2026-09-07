
package io.watermelon.ci.domain.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessGroupRepository extends JpaRepository<AccessGroup, UUID> {
    List<AccessGroup> findByOrganizationId(UUID organizationId);
    Optional<AccessGroup> findByOrganizationIdAndSlug(UUID organizationId, String slug);
}
