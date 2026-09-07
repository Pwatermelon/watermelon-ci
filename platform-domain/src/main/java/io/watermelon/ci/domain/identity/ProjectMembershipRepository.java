
package io.watermelon.ci.domain.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMembershipRepository extends JpaRepository<ProjectMembership, UUID> {
    List<ProjectMembership> findByProjectId(UUID projectId);
    Optional<ProjectMembership> findByProjectIdAndSubject(UUID projectId, String subject);
}
