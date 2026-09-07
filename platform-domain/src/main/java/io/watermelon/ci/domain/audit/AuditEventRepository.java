
package io.watermelon.ci.domain.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
