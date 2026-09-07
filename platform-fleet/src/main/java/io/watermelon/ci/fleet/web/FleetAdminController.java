package io.watermelon.ci.fleet.web;

import io.watermelon.ci.domain.fleet.NodeStatus;
import io.watermelon.ci.fleet.model.FleetDtos.ClusterView;
import io.watermelon.ci.fleet.model.FleetDtos.ContainerActionRequest;
import io.watermelon.ci.fleet.model.FleetDtos.ContainerView;
import io.watermelon.ci.fleet.model.FleetDtos.CreateClusterRequest;
import io.watermelon.ci.fleet.model.FleetDtos.CreateNodeRequest;
import io.watermelon.ci.fleet.model.FleetDtos.FleetOverview;
import io.watermelon.ci.fleet.model.FleetDtos.LogsView;
import io.watermelon.ci.fleet.model.FleetDtos.NodeCreatedView;
import io.watermelon.ci.fleet.model.FleetDtos.NodeHeartbeatRequest;
import io.watermelon.ci.fleet.model.FleetDtos.NodeInfoView;
import io.watermelon.ci.fleet.model.FleetDtos.NodeJoinRequest;
import io.watermelon.ci.fleet.model.FleetDtos.NodeView;
import io.watermelon.ci.fleet.service.FleetAdminService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * In-platform fleet admin API — Kubernetes-dashboard-like control without leaving Watermelon CI.
 */
@RestController
@RequestMapping("/api/v1/fleet")
public class FleetAdminController {

    private final FleetAdminService fleetAdminService;

    public FleetAdminController(FleetAdminService fleetAdminService) {
        this.fleetAdminService = fleetAdminService;
    }

    @GetMapping("/organizations/{organizationId}/overview")
    public FleetOverview overview(@PathVariable UUID organizationId) {
        return fleetAdminService.overview(organizationId);
    }

    @PostMapping("/clusters")
    @ResponseStatus(HttpStatus.CREATED)
    public ClusterView createCluster(@Valid @RequestBody CreateClusterRequest request) {
        return fleetAdminService.createCluster(
                request.organizationId(), request.name(), request.kind(), request.description());
    }

    @GetMapping("/organizations/{organizationId}/clusters")
    public List<ClusterView> listClusters(@PathVariable UUID organizationId) {
        return fleetAdminService.listClusters(organizationId);
    }

    @GetMapping("/clusters/{clusterId}")
    public ClusterView getCluster(@PathVariable UUID clusterId) {
        return fleetAdminService.getCluster(clusterId);
    }

    @PostMapping("/clusters/{clusterId}/nodes")
    @ResponseStatus(HttpStatus.CREATED)
    public NodeCreatedView addNode(@PathVariable UUID clusterId, @Valid @RequestBody CreateNodeRequest request) {
        return fleetAdminService.addWorkerNode(clusterId, request.name(), request.role(), request.engineUrl());
    }

    @GetMapping("/clusters/{clusterId}/nodes")
    public List<NodeView> listNodes(@PathVariable UUID clusterId) {
        return fleetAdminService.listNodes(clusterId);
    }

    @PostMapping("/clusters/{clusterId}/nodes/{nodeId}/join")
    public NodeView join(
            @PathVariable UUID clusterId, @PathVariable UUID nodeId, @Valid @RequestBody NodeJoinRequest request) {
        return fleetAdminService.completeJoin(
                clusterId,
                nodeId,
                request.joinToken(),
                request.engineUrl(),
                request.hostname(),
                request.architecture(),
                request.cpuCores(),
                request.memoryBytes(),
                request.agentVersion());
    }

    @PostMapping("/clusters/{clusterId}/nodes/{nodeId}/heartbeat")
    public NodeView heartbeat(
            @PathVariable UUID clusterId,
            @PathVariable UUID nodeId,
            @RequestBody(required = false) NodeHeartbeatRequest request) {
        NodeHeartbeatRequest body = request != null ? request : new NodeHeartbeatRequest(null, null, null);
        return fleetAdminService.heartbeat(clusterId, nodeId, body.hostname(), body.cpuCores(), body.memoryBytes());
    }

    @PostMapping("/clusters/{clusterId}/nodes/{nodeId}/drain")
    public NodeView drain(@PathVariable UUID clusterId, @PathVariable UUID nodeId) {
        return fleetAdminService.setNodeStatus(clusterId, nodeId, NodeStatus.DRAINING);
    }

    @PostMapping("/clusters/{clusterId}/nodes/{nodeId}/activate")
    public NodeView activate(@PathVariable UUID clusterId, @PathVariable UUID nodeId) {
        return fleetAdminService.setNodeStatus(clusterId, nodeId, NodeStatus.ONLINE);
    }

    @GetMapping("/clusters/{clusterId}/nodes/{nodeId}/info")
    public NodeInfoView nodeInfo(@PathVariable UUID clusterId, @PathVariable UUID nodeId) {
        return fleetAdminService.nodeInfo(clusterId, nodeId);
    }

    @GetMapping("/clusters/{clusterId}/containers")
    public List<ContainerView> containers(
            @PathVariable UUID clusterId,
            @RequestParam(required = false) UUID nodeId,
            @RequestParam(defaultValue = "true") boolean all) {
        return fleetAdminService.listContainers(clusterId, nodeId, all);
    }

    @GetMapping("/clusters/{clusterId}/nodes/{nodeId}/containers/{containerId}")
    public ContainerView inspect(
            @PathVariable UUID clusterId, @PathVariable UUID nodeId, @PathVariable String containerId) {
        return fleetAdminService.inspectContainer(clusterId, nodeId, containerId);
    }

    @PostMapping("/clusters/{clusterId}/nodes/{nodeId}/containers/{containerId}/actions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> action(
            @PathVariable UUID clusterId,
            @PathVariable UUID nodeId,
            @PathVariable String containerId,
            @Valid @RequestBody ContainerActionRequest request) {
        fleetAdminService.containerAction(clusterId, nodeId, containerId, request.action());
        return Map.of("status", "accepted", "action", request.action());
    }

    @GetMapping("/clusters/{clusterId}/nodes/{nodeId}/containers/{containerId}/logs")
    public LogsView logs(
            @PathVariable UUID clusterId,
            @PathVariable UUID nodeId,
            @PathVariable String containerId,
            @RequestParam(defaultValue = "200") int tail) {
        return fleetAdminService.containerLogs(clusterId, nodeId, containerId, tail);
    }

    @GetMapping("/clusters/{clusterId}/nodes/{nodeId}/containers/{containerId}/stats")
    public Map<String, Object> stats(
            @PathVariable UUID clusterId, @PathVariable UUID nodeId, @PathVariable String containerId) {
        return fleetAdminService.containerStats(clusterId, nodeId, containerId);
    }
}
