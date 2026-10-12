import { authHeaders, getAccessToken, isAuthEnabled } from './auth'

const ORG_KEY = 'wm.organizationId'
const PROJECT_KEY = 'wm.projectId'
const GIT_OWNER_KEY = 'wm.gitOwner'

async function req<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(),
      ...(init?.headers || {}),
    },
    ...init,
  })
  if (res.status === 401 && isAuthEnabled() && !getAccessToken()) {
    throw new Error('Требуется вход через Keycloak')
  }
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || res.statusText)
  }
  if (res.status === 204) return undefined as T
  return res.json()
}

export function getStoredOrgId() { return localStorage.getItem(ORG_KEY) }
export function setStoredOrgId(id: string) {
  if (!id) localStorage.removeItem(ORG_KEY)
  else localStorage.setItem(ORG_KEY, id)
}
export function getStoredProjectId() { return localStorage.getItem(PROJECT_KEY) }
export function setStoredProjectId(id: string) {
  if (!id) localStorage.removeItem(PROJECT_KEY)
  else localStorage.setItem(PROJECT_KEY, id)
}
export function getGitOwner() { return localStorage.getItem(GIT_OWNER_KEY) || 'watermelon-demo' }
export function setGitOwner(o: string) { localStorage.setItem(GIT_OWNER_KEY, o) }

export type Org = { id: string; slug: string; name: string }
export type Project = { id: string; organizationId: string; slug: string; name: string; description?: string }
export type Pipeline = {
  id: string; projectId: string; number: number; ref: string; commitSha?: string
  status: string; jenkinsJobName?: string; createdAt: string; finishedAt?: string
  compiledJenkinsfile?: string; manifestYaml?: string
}
export type Cluster = {
  id: string; name: string; kind: string; nodeCount: number; onlineNodes: number; containerCount: number
}
export type Container = {
  nodeId: string; nodeName: string; id: string; shortId: string; name: string
  image: string; state: string; statusText?: string; projectSlug?: string; environment?: string
}
export type WorkerNode = {
  id: string; clusterId: string; name: string; role: string; status: string
  engineUrl?: string; hostname?: string; reachable?: boolean
}
export type RemoteRepo = { owner: string; name: string; cloneUrl: string; webUrl: string; defaultBranch: string }
export type RemoteFile = { path: string; type: string; size: number; sha?: string }
export type Deployment = {
  id: string; projectId: string; environment: string; runtime: string
  imageRef?: string; releaseName: string; status: string; statusMessage?: string
  createdAt: string; lastSeenAt?: string
}

export const api = {
  seedDemo: () => req<Record<string, string>>('/api/v1/demo/seed', { method: 'POST' }),
  listOrgs: () => req<Org[]>('/api/v1/organizations'),
  createOrg: (name: string) => req<Org>('/api/v1/organizations', { method: 'POST', body: JSON.stringify({ name }) }),
  listProjects: (orgId: string) => req<Project[]>(`/api/v1/organizations/${orgId}/projects`),
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
  listDeployments: (projectId: string) => req<Deployment[]>(`/api/v1/projects/${projectId}/deployments`),
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
    req<{ id: string; joinToken: string; installHint?: string }>(`/api/v1/fleet/clusters/${clusterId}/nodes`, {
      method: 'POST', body: JSON.stringify({ name, role, engineUrl }),
    }),
  listRepos: (owner: string) => req<RemoteRepo[]>(`/api/v1/editor/orgs/${owner}/repos`),
  ensureRepo: (owner: string, repo: string) =>
    req<RemoteRepo>(`/api/v1/editor/orgs/${owner}/repos/${repo}`, {
      method: 'POST', body: JSON.stringify({ description: repo }),
    }),
  listTree: (owner: string, repo: string, path = '', ref = 'main') =>
    req<RemoteFile[]>(`/api/v1/editor/repos/${owner}/${repo}/tree?path=${encodeURIComponent(path)}&ref=${ref}`),
  readFile: (owner: string, repo: string, path: string, ref = 'main') =>
    req<{ path: string; ref: string; content: string }>(
      `/api/v1/editor/repos/${owner}/${repo}/file?path=${encodeURIComponent(path)}&ref=${ref}`,
    ),
  writeFile: (owner: string, repo: string, body: {
    path: string; content: string; message?: string; ref?: string; sha?: string
  }) =>
    req<{ path: string; sha: string; commitMessage: string }>(
      `/api/v1/editor/repos/${owner}/${repo}/file`,
      { method: 'PUT', body: JSON.stringify(body) },
    ),
  karbyzRun: (source: string, mode: 'run' | 'as-yaml' = 'run', stdin?: string) =>
    req<{ ok: boolean; exitCode: number; stdout: string }>('/api/v1/karbyz/run', {
      method: 'POST', body: JSON.stringify({ source, mode, stdin }),
    }),
  karbyzCompile: (source: string) =>
    req<{ ok: boolean; jobName?: string; jenkinsfile?: string; error?: string }>('/api/v1/karbyz/compile', {
      method: 'POST', body: JSON.stringify({ source }),
    }),
}
