import { useEffect, useState } from 'react'
import { api, Pipeline } from '../lib/api'
import { STOREFRONT_KARBYZ, STOREFRONT_YAML } from '../lib/storefrontGuide'
import { useWorkspace } from './LandingPage'

function badge(status: string) {
  const s = (status || '').toLowerCase()
  const cls = ['success', 'healthy', 'running'].some((x) => s.includes(x))
    ? 'ok'
    : ['queued', 'pending'].some((x) => s.includes(x))
      ? 'warn'
      : ['fail', 'error'].some((x) => s.includes(x))
        ? 'danger'
        : 'muted'
  return <span className={`badge ${cls}`}>{status}</span>
}

export default function PipelinesPage() {
  const { projectId } = useWorkspace()
  const [tab, setTab] = useState<'runs' | 'manifest' | 'sandbox'>('runs')
  const [pipelines, setPipelines] = useState<Pipeline[]>([])
  const [manifest, setManifest] = useState(STOREFRONT_KARBYZ)
  const [dialect, setDialect] = useState<'karbyz' | 'yaml'>('karbyz')
  const [preview, setPreview] = useState('')
  const [sandbox, setSandbox] = useState(`жыелма demo ::\n  логик ok <- дорес\n  чыгар (ok)\nахыр жыелма.\n`)
  const [term, setTerm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const reload = () =>
    projectId
      ? api.listPipelines(projectId).then(setPipelines).catch((e) => setError(String(e.message || e)))
      : Promise.resolve()

  useEffect(() => { reload() }, [projectId])

  return (
    <>
      <div className="topbar">
        <div>
          <h1>Пайплайны</h1>
          <p>Запуски · манифест YAML/Карбыз · песочница</p>
        </div>
      </div>
      <div className="tabs studio-mode-tabs">
        <button type="button" className={tab === 'runs' ? 'active' : ''} onClick={() => setTab('runs')}>Runs</button>
        <button type="button" className={tab === 'manifest' ? 'active' : ''} onClick={() => setTab('manifest')}>Манифест</button>
        <button type="button" className={tab === 'sandbox' ? 'active' : ''} onClick={() => setTab('sandbox')}>Песочница</button>
      </div>
      {error && <div className="error">{error}</div>}

      {tab === 'runs' && (
        <div className="panel">
          <h2>История</h2>
          {pipelines.length === 0 ? (
            <div className="empty">Нет запусков — скомпилируйте манифест.</div>
          ) : (
            <table className="table">
              <thead><tr><th>#</th><th>Ref</th><th>Статус</th><th>Создан</th></tr></thead>
              <tbody>
                {pipelines.map((p) => (
                  <tr key={p.id}>
                    <td className="mono">{p.number}</td>
                    <td className="mono">{p.ref}</td>
                    <td>{badge(p.status)}</td>
                    <td className="muted">{new Date(p.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {tab === 'manifest' && (
        <div className="grid two">
          <div className="panel form">
            <div className="btn-row">
              <button type="button" className={`btn ${dialect === 'karbyz' ? 'primary' : ''}`} onClick={() => { setDialect('karbyz'); setManifest(STOREFRONT_KARBYZ) }}>Карбыз</button>
              <button type="button" className={`btn ${dialect === 'yaml' ? 'primary' : ''}`} onClick={() => { setDialect('yaml'); setManifest(STOREFRONT_YAML) }}>YAML</button>
            </div>
            <label>Манифест
              <textarea value={manifest} onChange={(e) => setManifest(e.target.value)} />
            </label>
            <div className="btn-row">
              <button
                className="btn"
                type="button"
                disabled={!projectId || busy}
                onClick={async () => {
                  setBusy(true); setError(null)
                  try {
                    const r = await api.previewManifest(projectId, manifest)
                    setPreview(r.jenkinsfile)
                  } catch (e) {
                    setError(e instanceof Error ? e.message : String(e))
                  } finally {
                    setBusy(false)
                  }
                }}
              >Compile → Jenkinsfile</button>
              <button
                className="btn primary"
                type="button"
                disabled={!projectId || busy}
                onClick={async () => {
                  setBusy(true); setError(null)
                  try {
                    await api.startPipeline(projectId, 'main', manifest)
                    await reload()
                    setTab('runs')
                  } catch (e) {
                    setError(e instanceof Error ? e.message : String(e))
                  } finally {
                    setBusy(false)
                  }
                }}
              >Запустить</button>
            </div>
          </div>
          <div className="panel">
            <h2>Jenkinsfile</h2>
            <div className="codebox">{preview || '// Compile preview'}</div>
          </div>
        </div>
      )}

      {tab === 'sandbox' && (
        <div className="grid two">
          <div className="panel form">
            <h2>Карбыз · Run</h2>
            <textarea className="ide-editor" value={sandbox} onChange={(e) => setSandbox(e.target.value)} spellCheck={false} />
            <div className="btn-row">
              <button
                className="btn primary"
                type="button"
                disabled={busy}
                onClick={async () => {
                  setBusy(true)
                  try {
                    const r = await api.karbyzRun(sandbox, 'run')
                    setTerm(r.stdout || '')
                  } catch (e) {
                    setTerm(e instanceof Error ? e.message : String(e))
                  } finally {
                    setBusy(false)
                  }
                }}
              >Run</button>
              <button
                className="btn"
                type="button"
                disabled={busy}
                onClick={async () => {
                  setBusy(true)
                  try {
                    const r = await api.karbyzCompile(sandbox)
                    setTerm(r.ok ? (r.jenkinsfile || '') : (r.error || 'error'))
                  } catch (e) {
                    setTerm(e instanceof Error ? e.message : String(e))
                  } finally {
                    setBusy(false)
                  }
                }}
              >Compile</button>
            </div>
          </div>
          <div className="panel">
            <h2>Вывод</h2>
            <pre className="ide-term">{term || '—'}</pre>
          </div>
        </div>
      )}
    </>
  )
}
