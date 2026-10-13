import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, Pipeline } from '../lib/api'
import { KARBYZ_EXAMPLES } from '../lib/karbyzExamples'
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

type TermLine = { kind: 'in' | 'out' | 'err' | 'meta'; text: string }

export default function PipelinesPage() {
  const { projectId } = useWorkspace()
  const [tab, setTab] = useState<'runs' | 'manifest' | 'sandbox'>('runs')
  const [pipelines, setPipelines] = useState<Pipeline[]>([])
  const [manifest, setManifest] = useState(STOREFRONT_KARBYZ)
  const [dialect, setDialect] = useState<'karbyz' | 'yaml'>('karbyz')
  const [preview, setPreview] = useState('')
  const [exampleId, setExampleId] = useState(KARBYZ_EXAMPLES[0]?.id || 'hello')
  const [sandbox, setSandbox] = useState(KARBYZ_EXAMPLES[0]?.source || '')
  const [stdin, setStdin] = useState('')
  const [lines, setLines] = useState<TermLine[]>([
    { kind: 'meta', text: 'Песочница Карбыз · Run / IR / Compile · выберите пример' },
  ])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const termRef = useRef<HTMLDivElement>(null)

  const reload = () =>
    projectId
      ? api.listPipelines(projectId).then(setPipelines).catch((e) => setError(String(e.message || e)))
      : Promise.resolve()

  useEffect(() => { reload() }, [projectId])
  useEffect(() => {
    termRef.current?.scrollTo({ top: termRef.current.scrollHeight })
  }, [lines])

  const loadExample = (id: string) => {
    const ex = KARBYZ_EXAMPLES.find((e) => e.id === id)
    if (!ex) return
    setExampleId(id)
    setSandbox(ex.source)
    setStdin(ex.stdin || '')
    setLines([
      { kind: 'meta', text: `${ex.title} · ${ex.summary}` },
      ...(ex.expected ? [{ kind: 'meta' as const, text: `ожидание: ${ex.expected}` }] : []),
    ])
  }

  const push = (extra: TermLine[]) => setLines((prev) => [...prev, ...extra])

  const run = async (mode: 'run' | 'as-yaml') => {
    setBusy(true)
    push([{ kind: 'in', text: mode === 'run' ? '$ kabak run' : '$ kabak run --as-yaml' }])
    try {
      const r = await api.karbyzRun(sandbox, mode, stdin || undefined)
      push([
        { kind: r.ok ? 'out' : 'err', text: r.stdout || '(пусто)' },
        { kind: 'meta', text: `exit ${r.exitCode}` },
      ])
    } catch (e) {
      push([{ kind: 'err', text: e instanceof Error ? e.message : String(e) }])
    } finally {
      setBusy(false)
    }
  }

  const compile = async () => {
    setBusy(true)
    push([{ kind: 'in', text: '$ compile → Jenkinsfile' }])
    try {
      const r = await api.karbyzCompile(sandbox)
      if (r.ok) {
        push([
          { kind: 'meta', text: `job: ${r.jobName || '—'}` },
          { kind: 'out', text: r.jenkinsfile || '(пусто)' },
        ])
      } else {
        push([{ kind: 'err', text: r.error || 'ошибка компиляции' }])
      }
    } catch (e) {
      push([{ kind: 'err', text: e instanceof Error ? e.message : String(e) }])
    } finally {
      setBusy(false)
    }
  }

  const active = KARBYZ_EXAMPLES.find((e) => e.id === exampleId)

  return (
    <>
      <div className="topbar">
        <div>
          <h1>Пайплайны</h1>
          <p>Запуски · манифест · песочница Карбыз (IDE)</p>
        </div>
      </div>
      <div className="tabs studio-mode-tabs">
        <button type="button" className={tab === 'runs' ? 'active' : ''} onClick={() => setTab('runs')}>Runs</button>
        <button type="button" className={tab === 'manifest' ? 'active' : ''} onClick={() => setTab('manifest')}>Манифест</button>
        <button type="button" className={tab === 'sandbox' ? 'active' : ''} onClick={() => setTab('sandbox')}>Песочница IDE</button>
      </div>
      {error && <div className="error">{error}</div>}

      {tab === 'runs' && (
        <div className="panel">
          <h2>История</h2>
          {pipelines.length === 0 ? (
            <div className="empty">Нет запусков — вкладка «Манифест» или песочница.</div>
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
        <>
          <div className="ide-toolbar panel">
            <div className="ide-samples">
              <span className="label">Примеры</span>
              {KARBYZ_EXAMPLES.map((e) => (
                <button
                  key={e.id}
                  type="button"
                  className={`btn btn-sm ${exampleId === e.id ? 'primary' : ''}`}
                  onClick={() => loadExample(e.id)}
                  title={e.summary}
                >
                  {e.id}
                </button>
              ))}
            </div>
            <div className="btn-row">
              <button className="btn primary" type="button" disabled={busy} onClick={() => void run('run')}>
                {busy ? '…' : '▶ Run'}
              </button>
              <button className="btn" type="button" disabled={busy} onClick={() => void run('as-yaml')}>
                IR / YAML
              </button>
              <button className="btn" type="button" disabled={busy} onClick={() => void compile()}>
                Compile → Jenkins
              </button>
              <button className="btn ghost" type="button" onClick={() => setLines([])}>Clear</button>
              <Link className="btn ghost" to="/app/docs">Спека</Link>
            </div>
          </div>
          {active && (
            <p className="muted" style={{ margin: 0 }}>
              <strong>{active.title}</strong> — {active.summary}
              {active.constructs?.length ? (
                <> · <span className="mono">{active.constructs.join(' · ')}</span></>
              ) : null}
            </p>
          )}
          <div className="ide-layout">
            <div className="ide-editor-wrap panel">
              <div className="ide-label-row">
                <span className="label">program.kbz</span>
              </div>
              <textarea
                className="ide-editor"
                value={sandbox}
                onChange={(e) => setSandbox(e.target.value)}
                spellCheck={false}
              />
              <label className="ide-stdin">stdin (для керт)
                <input value={stdin} onChange={(e) => setStdin(e.target.value)} placeholder="строки ввода через \\n" />
              </label>
            </div>
            <div className="ide-term-wrap panel">
              <div className="ide-label-row">
                <span className="label">терминал</span>
              </div>
              <div className="ide-term" ref={termRef}>
                {lines.length === 0 ? (
                  <div className="ide-term-line meta">пусто</div>
                ) : (
                  lines.map((l, i) => (
                    <div key={i} className={`ide-term-line ${l.kind}`}>{l.text}</div>
                  ))
                )}
              </div>
            </div>
          </div>
        </>
      )}
    </>
  )
}
