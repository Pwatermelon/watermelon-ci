import { Link, NavLink, Route, Routes, useNavigate, useParams } from 'react-router-dom'
import { useEffect, useMemo, useState } from 'react'
import {
  api,
  ClusterKind,
  ClusterView,
  ContainerView,
  FleetOverview,
  getStoredOrgId,
  NodeCreatedView,
  NodeRole,
  NodeView,
  setStoredOrgId,
} from './lib/api'

function statusBadge(status: string) {
  const s = status.toLowerCase()
  const cls = s.includes('run') || s === 'online' || s === 'healthy'
    ? 'ok'
    : s.includes('pend') || s.includes('drain') || s.includes('restart')
      ? 'warn'
      : s.includes('off') || s.includes('exit') || s.includes('error') || s.includes('dead')
        ? 'danger'
        : 'muted'
  return <span className={`badge ${cls}`}>{status}</span>
}

function SetupGate({ children }: { children: (orgId: string) => React.ReactNode }) {
  const [orgId, setOrgId] = useState(getStoredOrgId())
  const [name, setName] = useState('Watermelon Demo')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (orgId) return <>{children(orgId)}</>

  return (
    <div className="main" style={{ maxWidth: 520, margin: '10vh auto' }}>
      <div className="topbar">
        <div>
          <h1>Watermelon Fleet</h1>
          <p>Создайте организацию — дальше админка кластеров, нод и контейнеров внутри CI.</p>
        </div>
      </div>
      <div className="panel form">
        <label>
          Название организации
          <input value={name} onChange={(e) => setName(e.target.value)} />
        </label>
        {error && <div className="error">{error}</div>}
        <button
          className="btn primary"
          disabled={busy || !name.trim()}
          onClick={async () => {
            setBusy(true)
            setError(null)
            try {
              const org = await api.createOrganization(name.trim())
              setStoredOrgId(org.id)
              setOrgId(org.id)
            } catch (e) {
              setError(e instanceof Error ? e.message : String(e))
            } finally {
              setBusy(false)
            }
          }}
        >
          Создать и открыть консоль
        </button>
      </div>
    </div>
  )
}

function Shell({ orgId, children }: { orgId: string; children: React.ReactNode }) {
  return (
    <div className="shell">
      <aside className="side">
        <div className="brand">
          <strong>Watermelon</strong>
          <span>fleet console</span>
        </div>
        <nav className="nav">
          <NavLink to="/" end>Overview</NavLink>
          <NavLink to="/clusters">Clusters</NavLink>
        </nav>
        <div className="muted mono" style={{ marginTop: 'auto', fontSize: 11 }}>
          org {orgId.slice(0, 8)}…
        </div>
      </aside>
      <div className="main">{children}</div>
    </div>
  )
}

function OverviewPage({ orgId }: { orgId: string }) {
  const [data, setData] = useState<FleetOverview | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.overview(orgId).then(setData).catch((e) => setError(String(e.message || e)))
  }, [orgId])

  return (
    <>
      <div className="topbar">
        <div>
          <h1>Fleet overview</h1>
          <p>Кластеры, воркеры и контейнеры — управление без ухода в внешние вебморды.</p>
        </div>
      </div>
      {error && <div className="error">{error}</div>}
      {data && (
        <>
          <div className="grid stats">
            <div className="stat"><div className="label">Clusters</div><div className="value">{data.clusters}</div></div>
            <div className="stat"><div className="label">Nodes</div><div className="value">{data.nodes}</div></div>
            <div className="stat"><div className="label">Online</div><div className="value">{data.onlineNodes}</div></div>
            <div className="stat"><div className="label">Running</div><div className="value">{data.runningContainers}</div></div>
          </div>
          <div className="panel">
            <h2>Recent clusters</h2>
            <table className="table">
              <thead>
                <tr>
                  <th>Name</th><th>Kind</th><th>Nodes</th><th>Containers</th>
                </tr>
              </thead>
              <tbody>
                {data.recentClusters.map((c) => (
                  <tr key={c.id}>
                    <td><Link to={`/clusters/${c.id}`}>{c.name}</Link></td>
                    <td className="mono">{c.kind}</td>
                    <td>{c.onlineNodes}/{c.nodeCount}</td>
                    <td>{c.containerCount}</td>
                  </tr>
                ))}
                {data.recentClusters.length === 0 && (
                  <tr><td colSpan={4} className="muted">Пока нет кластеров — создайте на вкладке Clusters.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      )}
    </>
  )
}

function ClustersPage({ orgId }: { orgId: string }) {
  const navigate = useNavigate()
  const [clusters, setClusters] = useState<ClusterView[]>([])
  const [name, setName] = useState('')
  const [kind, setKind] = useState<ClusterKind>('BARE_DOCKER')
  const [desc, setDesc] = useState('')
  const [error, setError] = useState<string | null>(null)

  const reload = () => api.listClusters(orgId).then(setClusters).catch((e) => setError(String(e.message || e)))
  useEffect(() => { reload() }, [orgId])

  return (
    <>
      <div className="topbar">
        <div>
          <h1>Clusters</h1>
          <p>Создавайте runtime-кластеры и добавляйте worker nodes прямо здесь.</p>
        </div>
      </div>
      {error && <div className="error">{error}</div>}
      <div className="grid two">
        <div className="panel">
          <h2>Список</h2>
          <div className="grid cards">
            {clusters.map((c) => (
              <button key={c.id} className="panel" style={{ textAlign: 'left', cursor: 'pointer' }} onClick={() => navigate(`/clusters/${c.id}`)}>
                <strong>{c.name}</strong>
                <div className="muted mono" style={{ marginTop: 6 }}>{c.kind}</div>
                <div style={{ marginTop: 10 }} className="muted">
                  {c.onlineNodes}/{c.nodeCount} online · {c.containerCount} containers
                </div>
              </button>
            ))}
          </div>
        </div>
        <div className="panel form">
          <h2>Новый cluster</h2>
          <label>Name<input value={name} onChange={(e) => setName(e.target.value)} placeholder="production-fleet" /></label>
          <label>Kind
            <select value={kind} onChange={(e) => setKind(e.target.value as ClusterKind)}>
              <option value="BARE_DOCKER">Bare Docker</option>
              <option value="DOCKER_SWARM_LIKE">Docker fleet</option>
              <option value="KUBERNETES">Kubernetes</option>
            </select>
          </label>
          <label>Description<textarea value={desc} onChange={(e) => setDesc(e.target.value)} /></label>
          <button
            className="btn primary"
            disabled={!name.trim()}
            onClick={async () => {
              setError(null)
              try {
                const c = await api.createCluster(orgId, name.trim(), kind, desc || undefined)
                setName(''); setDesc('')
                await reload()
                navigate(`/clusters/${c.id}`)
              } catch (e) {
                setError(e instanceof Error ? e.message : String(e))
              }
            }}
          >Создать cluster</button>
        </div>
      </div>
    </>
  )
}

function ClusterDetailPage({ orgId }: { orgId: string }) {
  const { clusterId = '' } = useParams()
  const [cluster, setCluster] = useState<ClusterView | null>(null)
  const [nodes, setNodes] = useState<NodeView[]>([])
  const [containers, setContainers] = useState<ContainerView[]>([])
  const [created, setCreated] = useState<NodeCreatedView | null>(null)
  const [nodeName, setNodeName] = useState('worker-1')
  const [role, setRole] = useState<NodeRole>('WORKER')
  const [engineUrl, setEngineUrl] = useState('http://host.docker.internal:2375')
  const [logs, setLogs] = useState<{ title: string; body: string } | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)

  const reload = async () => {
    const [c, n, cont] = await Promise.all([
      api.getCluster(clusterId),
      api.listNodes(clusterId),
      api.listContainers(clusterId),
    ])
    setCluster(c)
    setNodes(n)
    setContainers(cont)
  }

  useEffect(() => {
    reload().catch((e) => setError(String(e.message || e)))
    const t = setInterval(() => { reload().catch(() => undefined) }, 8000)
    return () => clearInterval(t)
  }, [clusterId])

  const running = useMemo(() => containers.filter((c) => c.state === 'running').length, [containers])

  return (
    <>
      <div className="topbar">
        <div>
          <h1>{cluster?.name || 'Cluster'}</h1>
          <p className="mono">{cluster?.kind} · {nodes.length} nodes · {running}/{containers.length} running</p>
        </div>
        <button className="btn" onClick={() => reload().catch((e) => setError(String(e.message || e)))}>Refresh</button>
      </div>
      {error && <div className="error">{error}</div>}

      <div className="grid two">
        <div className="panel">
          <h2>Worker nodes</h2>
          <table className="table">
            <thead>
              <tr>
                <th>Name</th><th>Role</th><th>Status</th><th>Reachable</th><th></th>
              </tr>
            </thead>
            <tbody>
              {nodes.map((n) => (
                <tr key={n.id}>
                  <td>
                    <div>{n.name}</div>
                    <div className="muted mono">{n.hostname || n.engineUrl}</div>
                  </td>
                  <td className="mono">{n.role}</td>
                  <td>{statusBadge(n.status)}</td>
                  <td>{n.reachable ? statusBadge('online') : statusBadge('offline')}</td>
                  <td className="btn-row">
                    {n.status !== 'DRAINING' ? (
                      <button className="btn" onClick={() => api.drainNode(clusterId, n.id).then(reload)}>Drain</button>
                    ) : (
                      <button className="btn" onClick={() => api.activateNode(clusterId, n.id).then(reload)}>Activate</button>
                    )}
                  </td>
                </tr>
              ))}
              {nodes.length === 0 && <tr><td colSpan={5} className="muted">Нод ещё нет</td></tr>}
            </tbody>
          </table>
        </div>

        <div className="panel form">
          <h2>Добавить worker node</h2>
          <label>Name<input value={nodeName} onChange={(e) => setNodeName(e.target.value)} /></label>
          <label>Role
            <select value={role} onChange={(e) => setRole(e.target.value as NodeRole)}>
              <option value="WORKER">WORKER</option>
              <option value="CONTROL">CONTROL</option>
            </select>
          </label>
          <label>Docker Engine URL<input value={engineUrl} onChange={(e) => setEngineUrl(e.target.value)} /></label>
          <button
            className="btn primary"
            onClick={async () => {
              setError(null)
              try {
                const n = await api.addNode(clusterId, nodeName.trim(), role, engineUrl.trim())
                setCreated(n)
                await reload()
              } catch (e) {
                setError(e instanceof Error ? e.message : String(e))
              }
            }}
          >Выписать join token</button>
          {created && (
            <div className="panel" style={{ marginTop: 8 }}>
              <div className="muted">Join token (показывается один раз)</div>
              <div className="mono" style={{ wordBreak: 'break-all', marginTop: 6 }}>{created.joinToken}</div>
              <div className="muted" style={{ marginTop: 10 }}>Install hint</div>
              <div className="mono" style={{ marginTop: 6, whiteSpace: 'pre-wrap' }}>{created.installHint}</div>
            </div>
          )}
        </div>
      </div>

      <div className="panel">
        <h2>Containers — start / stop / restart / logs / remove</h2>
        <table className="table">
          <thead>
            <tr>
              <th>Name</th><th>Node</th><th>Image</th><th>State</th><th>CI links</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {containers.map((c) => (
              <tr key={c.id}>
                <td>
                  <div>{c.name}</div>
                  <div className="muted mono">{c.shortId}</div>
                </td>
                <td className="mono">{c.nodeName}</td>
                <td className="mono">{c.image}</td>
                <td>{statusBadge(c.state)}</td>
                <td className="mono muted">
                  {[c.projectSlug, c.environment].filter(Boolean).join(' / ') || '—'}
                </td>
                <td>
                  <div className="btn-row">
                    {(['start', 'stop', 'restart'] as const).map((action) => (
                      <button
                        key={action}
                        className="btn"
                        disabled={busyId === c.id}
                        onClick={async () => {
                          setBusyId(c.id)
                          try {
                            await api.containerAction(clusterId, c.nodeId, c.id, action)
                            await reload()
                          } catch (e) {
                            setError(e instanceof Error ? e.message : String(e))
                          } finally {
                            setBusyId(null)
                          }
                        }}
                      >{action}</button>
                    ))}
                    <button
                      className="btn"
                      onClick={async () => {
                        const res = await api.logs(clusterId, c.nodeId, c.id)
                        setLogs({ title: `${c.name} logs`, body: res.logs || '(empty)' })
                      }}
                    >logs</button>
                    <button
                      className="btn danger"
                      disabled={busyId === c.id}
                      onClick={async () => {
                        if (!confirm(`Remove ${c.name}?`)) return
                        setBusyId(c.id)
                        try {
                          await api.containerAction(clusterId, c.nodeId, c.id, 'remove')
                          await reload()
                        } catch (e) {
                          setError(e instanceof Error ? e.message : String(e))
                        } finally {
                          setBusyId(null)
                        }
                      }}
                    >remove</button>
                  </div>
                </td>
              </tr>
            ))}
            {containers.length === 0 && (
              <tr><td colSpan={6} className="muted">Контейнеров нет или ноды offline</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {logs && (
        <div className="drawer" onClick={() => setLogs(null)}>
          <div className="drawer-panel" onClick={(e) => e.stopPropagation()}>
            <div className="topbar">
              <div>
                <h1 style={{ fontSize: '1.1rem' }}>{logs.title}</h1>
                <p>Логи прямо в Watermelon — без kubectl/portainer.</p>
              </div>
              <button className="btn" onClick={() => setLogs(null)}>Close</button>
            </div>
            <div className="logs">{logs.body}</div>
          </div>
        </div>
      )}
      <div className="muted mono" style={{ fontSize: 11 }}>org context {orgId.slice(0, 8)}</div>
    </>
  )
}

export default function App() {
  return (
    <SetupGate>
      {(orgId) => (
        <Shell orgId={orgId}>
          <Routes>
            <Route path="/" element={<OverviewPage orgId={orgId} />} />
            <Route path="/clusters" element={<ClustersPage orgId={orgId} />} />
            <Route path="/clusters/:clusterId" element={<ClusterDetailPage orgId={orgId} />} />
          </Routes>
        </Shell>
      )}
    </SetupGate>
  )
}
