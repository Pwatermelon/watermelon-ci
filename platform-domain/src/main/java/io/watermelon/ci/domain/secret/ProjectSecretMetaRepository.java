package io.watermelon.ci.domain.secret;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectSecretMetaRepository extends JpaRepository<ProjectSecretMeta, UUID> {
    List<ProjectSecretMeta> findByProjectIdOrderByEnvironmentAscNameAsc(UUID projectId);
    List<ProjectSecretMeta> findByProjectIdAndEnvironmentOrderByNameAsc(UUID projectId, String environment);
    Optional<ProjectSecretMeta> findByProjectIdAndEnvironmentAndName(UUID projectId, String environment, String name);
}
