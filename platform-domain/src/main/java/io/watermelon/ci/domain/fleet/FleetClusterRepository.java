package io.watermelon.ci.domain.fleet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FleetClusterRepository extends JpaRepository<FleetCluster, UUID> {
    List<FleetCluster> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    Optional<FleetCluster> findByOrganizationIdAndSlug(UUID organizationId, String slug);
}
