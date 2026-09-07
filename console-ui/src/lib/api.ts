export type ClusterKind = 'DOCKER_SWARM_LIKE' | 'BARE_DOCKER' | 'KUBERNETES'
export type NodeRole = 'CONTROL' | 'WORKER'
export type NodeStatus = 'PENDING' | 'ONLINE' | 'DRAINING' | 'OFFLINE' | 'ERROR'

export interface ClusterView {
  id: string
  organizationId: string
  slug: string
  name: string
  kind: ClusterKind
  description?: string
  createdAt: string
  nodeCount: number
  onlineNodes: number
  containerCount: number
}

export interface NodeView {
  id: string
  clusterId: string
  name: string
  role: NodeRole
  status: NodeStatus
  engineUrl: string
  hostname?: string
  architecture?: string
  cpuCores?: number
  memoryBytes?: number
  agentVersion?: string
  createdAt: string
  lastHeartbeatAt?: string
  reachable: boolean
}

export interface NodeCreatedView extends Omit<NodeView, 'reachable' | 'hostname' | 'architecture' | 'cpuCores' | 'memoryBytes' | 'agentVersion' | 'createdAt' | 'lastHeartbeatAt'> {
  joinToken: string
  installHint: string
}

export interface ContainerView {
  nodeId: string
  nodeName: string
  id: string
  shortId: string
  name: string
  image: string
  state: string
  statusText: string
  projectSlug?: string
  environment?: string
  pipelineRunId?: string
  ports: string[]
}

export interface FleetOverview {
  clusters: number
  nodes: number
  onlineNodes: number
  containers: number
  runningContainers: number
  recentClusters: ClusterView[]
}

export interface OrganizationResponse {
  id: string
  slug: string
  name: string
}

const ORG_KEY = 'wm.organizationId'

async function req<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    headers: { 'Content-Type': 'application/json', ...(init?.headers || {}) },
    ...init,
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || res.statusText)
  }
  if (res.status === 204) return undefined as T
  return res.json()
}

export function getStoredOrgId(): string | null {
  return localStorage.getItem(ORG_KEY)
}

export function setStoredOrgId(id: string) {
  localStorage.setItem(ORG_KEY, id)
}

export const api = {
  createOrganization: (name: string) =>
    req<OrganizationResponse>('/api/v1/organizations', {
      method: 'POST',
      body: JSON.stringify({ name }),
    }),

  overview: (orgId: string) =>
    req<FleetOverview>(`/api/v1/fleet/organizations/${orgId}/overview`),

  listClusters: (orgId: string) =>
    req<ClusterView[]>(`/api/v1/fleet/organizations/${orgId}/clusters`),

  createCluster: (organizationId: string, name: string, kind: ClusterKind, description?: string) =>
    req<ClusterView>('/api/v1/fleet/clusters', {
      method: 'POST',
      body: JSON.stringify({ organizationId, name, kind, description }),
    }),

  getCluster: (clusterId: string) =>
    req<ClusterView>(`/api/v1/fleet/clusters/${clusterId}`),

  listNodes: (clusterId: string) =>
    req<NodeView[]>(`/api/v1/fleet/clusters/${clusterId}/nodes`),

  addNode: (clusterId: string, name: string, role: NodeRole, engineUrl: string) =>
    req<NodeCreatedView>(`/api/v1/fleet/clusters/${clusterId}/nodes`, {
      method: 'POST',
      body: JSON.stringify({ name, role, engineUrl }),
    }),

  drainNode: (clusterId: string, nodeId: string) =>
    req<NodeView>(`/api/v1/fleet/clusters/${clusterId}/nodes/${nodeId}/drain`, { method: 'POST' }),

  activateNode: (clusterId: string, nodeId: string) =>
    req<NodeView>(`/api/v1/fleet/clusters/${clusterId}/nodes/${nodeId}/activate`, { method: 'POST' }),

  listContainers: (clusterId: string, nodeId?: string) => {
    const q = nodeId ? `?nodeId=${nodeId}&all=true` : '?all=true'
    return req<ContainerView[]>(`/api/v1/fleet/clusters/${clusterId}/containers${q}`)
  },

  containerAction: (clusterId: string, nodeId: string, containerId: string, action: string) =>
    req<{ status: string }>(
      `/api/v1/fleet/clusters/${clusterId}/nodes/${nodeId}/containers/${containerId}/actions`,
      { method: 'POST', body: JSON.stringify({ action }) },
    ),

  logs: (clusterId: string, nodeId: string, containerId: string, tail = 300) =>
    req<{ containerId: string; logs: string }>(
      `/api/v1/fleet/clusters/${clusterId}/nodes/${nodeId}/containers/${containerId}/logs?tail=${tail}`,
    ),
}
