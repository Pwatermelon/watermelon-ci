
package io.watermelon.ci.domain.deployment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentRepository extends JpaRepository<Deployment, UUID> {
    List<Deployment> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    List<Deployment> findByProjectIdAndEnvironmentOrderByCreatedAtDesc(UUID projectId, String environment);
    Optional<Deployment> findByExternalId(String externalId);
    List<Deployment> findByStatusIn(List<DeploymentStatus> statuses);
}
