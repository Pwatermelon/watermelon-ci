import { useEffect, useState } from 'react'
import { api, Cluster, WorkerNode } from '../lib/api'
import { useWorkspace } from './LandingPage'

function badge(ok: boolean, label: string) {
  return <span className={`badge ${ok ? 'ok' : 'danger'}`}>{label}</span>
}

export default function HostsPage() {
  const { orgId } = useWorkspace()
  const [clusters, setClusters] = useState<Cluster[]>([])
  const [selected, setSelected] = useState<string | null>(null)
  const [nodes, setNodes] = useState<WorkerNode[]>([])
  const [error, setError] = useState<string | null>(null)
  const [nodeForm, setNodeForm] = useState({
    name: 'demo-vm-c',
    engineUrl: 'http://demo-vm-a:2375',
  })
  const [busy, setBusy] = useState(false)

  const refresh = async () => {
    if (!orgId) return
    setError(null)
    try {
      const c = await api.listClusters(orgId)
      setClusters(c)
      const id = selected && c.some((x) => x.id === selected) ? selected : c[0]?.id || null
      setSelected(id)
      if (id) setNodes(await api.listNodes(id))
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    }
  }

  useEffect(() => { refresh() }, [orgId])
  useEffect(() => {
    if (!selected) return
    api.listNodes(selected).then(setNodes).catch((e) => setError(String(e.message || e)))
  }, [selected])

  return (
    <>
      <div className="topbar">
        <div>
          <h1>Хосты</h1>
          <p>Docker Engine на demo-VM (DinD). CI раскатывает образы через DOCKER_HOST.</p>
        </div>
        <button type="button" className="btn ghost" onClick={refresh}>Обновить</button>
      </div>
      {error && <div className="error">{error}</div>}

      <div className="grid two">
        <div className="panel">
          <h2>Кластеры</h2>
          {clusters.length === 0 ? (
            <div className="empty">Нет кластеров — нажмите «Сидировать демо» на лендинге.</div>
          ) : (
            <table className="table">
              <thead><tr><th>Имя</th><th>Ноды</th><th>Контейнеры</th></tr></thead>
              <tbody>
                {clusters.map((c) => (
                  <tr
                    key={c.id}
                    className={selected === c.id ? 'row-active' : undefined}
                    style={{ cursor: 'pointer' }}
                    onClick={() => setSelected(c.id)}
                  >
                    <td>{c.name}</td>
                    <td>{c.onlineNodes}/{c.nodeCount}</td>
                    <td>{c.containerCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="panel">
          <h2>Добавить ноду</h2>
          <label className="field">
            <span>Имя</span>
            <input value={nodeForm.name} onChange={(e) => setNodeForm({ ...nodeForm, name: e.target.value })} />
          </label>
          <label className="field">
            <span>Engine URL (HTTP)</span>
            <input
              value={nodeForm.engineUrl}
              onChange={(e) => setNodeForm({ ...nodeForm, engineUrl: e.target.value })}
              placeholder="http://demo-vm-a:2375"
            />
          </label>
          <p className="muted">Только TCP/HTTP Docker API. unix:///… не поддерживается.</p>
          <button
            type="button"
            className="btn"
            disabled={!selected || busy}
            onClick={async () => {
              if (!selected) return
              setBusy(true); setError(null)
              try {
                await api.addNode(selected, nodeForm.name, nodeForm.engineUrl)
                setNodes(await api.listNodes(selected))
              } catch (e) {
                setError(e instanceof Error ? e.message : String(e))
              } finally {
                setBusy(false)
              }
            }}
          >
            Зарегистрировать
          </button>
        </div>
      </div>

      <div className="panel">
        <h2>Ноды {selected ? '' : '(выберите кластер)'}</h2>
        {nodes.length === 0 ? (
          <div className="empty">Нет нод в кластере.</div>
        ) : (
          <table className="table">
            <thead>
              <tr><th>Имя</th><th>Hostname</th><th>Engine</th><th>Reachable</th><th>Статус</th></tr>
            </thead>
            <tbody>
              {nodes.map((n) => (
                <tr key={n.id}>
                  <td>{n.name}</td>
                  <td className="mono muted">{n.hostname || '—'}</td>
                  <td className="mono muted">{n.engineUrl || '—'}</td>
                  <td>{badge(!!n.reachable, n.reachable ? 'yes' : 'no')}</td>
                  <td><span className={`badge ${n.status === 'ONLINE' ? 'ok' : 'muted'}`}>{n.status}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
