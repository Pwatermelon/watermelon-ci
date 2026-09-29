import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'

type Block =
  | { t: 'h1' | 'h2' | 'h3'; text: string; id: string }
  | { t: 'p'; text: string }
  | { t: 'ul'; items: string[] }
  | { t: 'ol'; items: string[] }
  | { t: 'code'; lang: string; text: string }
  | { t: 'table'; headers: string[]; rows: string[][] }
  | { t: 'hr' }

function slugify(s: string) {
  return s
    .toLowerCase()
    .replace(/[^\p{L}\p{N}]+/gu, '-')
    .replace(/^-|-$/g, '')
    .slice(0, 80)
}

function inline(text: string): ReactNode[] {
  const out: ReactNode[] = []
  const re = /(`[^`]+`|\*\*[^*]+\*\*)/g
  let last = 0
  let m: RegExpExecArray | null
  let key = 0
  while ((m = re.exec(text))) {
    if (m.index > last) out.push(text.slice(last, m.index))
    const raw = m[0]
    if (raw.startsWith('`')) {
      out.push(<code key={key++}>{raw.slice(1, -1)}</code>)
    } else {
      out.push(<strong key={key++}>{raw.slice(2, -2)}</strong>)
    }
    last = m.index + raw.length
  }
  if (last < text.length) out.push(text.slice(last))
  return out
}

function parseMd(src: string): Block[] {
  const lines = src.replace(/\r\n/g, '\n').split('\n')
  const blocks: Block[] = []
  let i = 0
  while (i < lines.length) {
    const line = lines[i]
    if (!line.trim()) {
      i += 1
      continue
    }
    if (line.startsWith('```')) {
      const lang = line.slice(3).trim()
      const buf: string[] = []
      i += 1
      while (i < lines.length && !lines[i].startsWith('```')) {
        buf.push(lines[i])
        i += 1
      }
      i += 1
      blocks.push({ t: 'code', lang, text: buf.join('\n') })
      continue
    }
    if (/^---+$/.test(line.trim())) {
      blocks.push({ t: 'hr' })
      i += 1
      continue
    }
    if (line.startsWith('|')) {
      const rows: string[][] = []
      while (i < lines.length && lines[i].startsWith('|')) {
        const cells = lines[i]
          .split('|')
          .slice(1, -1)
          .map((c) => c.trim())
        if (!/^:?-{3,}:?$/.test(cells[0] || '')) rows.push(cells)
        i += 1
      }
      if (rows.length) {
        blocks.push({ t: 'table', headers: rows[0], rows: rows.slice(1) })
      }
      continue
    }
    if (line.startsWith('# ')) {
      const text = line.slice(2).trim()
      blocks.push({ t: 'h1', text, id: slugify(text) })
      i += 1
      continue
    }
    if (line.startsWith('## ')) {
      const text = line.slice(3).trim()
      blocks.push({ t: 'h2', text, id: slugify(text) })
      i += 1
      continue
    }
    if (line.startsWith('### ')) {
      const text = line.slice(4).trim()
      blocks.push({ t: 'h3', text, id: slugify(text) })
      i += 1
      continue
    }
    if (/^[-*] /.test(line)) {
      const items: string[] = []
      while (i < lines.length && /^[-*] /.test(lines[i])) {
        items.push(lines[i].replace(/^[-*] /, ''))
        i += 1
      }
      blocks.push({ t: 'ul', items })
      continue
    }
    if (/^\d+\. /.test(line)) {
      const items: string[] = []
      while (i < lines.length && /^\d+\. /.test(lines[i])) {
        items.push(lines[i].replace(/^\d+\. /, ''))
        i += 1
      }
      blocks.push({ t: 'ol', items })
      continue
    }
    const para: string[] = [line]
    i += 1
    while (
      i < lines.length
      && lines[i].trim()
      && !lines[i].startsWith('#')
      && !lines[i].startsWith('|')
      && !lines[i].startsWith('```')
      && !/^[-*] /.test(lines[i])
      && !/^\d+\. /.test(lines[i])
      && !/^---+$/.test(lines[i].trim())
    ) {
      para.push(lines[i])
      i += 1
    }
    blocks.push({ t: 'p', text: para.join(' ') })
  }
  return blocks
}

function MdView({ source }: { source: string }) {
  const blocks = useMemo(() => parseMd(source), [source])
  return (
    <div className="spec-md">
      {blocks.map((b, idx) => {
        if (b.t === 'h1') return <h1 key={idx} id={b.id}>{inline(b.text)}</h1>
        if (b.t === 'h2') return <h2 key={idx} id={b.id} className="docs-section">{inline(b.text)}</h2>
        if (b.t === 'h3') return <h3 key={idx} id={b.id}>{inline(b.text)}</h3>
        if (b.t === 'p') return <p key={idx}>{inline(b.text)}</p>
        if (b.t === 'hr') return <hr key={idx} className="spec-hr" />
        if (b.t === 'code') return <pre key={idx} className="docs-code">{b.text}</pre>
        if (b.t === 'ul') {
          return (
            <ul key={idx} className="docs-list">
              {b.items.map((it, j) => <li key={j}>{inline(it)}</li>)}
            </ul>
          )
        }
        if (b.t === 'ol') {
          return (
            <ol key={idx} className="docs-list">
              {b.items.map((it, j) => <li key={j}>{inline(it)}</li>)}
            </ol>
          )
        }
        return (
          <table key={idx} className="docs-table">
            <thead>
              <tr>{b.headers.map((h) => <th key={h}>{inline(h)}</th>)}</tr>
            </thead>
            <tbody>
              {b.rows.map((row, r) => (
                <tr key={r}>{row.map((c, cidx) => <td key={cidx}>{inline(c)}</td>)}</tr>
              ))}
            </tbody>
          </table>
        )
      })}
    </div>
  )
}

type Tab = 'spec' | 'keywords' | 'ebnf'

export default function DocsPage({ embedded = false }: { embedded?: boolean }) {
  const [tab, setTab] = useState<Tab>('spec')
  const [spec, setSpec] = useState('')
  const [keywords, setKeywords] = useState('')
  const [ebnf, setEbnf] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [active, setActive] = useState('')

  useEffect(() => {
    let cancelled = false
    Promise.all([
      fetch('/docs-static/karbyz/SPECIFICATION.md').then((r) => {
        if (!r.ok) throw new Error(`SPECIFICATION.md HTTP ${r.status}`)
        return r.text()
      }),
      fetch('/docs-static/karbyz/KEYWORDS.md').then((r) => {
        if (!r.ok) throw new Error(`KEYWORDS.md HTTP ${r.status}`)
        return r.text()
      }),
      fetch('/docs-static/karbyz/karbyz.ebnf').then((r) => {
        if (!r.ok) throw new Error(`karbyz.ebnf HTTP ${r.status}`)
        return r.text()
      }),
    ])
      .then(([s, k, e]) => {
        if (cancelled) return
        setSpec(s)
        setKeywords(k)
        setEbnf(e)
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : String(e))
      })
    return () => { cancelled = true }
  }, [])

  const toc = useMemo(() => {
    const src = tab === 'spec' ? spec : tab === 'keywords' ? keywords : ''
    return parseMd(src)
      .filter((b): b is Extract<Block, { t: 'h2' }> => b.t === 'h2')
      .map((b) => ({ id: b.id, title: b.text }))
  }, [spec, keywords, tab])

  const body = tab === 'spec' ? spec : tab === 'keywords' ? keywords : ''

  return (
    <div className={embedded ? 'docs-embedded' : 'docs'}>
      {embedded ? (
        <div className="topbar">
          <div>
            <h1>Спецификация Карбыз 1.2</h1>
            <p>Нормативный текст: лексика, типы, отказы, РБНФ, проходы транслятора.</p>
          </div>
          <div className="btn-row">
            <Link className="btn primary" to="/app/ide">IDE</Link>
            <Link className="btn ghost" to="/app/studio">Storefront</Link>
          </div>
        </div>
      ) : (
        <header className="docs-top">
          <Link to="/" className="mark">
            <span className="mark-orb" />
            <span className="logo">Water<span>melon</span></span>
          </Link>
          <div className="docs-top-meta">
            <span className="badge ok">Karbyz 1.2</span>
            <Link className="btn ghost" to="/app/ide">IDE</Link>
            <Link className="btn ghost" to="/">На главную</Link>
          </div>
        </header>
      )}

      <div className="tabs studio-mode-tabs">
        <button type="button" className={tab === 'spec' ? 'active' : ''} onClick={() => setTab('spec')}>
          Спецификация
        </button>
        <button type="button" className={tab === 'keywords' ? 'active' : ''} onClick={() => setTab('keywords')}>
          Словарь
        </button>
        <button type="button" className={tab === 'ebnf' ? 'active' : ''} onClick={() => setTab('ebnf')}>
          РБНФ
        </button>
      </div>

      {error && <div className="error">{error}</div>}
      {!error && !spec && <div className="muted">Загрузка нормативного текста…</div>}

      <div className="docs-layout">
        {tab !== 'ebnf' && (
          <aside className="docs-toc">
            <div className="docs-toc-title">Оглавление</div>
            {toc.map((s) => (
              <button
                key={s.id}
                type="button"
                className={active === s.id ? 'active' : ''}
                onClick={() => {
                  setActive(s.id)
                  document.getElementById(s.id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
                }}
              >
                {s.title}
              </button>
            ))}
          </aside>
        )}
        <article className="docs-body spec-article">
          {tab === 'ebnf' ? (
            <>
              <h2 id="rbnf">Нормативная РБНФ</h2>
              <p className="docs-lead">Файл <code>karbyz.ebnf</code>, согласован с транслятором kabak.</p>
              <pre className="docs-code">{ebnf || '…'}</pre>
            </>
          ) : (
            body && <MdView source={body} />
          )}
        </article>
      </div>
    </div>
  )
}
