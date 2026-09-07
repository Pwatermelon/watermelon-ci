
package io.watermelon.ci.domain.artifact;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtifactRecordRepository extends JpaRepository<ArtifactRecord, UUID> {
    List<ArtifactRecord> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    List<ArtifactRecord> findByPipelineRunId(UUID pipelineRunId);
    List<ArtifactRecord> findByProjectIdAndKindOrderByCreatedAtDesc(UUID projectId, ArtifactKind kind);
}
