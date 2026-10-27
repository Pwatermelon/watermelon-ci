import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, Pipeline, PipelineDetail, PipelineStage } from '../lib/api'
import { KARBYZ_EXAMPLES } from '../lib/karbyzExamples'
import { KBZ_FILE_ACCEPT, readLocalTextFile } from '../lib/loadKbzFile'
import { STOREFRONT_KARBYZ, STOREFRONT_YAML } from '../lib/storefrontGuide'
import { useWorkspace } from './LandingPage'

function badge(status: string) {
  const s = (status || '').toLowerCase()
  const cls = ['success', 'healthy'].some((x) => s.includes(x))
    ? 'ok'
    : ['running', 'queued', 'pending', 'in_progress'].some((x) => s.includes(x))
      ? 'warn'
      : ['fail', 'error', 'cancel'].some((x) => s.includes(x))
        ? 'danger'
        : 'muted'
  return <span className={`badge ${cls}`}>{status}</span>
}

function StageTree({ stages }: { stages: PipelineStage[] }) {
  if (!stages.length) {
    return <div className="empty">Стадии появятся после sync с Jenkins (или из манифеста).</div>
  }
  return (
    <ul className="stage-tree">
      {stages.map((s, i) => (
        <li key={`${s.name}-${i}`} className={`stage-node status-${(s.status || '').toLowerCase()}`}>
          <span className="stage-dot" />
          <div className="stage-body">
            <div className="stage-name">{s.name}</div>
            <div className="stage-meta">
              {badge(s.status)}
              {s.durationMs > 0 ? <span className="muted mono">{(s.durationMs / 1000).toFixed(1)}s</span> : null}
            </div>
          </div>
        </li>
      ))}
    </ul>
  )
}

type TermLine = { kind: 'in' | 'out' | 'err' | 'meta'; text: string }

export default function PipelinesPage() {
  const { projectId } = useWorkspace()
  const [tab, setTab] = useState<'runs' | 'manifest' | 'sandbox'>('runs')
  const [pipelines, setPipelines] = useState<Pipeline[]>([])
  const [selected, setSelected] = useState<number | null>(null)
  const [detail, setDetail] = useState<PipelineDetail | null>(null)
  const [manifest, setManifest] = useState(STOREFRONT_YAML)
  const [dialect, setDialect] = useState<'karbyz' | 'yaml'>('yaml')
  const [preview, setPreview] = useState('')
  const [exampleId, setExampleId] = useState(KARBYZ_EXAMPLES[0]?.id || 'hello')
  const [fileName, setFileName] = useState('program.kbz')
  const [sandbox, setSandbox] = useState(KARBYZ_EXAMPLES[0]?.source || '')
  const [stdin, setStdin] = useState('')
  const [lines, setLines] = useState<TermLine[]>([
    { kind: 'meta', text: 'Песочница Карбыз · загрузите .kbz или выберите пример' },
  ])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const termRef = useRef<HTMLDivElement>(null)
  const fileRef = useRef<HTMLInputElement>(null)

  const reload = async () => {
    if (!projectId) return
    const list = await api.listPipelines(projectId)
    setPipelines(list)
    return list
  }

  const loadDetail = async (number: number) => {
    if (!projectId) return
    const d = await api.getPipelineDetail(projectId, number)
    setDetail(d)
    setSelected(number)
  }

  useEffect(() => {
    reload().then((list) => {
      if (list?.[0] && selected == null) void loadDetail(list[0].number)
    }).catch((e) => setError(String(e.message || e)))
  }, [projectId])

  // auto-sync selected running pipeline
  useEffect(() => {
    if (!projectId || selected == null) return
    const p = pipelines.find((x) => x.number === selected)
    if (!p || !['RUNNING', 'QUEUED'].includes(p.status)) return
    const t = setInterval(() => {
      api.syncPipeline(projectId, selected)
        .then(() => Promise.all([reload(), loadDetail(selected)]))
        .catch(() => {})
    }, 5000)
    return () => clearInterval(t)
  }, [projectId, selected, pipelines])

  useEffect(() => {
    termRef.current?.scrollTo({ top: termRef.current.scrollHeight })
  }, [lines])

  const loadExample = (id: string) => {
    const ex = KARBYZ_EXAMPLES.find((e) => e.id === id)
    if (!ex) return
    setExampleId(id)
    setFileName(`${id}.kbz`)
    setSandbox(ex.source)
    setStdin(ex.stdin || '')
    if (fileRef.current) fileRef.current.value = ''
    setLines([
      { kind: 'meta', text: `${ex.title} · ${ex.summary}` },
      ...(ex.expected ? [{ kind: 'meta' as const, text: `ожидание: ${ex.expected}` }] : []),
    ])
  }

  const push = (extra: TermLine[]) => setLines((prev) => [...prev, ...extra])

  const loadFile = async (file: File | null) => {
    if (!file) return
    setBusy(true)
    try {
      const text = await readLocalTextFile(file)
      setSandbox(text)
      setFileName(file.name)
      setExampleId('')
      setLines([{ kind: 'meta', text: `загружен файл · ${file.name} · ${text.length} символов` }])
    } catch (e) {
      push([{ kind: 'err', text: e instanceof Error ? e.message : String(e) }])
    } finally {
      setBusy(false)
    }
  }

  const run = async (mode: 'run' | 'as-yaml') => {
    setBusy(true)
    push([{ kind: 'in', text: mode === 'run' ? `$ kabak run ${fileName}` : `$ kabak run ${fileName} --as-yaml` }])
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
          <p>История · дерево стадий · рестарт · логи Jenkins</p>
        </div>
        <button type="button" className="btn ghost" disabled={busy || !projectId} onClick={() => void reload()}>
          Обновить
        </button>
      </div>
      <div className="tabs studio-mode-tabs">
        <button type="button" className={tab === 'runs' ? 'active' : ''} onClick={() => setTab('runs')}>Runs</button>
        <button type="button" className={tab === 'manifest' ? 'active' : ''} onClick={() => setTab('manifest')}>Манифест</button>
        <button type="button" className={tab === 'sandbox' ? 'active' : ''} onClick={() => setTab('sandbox')}>Песочница IDE</button>
      </div>
      {error && <div className="error">{error}</div>}

      {tab === 'runs' && (
        <div className="grid two">
          <div className="panel">
            <h2>История</h2>
            {pipelines.length === 0 ? (
              <div className="empty">Нет запусков — вкладка «Манифест» → Запустить.</div>
            ) : (
              <table className="table">
                <thead><tr><th>#</th><th>Ref</th><th>Статус</th><th>Build</th></tr></thead>
                <tbody>
                  {pipelines.map((p) => (
                    <tr
                      key={p.id}
                      className={selected === p.number ? 'row-active' : undefined}
                      style={{ cursor: 'pointer' }}
                      onClick={() => void loadDetail(p.number).catch((e) => setError(String(e.message || e)))}
                    >
                      <td className="mono">#{p.number}</td>
                      <td className="mono">{p.ref}</td>
                      <td>{badge(p.status)}</td>
                      <td className="mono muted">{p.jenkinsBuildNumber ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          <div className="panel">
            <div className="btn-row" style={{ justifyContent: 'space-between', marginBottom: 8 }}>
              <h2 style={{ margin: 0 }}>
                {selected != null ? `Run #${selected}` : 'Выберите run'}
              </h2>
              <div className="btn-row">
                <button
                  type="button"
                  className="btn ghost btn-sm"
                  disabled={busy || selected == null || !projectId}
                  onClick={async () => {
                    if (!projectId || selected == null) return
                    setBusy(true); setError(null)
                    try {
                      await api.syncPipeline(projectId, selected)
                      await reload()
                      await loadDetail(selected)
                    } catch (e) {
                      setError(e instanceof Error ? e.message : String(e))
                    } finally {
                      setBusy(false)
                    }
                  }}
                >
                  Sync
                </button>
                <button
                  type="button"
                  className="btn primary btn-sm"
                  disabled={busy || selected == null || !projectId}
                  onClick={async () => {
                    if (!projectId || selected == null) return
                    setBusy(true); setError(null)
                    try {
                      const r = await api.restartPipeline(projectId, selected)
                      await reload()
                      await loadDetail(r.number)
                    } catch (e) {
                      setError(e instanceof Error ? e.message : String(e))
                    } finally {
                      setBusy(false)
                    }
                  }}
                >
                  Перезапустить
                </button>
              </div>
            </div>

            {!detail ? (
              <div className="empty">Кликните run слева.</div>
            ) : (
              <>
                <p className="muted" style={{ marginTop: 0 }}>
                  job: <span className="mono">{detail.run.jenkinsJobName || '—'}</span>
                  {detail.jenkinsUrl ? (
                    <> · <a href={detail.jenkinsUrl.replace('http://jenkins:8080', 'http://localhost:8082')} target="_blank" rel="noreferrer">открыть в Jenkins</a></>
                  ) : null}
                </p>
                <h3 className="stage-heading">Дерево стадий</h3>
                <StageTree stages={detail.stages || []} />
                <h3 className="stage-heading">Console</h3>
                <pre className="code-block">{detail.consoleText || '(нет логов — sync после старта в Jenkins)'}</pre>
              </>
            )}
          </div>
        </div>
      )}

      {tab === 'manifest' && (
        <div className="grid two">
          <div className="panel form">
            <div className="btn-row">
              <button type="button" className={`btn ${dialect === 'yaml' ? 'primary' : ''}`} onClick={() => { setDialect('yaml'); setManifest(STOREFRONT_YAML) }}>YAML</button>
              <button type="button" className={`btn ${dialect === 'karbyz' ? 'primary' : ''}`} onClick={() => { setDialect('karbyz'); setManifest(STOREFRONT_KARBYZ) }}>Карбыз</button>
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
                    const r = await api.startPipeline(projectId, 'main', manifest)
                    await reload()
                    setTab('runs')
                    await loadDetail(r.number)
                  } catch (e) {
                    setError(e instanceof Error ? e.message : String(e))
                    await reload().catch(() => {})
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
              <input
                ref={fileRef}
                type="file"
                accept={KBZ_FILE_ACCEPT}
                hidden
                onChange={(e) => void loadFile(e.target.files?.[0] || null)}
              />
              <button className="btn" type="button" disabled={busy} onClick={() => fileRef.current?.click()}>
                Загрузить .kbz
              </button>
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
          {active ? (
            <p className="muted" style={{ margin: 0 }}>
              <strong>{active.title}</strong> — {active.summary}
              {active.constructs?.length ? (
                <> · <span className="mono">{active.constructs.join(' · ')}</span></>
              ) : null}
            </p>
          ) : (
            <p className="muted" style={{ margin: 0 }}>
              Файл <span className="mono">{fileName}</span>
            </p>
          )}
          <div className="ide-layout">
            <div className="ide-editor-wrap panel">
              <div className="ide-label-row">
                <span className="label mono">{fileName}</span>
              </div>
              <textarea
                className="ide-editor"
                value={sandbox}
                onChange={(e) => setSandbox(e.target.value)}
                spellCheck={false}
                onDragOver={(e) => e.preventDefault()}
                onDrop={(e) => {
                  e.preventDefault()
                  const f = e.dataTransfer.files?.[0]
                  if (f) void loadFile(f)
                }}
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
