package io.watermelon.ci.fleet.service;

import io.watermelon.ci.common.error.ErrorCode;
import io.watermelon.ci.common.error.NotFoundException;
import io.watermelon.ci.common.error.PlatformException;
import io.watermelon.ci.common.ids.Ids;
import io.watermelon.ci.common.time.Clock;
import io.watermelon.ci.common.util.Slugs;
import io.watermelon.ci.domain.fleet.ClusterKind;
import io.watermelon.ci.domain.fleet.FleetCluster;
import io.watermelon.ci.domain.fleet.FleetClusterRepository;
import io.watermelon.ci.domain.fleet.NodeRole;
import io.watermelon.ci.domain.fleet.NodeStatus;
import io.watermelon.ci.domain.fleet.WorkerNode;
import io.watermelon.ci.domain.fleet.WorkerNodeRepository;
import io.watermelon.ci.domain.organization.OrganizationRepository;
import io.watermelon.ci.fleet.docker.NodeDockerGateway;
import io.watermelon.ci.fleet.docker.NodeDockerGateway.ManagedContainer;
import io.watermelon.ci.fleet.model.FleetDtos.ClusterView;
import io.watermelon.ci.fleet.model.FleetDtos.ContainerView;
import io.watermelon.ci.fleet.model.FleetDtos.FleetOverview;
import io.watermelon.ci.fleet.model.FleetDtos.LogsView;
import io.watermelon.ci.fleet.model.FleetDtos.NodeCreatedView;
import io.watermelon.ci.fleet.model.FleetDtos.NodeInfoView;
import io.watermelon.ci.fleet.model.FleetDtos.NodeView;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FleetAdminService {

    private final FleetClusterRepository clusterRepository;
    private final WorkerNodeRepository nodeRepository;
    private final OrganizationRepository organizationRepository;
    private final NodeDockerGateway dockerGateway;
    private final Clock clock = Clock.system();
    private final SecureRandom secureRandom = new SecureRandom();

    public FleetAdminService(
            FleetClusterRepository clusterRepository,
            WorkerNodeRepository nodeRepository,
            OrganizationRepository organizationRepository,
            NodeDockerGateway dockerGateway) {
        this.clusterRepository = clusterRepository;
        this.nodeRepository = nodeRepository;
        this.organizationRepository = organizationRepository;
        this.dockerGateway = dockerGateway;
    }

    @Transactional
    public ClusterView createCluster(UUID organizationId, String name, ClusterKind kind, String description) {
        organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new NotFoundException("organization not found"));
        String slug = Slugs.of(name);
        FleetCluster cluster =
                new FleetCluster(Ids.newId(), organizationId, slug, name, kind, description, clock.now());
        clusterRepository.save(cluster);
        return toClusterView(cluster, List.of());
    }

    @Transactional(readOnly = true)
    public List<ClusterView> listClusters(UUID organizationId) {
        return clusterRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(c -> toClusterView(c, nodeRepository.findByClusterIdOrderByCreatedAtAsc(c.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ClusterView getCluster(UUID clusterId) {
        FleetCluster cluster = requireCluster(clusterId);
        return toClusterView(cluster, nodeRepository.findByClusterIdOrderByCreatedAtAsc(clusterId));
    }

    @Transactional
    public NodeCreatedView addWorkerNode(UUID clusterId, String name, NodeRole role, String engineUrl) {
        requireCluster(clusterId);
        String joinToken = "wmjoin_" + randomToken(24);
        WorkerNode node = new WorkerNode(
                Ids.newId(),
                clusterId,
                name,
                role,
                NodeStatus.PENDING,
                engineUrl,
                sha256(joinToken),
                clock.now());
        nodeRepository.save(node);
        String hint = "curl -fsSL https://watermelon.local/agent/install.sh | WM_JOIN_TOKEN="
                + joinToken
                + " WM_CLUSTER="
                + clusterId
                + " bash";
        return new NodeCreatedView(
                node.getId(),
                clusterId,
                node.getName(),
                node.getRole(),
                node.getStatus(),
                node.getEngineUrl(),
                joinToken,
                hint);
    }

    @Transactional
    public NodeView completeJoin(UUID clusterId, UUID nodeId, String joinToken, String engineUrl,
                                 String hostname, String arch, Integer cpu, Long memory, String agentVersion) {
        WorkerNode node = requireNode(clusterId, nodeId);
        if (!sha256(joinToken).equals(node.getJoinTokenHash())) {
            throw new PlatformException(ErrorCode.FORBIDDEN, "invalid join token");
        }
        if (engineUrl != null && !engineUrl.isBlank()) {
            node.setEngineUrl(engineUrl);
        }
        node.markOnline(hostname, arch, cpu, memory, agentVersion, clock.now());
        nodeRepository.save(node);
        return toNodeView(node, dockerGateway.ping(node.getEngineUrl()));
    }

    @Transactional
    public NodeView heartbeat(UUID clusterId, UUID nodeId, String hostname, Integer cpu, Long memory) {
        WorkerNode node = requireNode(clusterId, nodeId);
        if (hostname != null) {
            node.markOnline(
                    hostname,
                    node.getArchitecture(),
                    cpu != null ? cpu : node.getCpuCores(),
                    memory != null ? memory : node.getMemoryBytes(),
                    node.getAgentVersion(),
                    clock.now());
        } else {
            node.heartbeat(clock.now());
        }
        nodeRepository.save(node);
        return toNodeView(node, true);
    }

    @Transactional(readOnly = true)
    public List<NodeView> listNodes(UUID clusterId) {
        requireCluster(clusterId);
        return nodeRepository.findByClusterIdOrderByCreatedAtAsc(clusterId).stream()
                .map(n -> toNodeView(n, dockerGateway.ping(n.getEngineUrl())))
                .toList();
    }

    @Transactional
    public NodeView setNodeStatus(UUID clusterId, UUID nodeId, NodeStatus status) {
        WorkerNode node = requireNode(clusterId, nodeId);
        node.setStatus(status);
        nodeRepository.save(node);
        return toNodeView(node, dockerGateway.ping(node.getEngineUrl()));
    }

    @Transactional(readOnly = true)
    public List<ContainerView> listContainers(UUID clusterId, UUID nodeIdOrNull, boolean all) {
        requireCluster(clusterId);
        List<WorkerNode> nodes = nodeIdOrNull == null
                ? nodeRepository.findByClusterIdOrderByCreatedAtAsc(clusterId)
                : List.of(requireNode(clusterId, nodeIdOrNull));
        List<ContainerView> result = new ArrayList<>();
        for (WorkerNode node : nodes) {
            if (node.getStatus() == NodeStatus.OFFLINE) {
                continue;
            }
            try {
                for (ManagedContainer c : dockerGateway.listContainers(node.getEngineUrl(), all)) {
                    result.add(toContainerView(node, c));
                }
            } catch (PlatformException ex) {
                // node unreachable — skip for list, UI shows node status
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ContainerView inspectContainer(UUID clusterId, UUID nodeId, String containerId) {
        WorkerNode node = requireNode(clusterId, nodeId);
        return toContainerView(node, dockerGateway.inspect(node.getEngineUrl(), containerId));
    }

    @Transactional(readOnly = true)
    public LogsView containerLogs(UUID clusterId, UUID nodeId, String containerId, int tail) {
        WorkerNode node = requireNode(clusterId, nodeId);
        return new LogsView(containerId, dockerGateway.logs(node.getEngineUrl(), containerId, tail));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> containerStats(UUID clusterId, UUID nodeId, String containerId) {
        WorkerNode node = requireNode(clusterId, nodeId);
        return dockerGateway.stats(node.getEngineUrl(), containerId);
    }

    @Transactional(readOnly = true)
    public NodeInfoView nodeInfo(UUID clusterId, UUID nodeId) {
        WorkerNode node = requireNode(clusterId, nodeId);
        return new NodeInfoView(nodeId, dockerGateway.info(node.getEngineUrl()));
    }

    public void containerAction(UUID clusterId, UUID nodeId, String containerId, String action) {
        WorkerNode node = requireNode(clusterId, nodeId);
        String engine = node.getEngineUrl();
        switch (action.toLowerCase(Locale.ROOT)) {
            case "start" -> dockerGateway.start(engine, containerId);
            case "stop" -> dockerGateway.stop(engine, containerId);
            case "restart" -> dockerGateway.restart(engine, containerId);
            case "remove", "delete" -> dockerGateway.remove(engine, containerId, true);
            default -> throw new PlatformException(ErrorCode.VALIDATION_FAILED, "unknown action: " + action);
        }
    }

    @Transactional(readOnly = true)
    public FleetOverview overview(UUID organizationId) {
        List<FleetCluster> clusters = clusterRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
        int nodes = 0;
        int online = 0;
        int containers = 0;
        int running = 0;
        List<ClusterView> views = new ArrayList<>();
        for (FleetCluster cluster : clusters) {
            List<WorkerNode> clusterNodes = nodeRepository.findByClusterIdOrderByCreatedAtAsc(cluster.getId());
            nodes += clusterNodes.size();
            online += (int) clusterNodes.stream().filter(n -> n.getStatus() == NodeStatus.ONLINE).count();
            ClusterView view = toClusterView(cluster, clusterNodes);
            containers += view.containerCount();
            views.add(view);
            for (WorkerNode node : clusterNodes) {
                if (node.getStatus() != NodeStatus.ONLINE) {
                    continue;
                }
                try {
                    running += (int) dockerGateway.listContainers(node.getEngineUrl(), false).stream()
                            .filter(c -> "running".equalsIgnoreCase(c.state()))
                            .count();
                } catch (PlatformException ignored) {
                    // ignore
                }
            }
        }
        return new FleetOverview(clusters.size(), nodes, online, containers, running, views.stream().limit(8).toList());
    }

    private FleetCluster requireCluster(UUID clusterId) {
        return clusterRepository.findById(clusterId).orElseThrow(() -> new NotFoundException("cluster not found"));
    }

    private WorkerNode requireNode(UUID clusterId, UUID nodeId) {
        WorkerNode node = nodeRepository.findById(nodeId).orElseThrow(() -> new NotFoundException("node not found"));
        if (!node.getClusterId().equals(clusterId)) {
            throw new NotFoundException("node not found in cluster");
        }
        return node;
    }

    private ClusterView toClusterView(FleetCluster cluster, List<WorkerNode> nodes) {
        int online = (int) nodes.stream().filter(n -> n.getStatus() == NodeStatus.ONLINE).count();
        int containerCount = 0;
        for (WorkerNode node : nodes) {
            if (node.getStatus() != NodeStatus.ONLINE && node.getStatus() != NodeStatus.DRAINING) {
                continue;
            }
            try {
                containerCount += dockerGateway.listContainers(node.getEngineUrl(), true).size();
            } catch (PlatformException ignored) {
                // unreachable
            }
        }
        return new ClusterView(
                cluster.getId(),
                cluster.getOrganizationId(),
                cluster.getSlug(),
                cluster.getName(),
                cluster.getKind(),
                cluster.getDescription(),
                cluster.getCreatedAt(),
                nodes.size(),
                online,
                containerCount);
    }

    private NodeView toNodeView(WorkerNode node, boolean reachable) {
        return new NodeView(
                node.getId(),
                node.getClusterId(),
                node.getName(),
                node.getRole(),
                node.getStatus(),
                node.getEngineUrl(),
                node.getHostname(),
                node.getArchitecture(),
                node.getCpuCores(),
                node.getMemoryBytes(),
                node.getAgentVersion(),
                node.getCreatedAt(),
                node.getLastHeartbeatAt(),
                reachable);
    }

    private ContainerView toContainerView(WorkerNode node, ManagedContainer c) {
        return new ContainerView(
                node.getId(),
                node.getName(),
                c.id(),
                c.shortId(),
                c.name(),
                c.image(),
                c.state(),
                c.statusText(),
                c.projectSlug(),
                c.environment(),
                c.pipelineRunId(),
                c.ports());
    }

    private String randomToken(int bytes) {
        byte[] buf = new byte[bytes];
        secureRandom.nextBytes(buf);
        return HexFormat.of().formatHex(buf);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
