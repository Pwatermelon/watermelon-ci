import { useEffect, useState } from 'react'
import { api, Cluster, WorkerNode } from '../lib/api'
import { useWorkspace } from './LandingPage'

function badge(status: string) {
  const s = (status || '').toLowerCase()
  const cls = ['online', 'healthy', 'success'].some((x) => s.includes(x))
    ? 'ok'
    : ['drain', 'warn'].some((x) => s.includes(x))
      ? 'warn'
      : ['off', 'fail', 'error'].some((x) => s.includes(x))
        ? 'danger'
        : 'muted'
  return <span className={`badge ${cls}`}>{status}</span>
}

export default function HostsPage() {
  const { orgId } = useWorkspace()
  const [clusters, setClusters] = useState<Cluster[]>([])
  const [selected, setSelected] = useState<string | null>(null)
  const [nodes, setNodes] = useState<WorkerNode[]>([])
  const [clusterName, setClusterName] = useState('production-fleet')
  const [nodeForm, setNodeForm] = useState({ name: 'worker-1', engineUrl: 'unix:///var/run/docker.sock' })
  const [hint, setHint] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const reloadClusters = async () => {
    const list = await api.listClusters(orgId)
    setClusters(list)
    if (!selected && list[0]) setSelected(list[0].id)
  }

  useEffect(() => {
    if (!orgId) return
    reloadClusters().catch((e) => setError(String(e.message || e)))
  }, [orgId])

  useEffect(() => {
    if (!selected) return
    api.listNodes(selected).then(setNodes).catch((e) => setError(String(e.message || e)))
  }, [selected])

  return (
    <>
      <div className="topbar">
        <div>
          <h1>Хосты</h1>
          <p>Кластеры и регистрация Docker Engine узлов (remote engine URL). Не полный Swarm orchestrator.</p>
        </div>
      </div>
      {error && <div className="error">{error}</div>}
      <div className="grid two">
        <div className="panel">
          <h2>Кластеры</h2>
          {clusters.length === 0 ? (
            <div className="empty">Создайте кластер справа.</div>
          ) : (
            <table className="table">
              <thead><tr><th>Имя</th><th>Kind</th><th>Ноды</th></tr></thead>
              <tbody>
                {clusters.map((c) => (
                  <tr
                    key={c.id}
                    style={{ cursor: 'pointer', background: selected === c.id ? 'rgba(62,207,122,0.08)' : undefined }}
                    onClick={() => { setSelected(c.id); setHint(null) }}
                  >
                    <td>{c.name}</td>
                    <td className="mono">{c.kind}</td>
                    <td>{c.onlineNodes}/{c.nodeCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <div className="form" style={{ marginTop: 16 }}>
            <h2>Новый кластер</h2>
            <label>Имя<input value={clusterName} onChange={(e) => setClusterName(e.target.value)} /></label>
            <button
              className="btn primary"
              type="button"
              disabled={busy || !clusterName.trim()}
              onClick={async () => {
                setBusy(true); setError(null)
                try {
                  const c = await api.createCluster(orgId, clusterName.trim())
                  setClusterName('')
                  await reloadClusters()
                  setSelected(c.id)
                } catch (e) {
                  setError(e instanceof Error ? e.message : String(e))
                } finally {
                  setBusy(false)
                }
              }}
            >Создать</button>
          </div>
        </div>
        <div className="panel form">
          <h2>Добавить хост</h2>
          {!selected ? (
            <div className="muted">Выберите кластер.</div>
          ) : (
            <>
              <label>Имя<input value={nodeForm.name} onChange={(e) => setNodeForm({ ...nodeForm, name: e.target.value })} /></label>
              <label>Docker Engine URL
                <input value={nodeForm.engineUrl} onChange={(e) => setNodeForm({ ...nodeForm, engineUrl: e.target.value })} />
              </label>
              <button
                className="btn primary"
                type="button"
                disabled={busy}
                onClick={async () => {
                  setBusy(true); setError(null)
                  try {
                    const n = await api.addNode(selected, nodeForm.name, nodeForm.engineUrl)
                    setHint(n.installHint || `Join token: ${n.joinToken}`)
                    setNodes(await api.listNodes(selected))
                    await reloadClusters()
                  } catch (e) {
                    setError(e instanceof Error ? e.message : String(e))
                  } finally {
                    setBusy(false)
                  }
                }}
              >Зарегистрировать</button>
              {hint && <div className="codebox">{hint}</div>}
            </>
          )}
        </div>
      </div>
      {selected && (
        <div className="panel">
          <h2>Узлы</h2>
          {nodes.length === 0 ? (
            <div className="empty">Нет зарегистрированных хостов.</div>
          ) : (
            <table className="table">
              <thead><tr><th>Имя</th><th>Роль</th><th>Статус</th><th>Engine</th></tr></thead>
              <tbody>
                {nodes.map((n) => (
                  <tr key={n.id}>
                    <td>{n.name}</td>
                    <td className="mono">{n.role}</td>
                    <td>{badge(n.status)}</td>
                    <td className="mono muted">{n.hostname || n.engineUrl || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </>
  )
}
