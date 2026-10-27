import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, Cluster, Pipeline, Project, WorkerNode } from '../lib/api'
import { useWorkspace } from './LandingPage'

function badge(status: string) {
  const s = (status || '').toLowerCase()
  const cls = ['success', 'healthy', 'online', 'running'].some((x) => s.includes(x))
    ? 'ok'
    : ['queued', 'pending', 'warn'].some((x) => s.includes(x))
      ? 'warn'
      : ['fail', 'error', 'off'].some((x) => s.includes(x))
        ? 'danger'
        : 'muted'
  return <span className={`badge ${cls}`}>{status}</span>
}

export default function DashboardPage() {
  const { orgId, projectId } = useWorkspace()
  const [project, setProject] = useState<Project | null>(null)
  const [pipelines, setPipelines] = useState<Pipeline[]>([])
  const [fleet, setFleet] = useState({ clusters: 0, onlineNodes: 0, runningContainers: 0 })
  const [clusters, setClusters] = useState<Cluster[]>([])
  const [nodes, setNodes] = useState<WorkerNode[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!orgId) return
    Promise.all([
      projectId ? api.getProject(projectId).catch(() => null) : Promise.resolve(null),
      projectId ? api.listPipelines(projectId).catch(() => []) : Promise.resolve([]),
      api.fleetOverview(orgId).catch(() => ({ clusters: 0, nodes: 0, onlineNodes: 0, containers: 0, runningContainers: 0 })),
      api.listClusters(orgId).catch(() => []),
    ])
      .then(async ([p, pipes, f, c]) => {
        setProject(p)
        setPipelines(pipes.slice(0, 8))
        setFleet(f)
        setClusters(c)
        if (c[0]) {
          const n = await api.listNodes(c[0].id).catch(() => [])
          setNodes(n)
        }
      })
      .catch((e) => setError(String(e.message || e)))
  }, [orgId, projectId])

  return (
    <>
      <div className="topbar">
        <div>
          <h1>Обзор демо</h1>
          <p>{project?.name || 'Workspace'} · Git → Карбыз/YAML → Registry → Docker VM</p>
        </div>
      </div>
      {error && <div className="error">{error}</div>}

      <div className="panel rise">
        <h2>Как пользоваться</h2>
        <ol className="demo-steps">
          <li><Link to="/app/repos">Репозитории</Link> — storefront и payments с Dockerfile и watermelon-ci.kbz</li>
          <li><Link to="/app/pipelines">Пайплайны</Link> — запуск сборки и раскатки на VM</li>
          <li><Link to="/app/hosts">Хосты</Link> — demo-vm-a / demo-vm-b (DinD)</li>
          <li><Link to="/app/services">Сервисы</Link> — статус контейнеров на этих VM</li>
          <li><Link to="/app/registry">Registry</Link> — собранные образы и теги</li>
        </ol>
      </div>

      <div className="grid stats">
        {[
          ['Проект', project?.slug || '—'],
          ['Runs', pipelines.length],
          ['VM online', fleet.onlineNodes],
          ['Контейнеры', fleet.runningContainers],
        ].map(([label, value]) => (
          <div className="stat" key={String(label)}>
            <div className="label">{label}</div>
            <div className="value">{value}</div>
          </div>
        ))}
      </div>

      <div className="grid two">
        <div className="panel">
          <h2>Пайплайны</h2>
          {pipelines.length === 0 ? (
            <div className="empty">Нет запусков. Откройте <Link to="/app/pipelines">Пайплайны</Link>.</div>
          ) : (
            <table className="table">
              <thead><tr><th>#</th><th>Ref</th><th>Статус</th></tr></thead>
              <tbody>
                {pipelines.map((p) => (
                  <tr key={p.id}>
                    <td className="mono">{p.number}</td>
                    <td className="mono">{p.ref}</td>
                    <td>{badge(p.status)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
        <div className="panel">
          <h2>Виртуалки</h2>
          {nodes.length === 0 ? (
            <div className="empty">Нет нод. Проверьте seed / <Link to="/app/hosts">Хосты</Link>.</div>
          ) : (
            <table className="table">
              <thead><tr><th>Имя</th><th>Engine</th><th>Статус</th></tr></thead>
              <tbody>
                {nodes.map((n) => (
                  <tr key={n.id}>
                    <td>{n.name}</td>
                    <td className="mono muted">{n.engineUrl || '—'}</td>
                    <td>{badge(n.reachable ? 'ONLINE' : n.status)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {clusters[0] && (
            <p className="muted" style={{ marginBottom: 0 }}>
              Кластер {clusters[0].name}: {clusters[0].onlineNodes}/{clusters[0].nodeCount} online · {clusters[0].containerCount} контейнеров
            </p>
          )}
        </div>
      </div>
    </>
  )
}
