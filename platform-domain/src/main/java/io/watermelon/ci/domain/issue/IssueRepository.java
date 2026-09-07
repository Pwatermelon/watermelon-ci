
package io.watermelon.ci.domain.issue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IssueRepository extends JpaRepository<Issue, UUID> {
    List<Issue> findByProjectIdOrderByNumberDesc(UUID projectId);
    Optional<Issue> findByProjectIdAndNumber(UUID projectId, long number);

    @Query("select coalesce(max(i.number), 0) from Issue i where i.projectId = :projectId")
    long findMaxNumber(UUID projectId);
}
