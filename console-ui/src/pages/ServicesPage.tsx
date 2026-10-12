import { useEffect, useState } from 'react'
import { api, Cluster, Container, Deployment } from '../lib/api'
import { useWorkspace } from './LandingPage'

function badge(status: string) {
  const s = (status || '').toLowerCase()
  const cls = ['success', 'healthy', 'running', 'online'].some((x) => s.includes(x))
    ? 'ok'
    : ['deploy', 'pending', 'warn'].some((x) => s.includes(x))
      ? 'warn'
      : ['fail', 'error', 'degrad', 'exited'].some((x) => s.includes(x))
        ? 'danger'
        : 'muted'
  return <span className={`badge ${cls}`}>{status}</span>
}

export default function ServicesPage() {
  const { orgId, projectId } = useWorkspace()
  const [deployments, setDeployments] = useState<Deployment[]>([])
  const [clusters, setClusters] = useState<Cluster[]>([])
  const [clusterId, setClusterId] = useState<string | null>(null)
  const [containers, setContainers] = useState<Container[]>([])
  const [logs, setLogs] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!orgId) return
    Promise.all([
      projectId ? api.listDeployments(projectId).catch(() => []) : Promise.resolve([]),
      api.listClusters(orgId).catch(() => []),
    ]).then(([d, c]) => {
      setDeployments(d)
      setClusters(c)
      if (c[0]) setClusterId(c[0].id)
    }).catch((e) => setError(String(e.message || e)))
  }, [orgId, projectId])

  useEffect(() => {
    if (!clusterId) return
    api.listContainers(clusterId).then(setContainers).catch((e) => setError(String(e.message || e)))
  }, [clusterId])

  const act = async (c: Container, action: string) => {
    if (!clusterId) return
    setBusy(true); setError(null)
    try {
      await api.containerAction(clusterId, c.nodeId, c.id, action)
      setContainers(await api.listContainers(clusterId))
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div className="topbar">
        <div>
          <h1>Сервисы</h1>
          <p>Статусы deployments и контейнеры на зарегистрированных хостах</p>
        </div>
      </div>
      {error && <div className="error">{error}</div>}

      <div className="panel">
        <h2>Deployments</h2>
        {deployments.length === 0 ? (
          <div className="empty">Нет отслеживаемых deployments для проекта.</div>
        ) : (
          <table className="table">
            <thead><tr><th>Release</th><th>Env</th><th>Статус</th><th>Image</th><th>Seen</th></tr></thead>
            <tbody>
              {deployments.map((d) => (
                <tr key={d.id}>
                  <td className="mono">{d.releaseName}</td>
                  <td>{d.environment}</td>
                  <td>{badge(d.status)}</td>
                  <td className="mono muted">{d.imageRef || '—'}</td>
                  <td className="muted">{d.lastSeenAt ? new Date(d.lastSeenAt).toLocaleString() : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="panel">
        <div className="btn-row" style={{ justifyContent: 'space-between', marginBottom: 8 }}>
          <h2 style={{ margin: 0 }}>Контейнеры</h2>
          <select
            value={clusterId || ''}
            onChange={(e) => setClusterId(e.target.value || null)}
          >
            {clusters.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
        {containers.length === 0 ? (
          <div className="empty">Нет контейнеров (нужен online-хост с Docker API).</div>
        ) : (
          <table className="table">
            <thead><tr><th>Имя</th><th>Image</th><th>State</th><th>Node</th><th></th></tr></thead>
            <tbody>
              {containers.map((c) => (
                <tr key={`${c.nodeId}-${c.id}`}>
                  <td>
                    <div>{c.name}</div>
                    <div className="mono muted">{c.shortId}</div>
                  </td>
                  <td className="mono muted">{c.image}</td>
                  <td>{badge(c.state)}</td>
                  <td className="mono">{c.nodeName}</td>
                  <td>
                    <div className="btn-row">
                      <button className="btn btn-sm" type="button" disabled={busy} onClick={() => act(c, 'start')}>Start</button>
                      <button className="btn btn-sm" type="button" disabled={busy} onClick={() => act(c, 'stop')}>Stop</button>
                      <button className="btn btn-sm" type="button" disabled={busy} onClick={() => act(c, 'restart')}>Restart</button>
                      <button
                        className="btn btn-sm"
                        type="button"
                        disabled={busy || !clusterId}
                        onClick={async () => {
                          setBusy(true)
                          try {
                            const r = await api.containerLogs(clusterId!, c.nodeId, c.id)
                            setLogs(`# ${c.name}\n${r.logs || '(empty)'}`)
                          } catch (e) {
                            setError(e instanceof Error ? e.message : String(e))
                          } finally {
                            setBusy(false)
                          }
                        }}
                      >Logs</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {logs && (
          <div style={{ marginTop: 12 }}>
            <div className="btn-row" style={{ justifyContent: 'space-between' }}>
              <h2 style={{ margin: 0 }}>Logs</h2>
              <button className="btn ghost" type="button" onClick={() => setLogs(null)}>Закрыть</button>
            </div>
            <div className="codebox">{logs}</div>
          </div>
        )}
      </div>
    </>
  )
}
