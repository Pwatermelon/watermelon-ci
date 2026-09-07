
package io.watermelon.ci.domain.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByOrganizationId(UUID organizationId);
    Optional<Project> findByOrganizationIdAndSlug(UUID organizationId, String slug);
}
