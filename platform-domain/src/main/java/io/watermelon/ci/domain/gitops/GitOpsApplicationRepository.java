package io.watermelon.ci.domain.gitops;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GitOpsApplicationRepository extends JpaRepository<GitOpsApplication, UUID> {
    List<GitOpsApplication> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);
    Optional<GitOpsApplication> findByProjectIdAndEnvironmentAndReleaseName(
            UUID projectId, String environment, String releaseName);
    Optional<GitOpsApplication> findByArgoAppName(String argoAppName);
}
