import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, getGitOwner, RemoteFile, RemoteRepo } from '../lib/api'
import { useWorkspace } from './LandingPage'

export default function ReposPage() {
  const owner = getGitOwner()
  const [repos, setRepos] = useState<RemoteRepo[]>([])
  const [error, setError] = useState<string | null>(null)
  const [name, setName] = useState('')
  const navigate = useNavigate()

  const reload = () =>
    api.listRepos(owner)
      .then(setRepos)
      .catch((e) => setError(e instanceof Error ? e.message : String(e)))

  useEffect(() => { reload() }, [owner])

  return (
    <>
      <div className="topbar">
        <div>
          <h1>Репозитории</h1>
          <p className="mono">org · {owner}</p>
        </div>
      </div>
      {error && <div className="error">{error}</div>}
      <div className="grid two">
        <div className="panel">
          <h2>Список</h2>
          {repos.length === 0 ? (
            <div className="empty">Пусто — создайте репозиторий или откройте демо.</div>
          ) : (
            <table className="table">
              <thead><tr><th>Имя</th><th>Ветка</th><th></th></tr></thead>
              <tbody>
                {repos.map((r) => (
                  <tr key={r.name}>
                    <td className="mono">{r.owner}/{r.name}</td>
                    <td className="mono">{r.defaultBranch}</td>
                    <td>
                      <button className="btn" type="button" onClick={() => navigate(`/app/repos/${r.owner}/${r.name}`)}>
                        Открыть
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
        <div className="panel form">
          <h2>Новый репозиторий</h2>
          <label>Имя
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="my-service" />
          </label>
          <button
            className="btn primary"
            type="button"
            disabled={!name.trim()}
            onClick={async () => {
              setError(null)
              try {
                const r = await api.ensureRepo(owner, name.trim())
                await reload()
                navigate(`/app/repos/${r.owner}/${r.name}`)
              } catch (e) {
                setError(e instanceof Error ? e.message : String(e))
              }
            }}
          >Создать</button>
        </div>
      </div>
    </>
  )
}

export function RepoEditorPage() {
  const { owner = '', repo = '' } = useParams()
  const { projectId } = useWorkspace()
  const [path, setPath] = useState('')
  const [tree, setTree] = useState<RemoteFile[]>([])
  const [content, setContent] = useState('')
  const [sha, setSha] = useState('')
  const [message, setMessage] = useState('update')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [preview, setPreview] = useState('')
  const [dir, setDir] = useState('')

  const loadTree = (p = '') =>
    api.listTree(owner, repo, p)
      .then(setTree)
      .catch((e) => setError(e instanceof Error ? e.message : String(e)))

  useEffect(() => { loadTree('') }, [owner, repo])

  const openFile = async (filePath: string) => {
    setError(null)
    try {
      const f = await api.readFile(owner, repo, filePath)
      setPath(filePath)
      setContent(f.content)
      const meta = tree.find((t) => t.path === filePath)
      setSha(meta?.sha || '')
      setDir(filePath.includes('/') ? filePath.slice(0, filePath.lastIndexOf('/')) : '')
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    }
  }

  const openDir = (p: string) => {
    setDir(p)
    loadTree(p)
  }

  return (
    <>
      <div className="topbar">
        <div>
          <Link className="btn ghost" to="/app/repos" style={{ padding: 0 }}>← Репозитории</Link>
          <h1 className="mono">{owner}/{repo}</h1>
          <p>Дерево файлов · правка · commit</p>
        </div>
      </div>
      {error && <div className="error">{error}</div>}
      <div className="repo-layout">
        <aside className="panel repo-tree">
          <div className="label">/{dir || ''}</div>
          {dir && (
            <button type="button" className="repo-file" onClick={() => openDir(dir.includes('/') ? dir.slice(0, dir.lastIndexOf('/')) : '')}>
              ..
            </button>
          )}
          {tree.map((f) => (
            <button
              key={f.path}
              type="button"
              className={`repo-file ${path === f.path ? 'active' : ''}`}
              onClick={() => (f.type === 'dir' ? openDir(f.path) : openFile(f.path))}
            >
              <span className="mono">{f.path.split('/').pop()}</span>
              <span className="muted">{f.type}</span>
            </button>
          ))}
          <div className="form" style={{ marginTop: 12 }}>
            <label>Новый файл
              <input
                placeholder="path/file.kbz"
                onKeyDown={async (e) => {
                  if (e.key !== 'Enter') return
                  const p = (e.target as HTMLInputElement).value.trim()
                  if (!p) return
                  setPath(p)
                  setContent('')
                  setSha('')
                }}
              />
            </label>
          </div>
        </aside>
        <div className="panel form repo-editor">
          <div className="label">{path || 'файл не выбран'}</div>
          <textarea
            className="ide-editor"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            disabled={!path}
            spellCheck={false}
          />
          <label>Сообщение commit
            <input value={message} onChange={(e) => setMessage(e.target.value)} />
          </label>
          <div className="btn-row">
            <button
              className="btn primary"
              type="button"
              disabled={!path || busy}
              onClick={async () => {
                setBusy(true); setError(null)
                try {
                  const r = await api.writeFile(owner, repo, { path, content, message, sha })
                  setSha(r.sha)
                  await loadTree(dir)
                } catch (e) {
                  setError(e instanceof Error ? e.message : String(e))
                } finally {
                  setBusy(false)
                }
              }}
            >Сохранить</button>
            {path.endsWith('.kbz') || path.endsWith('.yml') || path.endsWith('.yaml') ? (
              <>
                <button
                  className="btn"
                  type="button"
                  disabled={!projectId || busy || !content.trim()}
                  onClick={async () => {
                    setBusy(true); setError(null)
                    try {
                      const r = await api.previewManifest(projectId, content)
                      setPreview(r.jenkinsfile)
                    } catch (e) {
                      setError(e instanceof Error ? e.message : String(e))
                    } finally {
                      setBusy(false)
                    }
                  }}
                >Compile</button>
                <button
                  className="btn"
                  type="button"
                  disabled={!projectId || busy || !content.trim()}
                  onClick={async () => {
                    setBusy(true); setError(null)
                    try {
                      await api.startPipeline(projectId, 'main', content)
                    } catch (e) {
                      setError(e instanceof Error ? e.message : String(e))
                    } finally {
                      setBusy(false)
                    }
                  }}
                >Run pipeline</button>
              </>
            ) : null}
          </div>
          {preview && <div className="codebox" style={{ marginTop: 12 }}>{preview}</div>}
        </div>
      </div>
    </>
  )
}
