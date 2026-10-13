import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../lib/api'
import { KARBYZ_EXAMPLES } from '../lib/karbyzExamples'

type TermLine = { kind: 'in' | 'out' | 'err' | 'meta'; text: string }

export default function KarbyzIdePage() {
  const first = KARBYZ_EXAMPLES[0]
  const [exampleId, setExampleId] = useState(first?.id || 'hello')
  const [source, setSource] = useState(first?.source || '')
  const [stdin, setStdin] = useState(first?.stdin || '')
  const [busy, setBusy] = useState(false)
  const [lines, setLines] = useState<TermLine[]>([
    { kind: 'meta', text: 'Карбыз IDE · Run · IR/YAML · Compile → Jenkinsfile' },
  ])
  const termRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    termRef.current?.scrollTo({ top: termRef.current.scrollHeight })
  }, [lines])

  const push = (extra: TermLine[]) => setLines((prev) => [...prev, ...extra])

  const loadExample = (id: string) => {
    const ex = KARBYZ_EXAMPLES.find((e) => e.id === id)
    if (!ex) return
    setExampleId(id)
    setSource(ex.source)
    setStdin(ex.stdin || '')
    setLines([
      { kind: 'meta', text: `${ex.title} · ${ex.summary}` },
      ...(ex.expected ? [{ kind: 'meta' as const, text: `ожидание: ${ex.expected}` }] : []),
    ])
  }

  const run = async (mode: 'run' | 'as-yaml') => {
    setBusy(true)
    push([{ kind: 'in', text: mode === 'run' ? '$ kabak run program.kbz' : '$ kabak run --as-yaml' }])
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
          <h1>Карбыз · IDE</h1>
          <p>
            Редактор <code>.kbz</code>, Run / IR, Compile → Jenkinsfile.
            Спека — <Link to="/app/docs">здесь</Link>, дубль в{' '}
            <Link to="/app/pipelines">Пайплайны → Песочница</Link>.
          </p>
        </div>
      </div>

      <div className="ide-toolbar panel">
        <div className="ide-samples">
          <span className="label">Примеры ({KARBYZ_EXAMPLES.length})</span>
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
          <button className="btn primary" disabled={busy} onClick={() => void run('run')}>
            {busy ? '…' : '▶ Run'}
          </button>
          <button className="btn" disabled={busy} onClick={() => void run('as-yaml')}>
            IR / YAML
          </button>
          <button className="btn" disabled={busy} onClick={() => void compile()}>
            Compile → Jenkins
          </button>
          <button className="btn ghost" type="button" onClick={() => setLines([])}>
            Clear
          </button>
        </div>
      </div>

      {active && (
        <p className="muted" style={{ margin: 0 }}>
          <strong>{active.title}</strong> — {active.summary}
          {' · '}
          <span className="mono">{active.constructs.join(' · ')}</span>
        </p>
      )}

      <div className="ide-layout">
        <div className="ide-editor-wrap panel">
          <div className="ide-label-row">
            <span className="label">program.kbz</span>
          </div>
          <textarea
            className="ide-editor"
            value={source}
            onChange={(e) => setSource(e.target.value)}
            spellCheck={false}
          />
          <label className="ide-stdin">
            stdin (керт)
            <input value={stdin} onChange={(e) => setStdin(e.target.value)} placeholder="строки через перевод строки" />
          </label>
        </div>
        <div className="ide-term-wrap panel">
          <div className="ide-label-row">
            <span className="label">терминал</span>
          </div>
          <div className="ide-term" ref={termRef}>
            {lines.map((l, i) => (
              <div key={i} className={`ide-term-line ${l.kind}`}>{l.text}</div>
            ))}
          </div>
        </div>
      </div>
    </>
  )
}
