const ORG_KEY = 'wm.organizationId'
const PROJECT_KEY = 'wm.projectId'

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

export function getStoredOrgId() { return localStorage.getItem(ORG_KEY) }
export function setStoredOrgId(id: string) { localStorage.setItem(ORG_KEY, id) }
export function getStoredProjectId() { return localStorage.getItem(PROJECT_KEY) }
export function setStoredProjectId(id: string) { localStorage.setItem(PROJECT_KEY, id) }

export type Org = { id: string; slug: string; name: string }
export type Project = { id: string; organizationId: string; slug: string; name: string; description?: string }
export type Pipeline = {
  id: string; projectId: string; number: number; ref: string; commitSha?: string
  status: string; jenkinsJobName?: string; createdAt: string; finishedAt?: string
  compiledJenkinsfile?: string; manifestYaml?: string
}
export type Issue = { id: string; number: number; title: string; body?: string; status: string }
export type SecretMeta = { id: string; environment: string; name: string; vaultPath: string }
export type Artifact = { id: string; kind: string; name: string; version: string; locator: string }
export type GitOpsApp = {
  id: string; environment: string; releaseName: string; argoAppName: string
  syncStatus: string; healthStatus: string; imageRef?: string
}
export type Cluster = {
  id: string; name: string; kind: string; nodeCount: number; onlineNodes: number; containerCount: number
}
export type Container = {
  nodeId: string; nodeName: string; id: string; shortId: string; name: string
  image: string; state: string; statusText?: string; projectSlug?: string; environment?: string
  ports?: string[]
}
export type WorkerNode = {
  id: string; clusterId: string; name: string; role: string; status: string
  engineUrl?: string; hostname?: string; reachable?: boolean
}

export const api = {
  seedDemo: () => req<Record<string, string>>('/api/v1/demo/seed', { method: 'POST' }),
  listOrgs: () => req<Org[]>('/api/v1/organizations'),
  createOrg: (name: string) => req<Org>('/api/v1/organizations', { method: 'POST', body: JSON.stringify({ name }) }),
  listProjects: (orgId: string) => req<Project[]>(`/api/v1/organizations/${orgId}/projects`),
  createProject: (orgId: string, name: string, description?: string) =>
    req<Project>(`/api/v1/organizations/${orgId}/projects`, {
      method: 'POST', body: JSON.stringify({ name, description }),
    }),
  getProject: (id: string) => req<Project>(`/api/v1/projects/${id}`),
  listPipelines: (projectId: string) => req<Pipeline[]>(`/api/v1/projects/${projectId}/pipelines`),
  startPipeline: (projectId: string, ref: string, manifestYaml: string, commitSha?: string) =>
    req<Pipeline>(`/api/v1/projects/${projectId}/pipelines`, {
      method: 'POST', body: JSON.stringify({ ref, commitSha, manifestYaml }),
    }),
  previewManifest: (projectId: string, manifestYaml: string) =>
    req<{ jenkinsfile: string }>(`/api/v1/projects/${projectId}/manifests/preview`, {
      method: 'POST', body: JSON.stringify({ manifestYaml }),
    }),
  templates: () => req<Record<string, string>>('/api/v1/manifest-templates'),
  listIssues: (projectId: string) => req<Issue[]>(`/api/v1/projects/${projectId}/issues`),
  createIssue: (projectId: string, title: string, body?: string) =>
    req<Issue>(`/api/v1/projects/${projectId}/issues`, {
      method: 'POST', body: JSON.stringify({ title, body }),
    }),
  listSecrets: (projectId: string, environment?: string) =>
    req<{ secrets: SecretMeta[]; vaultPath: string }>(
      `/api/v1/projects/${projectId}/secrets${environment ? `?environment=${environment}` : ''}`,
    ),
  upsertSecret: (projectId: string, environment: string, name: string, value: string) =>
    req<SecretMeta>(`/api/v1/projects/${projectId}/secrets`, {
      method: 'PUT', body: JSON.stringify({ environment, name, value }),
    }),
  listArtifacts: (projectId: string) => req<Artifact[]>(`/api/v1/projects/${projectId}/artifacts`),
  listGitOps: (projectId: string) => req<GitOpsApp[]>(`/api/v1/projects/${projectId}/gitops/applications`),
  gitOpsDeploy: (projectId: string, body: Record<string, unknown>) =>
    req<GitOpsApp>(`/api/v1/projects/${projectId}/gitops/deploy`, {
      method: 'POST', body: JSON.stringify(body),
    }),
  listClusters: (orgId: string) => req<Cluster[]>(`/api/v1/fleet/organizations/${orgId}/clusters`),
  fleetOverview: (orgId: string) => req<{
    clusters: number; nodes: number; onlineNodes: number; containers: number; runningContainers: number
  }>(`/api/v1/fleet/organizations/${orgId}/overview`),
  listContainers: (clusterId: string) =>
    req<Container[]>(`/api/v1/fleet/clusters/${clusterId}/containers?all=true`),
  listNodes: (clusterId: string) =>
    req<WorkerNode[]>(`/api/v1/fleet/clusters/${clusterId}/nodes`),
  containerAction: (clusterId: string, nodeId: string, containerId: string, action: string) =>
    req(`/api/v1/fleet/clusters/${clusterId}/nodes/${nodeId}/containers/${containerId}/actions`, {
      method: 'POST', body: JSON.stringify({ action }),
    }),
  containerLogs: (clusterId: string, nodeId: string, containerId: string, tail = 200) =>
    req<{ containerId: string; logs: string }>(
      `/api/v1/fleet/clusters/${clusterId}/nodes/${nodeId}/containers/${containerId}/logs?tail=${tail}`,
    ),
  createCluster: (organizationId: string, name: string, kind = 'BARE_DOCKER') =>
    req<Cluster>('/api/v1/fleet/clusters', {
      method: 'POST', body: JSON.stringify({ organizationId, name, kind, description: '' }),
    }),
  addNode: (clusterId: string, name: string, engineUrl: string, role = 'WORKER') =>
    req<{ id: string; joinToken: string; installHint?: string }>('/api/v1/fleet/clusters/' + clusterId + '/nodes', {
      method: 'POST', body: JSON.stringify({ name, role, engineUrl }),
    }),
  karbyzRun: (source: string, mode: 'run' | 'as-yaml' = 'run', stdin?: string) =>
    req<{ ok: boolean; exitCode: number; stdout: string; mode?: string }>('/api/v1/karbyz/run', {
      method: 'POST', body: JSON.stringify({ source, mode, stdin }),
    }),
  karbyzCompile: (source: string) =>
    req<{ ok: boolean; jobName?: string; jenkinsfile?: string; error?: string }>('/api/v1/karbyz/compile', {
      method: 'POST', body: JSON.stringify({ source }),
    }),
}
