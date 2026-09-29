import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../lib/api'
import { KARBYZ_EXAMPLES } from '../lib/karbyzExamples'
import { STOREFRONT_KARBYZ } from '../lib/storefrontGuide'

const HELLO = `жыелма салам ::
  чыгар ("Карбыз работает!")
ахыр жыелма.
`

type TermLine = { kind: 'in' | 'out' | 'err' | 'meta'; text: string }

export default function KarbyzIdePage() {
  const [source, setSource] = useState(STOREFRONT_KARBYZ)
  const [stdin, setStdin] = useState('')
  const [busy, setBusy] = useState(false)
  const [lines, setLines] = useState<TermLine[]>([
    { kind: 'meta', text: 'Карбыз IDE · Run исполняет AST · Compile → Jenkinsfile · IR — каноническая запись манифеста' },
  ])
  const termRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    termRef.current?.scrollTo({ top: termRef.current.scrollHeight })
  }, [lines])

  const push = (extra: TermLine[]) => setLines((prev) => [...prev, ...extra])

  const run = async (mode: 'run' | 'as-yaml') => {
    setBusy(true)
    push([
      { kind: 'in', text: mode === 'run' ? '$ kabak run program.kbz' : '$ kabak run program.kbz --as-yaml' },
    ])
    try {
      const r = await api.karbyzRun(source, mode, stdin || undefined)
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
    push([{ kind: 'in', text: '$ compile program.kbz → Jenkinsfile' }])
    try {
      const r = await api.karbyzCompile(source)
      if (r.ok) {
        push([
          { kind: 'meta', text: `job: ${r.jobName}` },
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

  const loadExample = (id: string) => {
    if (id === 'storefront') {
      setSource(STOREFRONT_KARBYZ)
      return
    }
    if (id === 'hello') {
      setSource(HELLO)
      return
    }
    const ex = KARBYZ_EXAMPLES.find((e) => e.id === id)
    if (ex) setSource(ex.source)
  }

  return (
    <>
      <div className="topbar">
        <div>
          <h1>Карбыз · мини-IDE</h1>
          <p>
            Редактор <code>.kbz</code>, терминал исполнения и компиляция в Jenkinsfile.
            Спецификация — <Link to="/app/docs">здесь</Link>.
          </p>
        </div>
        <div className="btn-row">
          <Link className="btn ghost" to="/app/studio">Storefront</Link>
          <Link className="btn ghost" to="/app/docs">Спецификация</Link>
        </div>
      </div>

      <div className="ide-toolbar panel">
        <div className="ide-samples">
          <span className="label">Примеры</span>
          <button type="button" className="btn btn-sm" onClick={() => loadExample('hello')}>hello</button>
          <button type="button" className="btn btn-sm" onClick={() => loadExample('storefront')}>storefront</button>
          {KARBYZ_EXAMPLES.filter((e) => e.id !== 'devops').slice(0, 4).map((e) => (
            <button key={e.id} type="button" className="btn btn-sm" onClick={() => loadExample(e.id)}>
              {e.id}
            </button>
          ))}
        </div>
        <div className="btn-row">
          <button className="btn primary" disabled={busy} onClick={() => run('run')}>
            {busy ? '…' : '▶ Run'}
          </button>
          <button className="btn" disabled={busy} onClick={() => run('as-yaml')}>
            IR / YAML
          </button>
          <button className="btn" disabled={busy} onClick={compile}>
            Compile → Jenkins
          </button>
          <button className="btn ghost" type="button" onClick={() => setLines([])}>
            Clear
          </button>
        </div>
      </div>

      <div className="ide-layout">
        <div className="panel ide-editor-wrap">
          <div className="ide-label-row">
            <span className="label">program.kbz</span>
            <span className="muted mono">{source.split('\n').length} строк</span>
          </div>
          <textarea
            className="ide-editor"
            spellCheck={false}
            value={source}
            onChange={(e) => setSource(e.target.value)}
            onKeyDown={(e) => {
              if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
                e.preventDefault()
                void run('run')
              }
            }}
          />
          <label className="ide-stdin">
            stdin (для керт)
            <input value={stdin} onChange={(e) => setStdin(e.target.value)} placeholder="строка ввода…" />
          </label>
        </div>

        <div className="panel ide-term-wrap">
          <div className="ide-label-row">
            <span className="label">терминал</span>
            <span className="muted">⌘/Ctrl+Enter = Run</span>
          </div>
          <div className="ide-term" ref={termRef}>
            {lines.length === 0 ? (
              <div className="ide-term-line meta">пусто — нажмите Run</div>
            ) : (
              lines.map((l, i) => (
                <pre key={i} className={`ide-term-line ${l.kind}`}>{l.text}</pre>
              ))
            )}
          </div>
        </div>
      </div>
    </>
  )
}
