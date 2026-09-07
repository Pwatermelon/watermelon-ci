package io.watermelon.ci.domain.fleet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerNodeRepository extends JpaRepository<WorkerNode, UUID> {
    List<WorkerNode> findByClusterIdOrderByCreatedAtAsc(UUID clusterId);
    Optional<WorkerNode> findByClusterIdAndName(UUID clusterId, String name);
    List<WorkerNode> findByStatus(NodeStatus status);
}
