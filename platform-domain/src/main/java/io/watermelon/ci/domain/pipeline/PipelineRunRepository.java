
package io.watermelon.ci.domain.pipeline;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PipelineRunRepository extends JpaRepository<PipelineRun, UUID> {
    List<PipelineRun> findByProjectIdOrderByNumberDesc(UUID projectId);

    Optional<PipelineRun> findByProjectIdAndNumber(UUID projectId, long number);

    @Query("select coalesce(max(p.number), 0) from PipelineRun p where p.projectId = :projectId")
    long findMaxNumber(UUID projectId);
}
