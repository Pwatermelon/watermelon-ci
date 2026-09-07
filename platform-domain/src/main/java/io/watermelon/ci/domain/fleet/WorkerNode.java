package io.watermelon.ci.domain.fleet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "worker_nodes")
public class WorkerNode {

    @Id
    private UUID id;

    @Column(name = "cluster_id", nullable = false)
    private UUID clusterId;

    @Column(nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NodeRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NodeStatus status;

    /** Docker Engine / kubelet proxy endpoint reachable from control plane */
    @Column(nullable = false, length = 500)
    private String engineUrl;

    @Column(length = 200)
    private String hostname;

    @Column(length = 64)
    private String architecture;

    @Column
    private Integer cpuCores;

    @Column
    private Long memoryBytes;

    @Column(length = 128)
    private String agentVersion;

    @Column(nullable = false, length = 128)
    private String joinTokenHash;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant lastHeartbeatAt;

    protected WorkerNode() {}

    public WorkerNode(
            UUID id,
            UUID clusterId,
            String name,
            NodeRole role,
            NodeStatus status,
            String engineUrl,
            String joinTokenHash,
            Instant createdAt) {
        this.id = id;
        this.clusterId = clusterId;
        this.name = name;
        this.role = role;
        this.status = status;
        this.engineUrl = engineUrl;
        this.joinTokenHash = joinTokenHash;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getClusterId() { return clusterId; }
    public String getName() { return name; }
    public NodeRole getRole() { return role; }
    public NodeStatus getStatus() { return status; }
    public String getEngineUrl() { return engineUrl; }
    public String getHostname() { return hostname; }
    public String getArchitecture() { return architecture; }
    public Integer getCpuCores() { return cpuCores; }
    public Long getMemoryBytes() { return memoryBytes; }
    public String getAgentVersion() { return agentVersion; }
    public String getJoinTokenHash() { return joinTokenHash; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastHeartbeatAt() { return lastHeartbeatAt; }

    public void markOnline(String hostname, String arch, Integer cpu, Long memory, String agentVersion, Instant now) {
        this.status = NodeStatus.ONLINE;
        this.hostname = hostname;
        this.architecture = arch;
        this.cpuCores = cpu;
        this.memoryBytes = memory;
        this.agentVersion = agentVersion;
        this.lastHeartbeatAt = now;
    }

    public void heartbeat(Instant now) {
        this.lastHeartbeatAt = now;
        if (this.status == NodeStatus.OFFLINE || this.status == NodeStatus.PENDING) {
            this.status = NodeStatus.ONLINE;
        }
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }

    public void setEngineUrl(String engineUrl) {
        this.engineUrl = engineUrl;
    }

    public void setName(String name) {
        this.name = name;
    }
}
