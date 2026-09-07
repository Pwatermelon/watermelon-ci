package io.watermelon.ci.fleet.model;

import io.watermelon.ci.domain.fleet.ClusterKind;
import io.watermelon.ci.domain.fleet.NodeRole;
import io.watermelon.ci.domain.fleet.NodeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FleetDtos {

    private FleetDtos() {}

    public record CreateClusterRequest(
            @NotNull UUID organizationId,
            @NotBlank String name,
            @NotNull ClusterKind kind,
            String description) {}

    public record ClusterView(
            UUID id,
            UUID organizationId,
            String slug,
            String name,
            ClusterKind kind,
            String description,
            Instant createdAt,
            int nodeCount,
            int onlineNodes,
            int containerCount) {}

    public record CreateNodeRequest(
            @NotBlank String name,
            @NotNull NodeRole role,
            @NotBlank String engineUrl) {}

    public record NodeCreatedView(
            UUID id,
            UUID clusterId,
            String name,
            NodeRole role,
            NodeStatus status,
            String engineUrl,
            String joinToken,
            String installHint) {}

    public record NodeView(
            UUID id,
            UUID clusterId,
            String name,
            NodeRole role,
            NodeStatus status,
            String engineUrl,
            String hostname,
            String architecture,
            Integer cpuCores,
            Long memoryBytes,
            String agentVersion,
            Instant createdAt,
            Instant lastHeartbeatAt,
            boolean reachable) {}

    public record NodeJoinRequest(
            @NotBlank String joinToken,
            @NotBlank String engineUrl,
            String hostname,
            String architecture,
            Integer cpuCores,
            Long memoryBytes,
            String agentVersion) {}

    public record NodeHeartbeatRequest(String hostname, Integer cpuCores, Long memoryBytes) {}

    public record ContainerView(
            UUID nodeId,
            String nodeName,
            String id,
            String shortId,
            String name,
            String image,
            String state,
            String statusText,
            String projectSlug,
            String environment,
            String pipelineRunId,
            List<String> ports) {}

    public record ContainerActionRequest(@NotBlank String action) {}

    public record LogsView(String containerId, String logs) {}

    public record FleetOverview(
            int clusters,
            int nodes,
            int onlineNodes,
            int containers,
            int runningContainers,
            List<ClusterView> recentClusters) {}

    public record NodeInfoView(UUID nodeId, Map<String, Object> dockerInfo) {}
}
