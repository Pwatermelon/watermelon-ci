import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, Cluster, Pipeline, Project } from '../lib/api'
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
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!orgId) return
    Promise.all([
      projectId ? api.getProject(projectId).catch(() => null) : Promise.resolve(null),
      projectId ? api.listPipelines(projectId).catch(() => []) : Promise.resolve([]),
      api.fleetOverview(orgId).catch(() => ({ clusters: 0, nodes: 0, onlineNodes: 0, containers: 0, runningContainers: 0 })),
      api.listClusters(orgId).catch(() => []),
    ])
      .then(([p, pipes, f, c]) => {
        setProject(p)
        setPipelines(pipes.slice(0, 8))
        setFleet(f)
        setClusters(c)
      })
      .catch((e) => setError(String(e.message || e)))
  }, [orgId, projectId])

  return (
    <>
      <div className="topbar">
        <div>
          <h1>Обзор</h1>
          <p>{project?.name || 'Workspace'} · пайплайны, хосты, сервисы</p>
        </div>
      </div>
      {error && <div className="error">{error}</div>}
      <div className="grid stats">
        {[
          ['Проект', project?.slug || '—'],
          ['Runs', pipelines.length],
          ['Хосты online', fleet.onlineNodes],
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
          <h2>Последние пайплайны</h2>
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
          <h2>Кластеры</h2>
          {clusters.length === 0 ? (
            <div className="empty">Нет кластеров. Добавьте хост в <Link to="/app/hosts">Хостах</Link>.</div>
          ) : (
            <table className="table">
              <thead><tr><th>Имя</th><th>Ноды</th><th>Контейнеры</th></tr></thead>
              <tbody>
                {clusters.map((c) => (
                  <tr key={c.id}>
                    <td>{c.name}</td>
                    <td>{c.onlineNodes}/{c.nodeCount}</td>
                    <td>{c.containerCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </>
  )
}
