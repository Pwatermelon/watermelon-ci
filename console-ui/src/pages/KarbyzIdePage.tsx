import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../lib/api'
import { KARBYZ_EXAMPLES } from '../lib/karbyzExamples'
import { KBZ_FILE_ACCEPT, readLocalTextFile } from '../lib/loadKbzFile'

type TermLine = { kind: 'in' | 'out' | 'err' | 'meta'; text: string }

export default function KarbyzIdePage() {
  const first = KARBYZ_EXAMPLES[0]
  const [exampleId, setExampleId] = useState(first?.id || 'hello')
  const [fileName, setFileName] = useState('program.kbz')
  const [source, setSource] = useState(first?.source || '')
  const [stdin, setStdin] = useState(first?.stdin || '')
  const [busy, setBusy] = useState(false)
  const [lines, setLines] = useState<TermLine[]>([
    { kind: 'meta', text: 'Карбыз IDE · загрузите .kbz или выберите пример · Run / IR / Compile' },
  ])
  const termRef = useRef<HTMLDivElement>(null)
  const fileRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    termRef.current?.scrollTo({ top: termRef.current.scrollHeight })
  }, [lines])

  const push = (extra: TermLine[]) => setLines((prev) => [...prev, ...extra])

  const loadExample = (id: string) => {
    const ex = KARBYZ_EXAMPLES.find((e) => e.id === id)
    if (!ex) return
    setExampleId(id)
    setFileName(`${id}.kbz`)
    setSource(ex.source)
    setStdin(ex.stdin || '')
    if (fileRef.current) fileRef.current.value = ''
    setLines([
      { kind: 'meta', text: `${ex.title} · ${ex.summary}` },
      ...(ex.expected ? [{ kind: 'meta' as const, text: `ожидание: ${ex.expected}` }] : []),
    ])
  }

  const loadFile = async (file: File | null) => {
    if (!file) return
    setBusy(true)
    try {
      const text = await readLocalTextFile(file)
      setSource(text)
      setFileName(file.name)
      setExampleId('')
      setLines([
        { kind: 'meta', text: `загружен файл · ${file.name} · ${text.length} символов` },
      ])
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
    push([{ kind: 'in', text: `$ compile ${fileName} → Jenkinsfile` }])
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

      {active ? (
        <p className="muted" style={{ margin: 0 }}>
          <strong>{active.title}</strong> — {active.summary}
          {' · '}
          <span className="mono">{active.constructs.join(' · ')}</span>
        </p>
      ) : (
        <p className="muted" style={{ margin: 0 }}>
          Файл <span className="mono">{fileName}</span> — можно править и запускать.
        </p>
      )}

      <div className="ide-layout">
        <div className="ide-editor-wrap panel">
          <div className="ide-label-row">
            <span className="label mono">{fileName}</span>
          </div>
          <textarea
            className="ide-editor"
            value={source}
            onChange={(e) => setSource(e.target.value)}
            spellCheck={false}
            onDragOver={(e) => e.preventDefault()}
            onDrop={(e) => {
              e.preventDefault()
              const f = e.dataTransfer.files?.[0]
              if (f) void loadFile(f)
            }}
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
