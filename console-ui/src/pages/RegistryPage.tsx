import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, OrgRegistryView, RegistryImage } from '../lib/api'
import { useWorkspace } from './LandingPage'

type TreeNode = {
  name: string
  path: string
  children: TreeNode[]
  tags: { version: string; locator: string; projectSlug?: string }[]
}

function buildTree(catalog: RegistryImage[]): TreeNode {
  const root: TreeNode = { name: 'registry', path: '', children: [], tags: [] }
  for (const img of catalog) {
    const parts = (img.name || 'unknown').split('/').filter(Boolean)
    let cur = root
    let path = ''
    for (const part of parts) {
      path = path ? `${path}/${part}` : part
      let next = cur.children.find((c) => c.name === part)
      if (!next) {
        next = { name: part, path, children: [], tags: [] }
        cur.children.push(next)
      }
      cur = next
    }
    cur.tags.push({
      version: img.version,
      locator: img.locator,
      projectSlug: (img as RegistryImage & { projectSlug?: string }).projectSlug,
    })
  }
  const sortRec = (n: TreeNode) => {
    n.children.sort((a, b) => a.name.localeCompare(b.name))
    n.children.forEach(sortRec)
  }
  sortRec(root)
  return root
}

function Folder({ node, depth = 0 }: { node: TreeNode; depth?: number }) {
  const [open, setOpen] = useState(depth < 2)
  const hasKids = node.children.length > 0 || node.tags.length > 0
  return (
    <div className="reg-node" style={{ paddingLeft: depth ? 14 : 0 }}>
      {node.path ? (
        <button type="button" className="reg-folder" onClick={() => setOpen((v) => !v)} disabled={!hasKids}>
          <span className="reg-twisty mono">{open ? '▾' : '▸'}</span>
          <span className="reg-folder-name mono">{node.name}/</span>
          <span className="muted reg-count">
            {node.children.length ? `${node.children.length} dir` : ''}
            {node.tags.length ? `${node.tags.length} tag` : ''}
          </span>
        </button>
      ) : null}
      {open && (
        <div className="reg-children">
          {node.children.map((c) => (
            <Folder key={c.path} node={c} depth={depth + (node.path ? 1 : 0)} />
          ))}
          {node.tags.map((t) => (
            <div className="reg-tag" key={`${node.path}:${t.version}:${t.locator}`}>
              <span className="mono">{t.version}</span>
              <span className="mono muted">{t.locator}</span>
              {t.projectSlug ? (
                <Link className="badge ok" to="/app/pipelines">{t.projectSlug}</Link>
              ) : null}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default function RegistryPage() {
  const { orgId } = useWorkspace()
  const [data, setData] = useState<OrgRegistryView | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const load = () => {
    if (!orgId) return
    setLoading(true)
    setError(null)
    api.orgRegistry(orgId)
      .then(setData)
      .catch((e) => setError(String(e.message || e)))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [orgId])

  const tree = useMemo(() => buildTree(data?.catalog || []), [data])

  const recorded = useMemo(() => {
    const arts = data?.artifacts || []
    // latest per project+name+version, skip pure nginx seed noise if any left
    const map = new Map<string, (typeof arts)[0]>()
    for (const a of arts) {
      if (a.source === 'runtime-seed') continue
      const key = `${a.projectSlug}/${a.name}:${a.version}`
      if (!map.has(key)) map.set(key, a)
    }
    return [...map.values()]
  }, [data])

  const byProject = useMemo(() => {
    const groups = new Map<string, typeof recorded>()
    for (const a of recorded) {
      const list = groups.get(a.projectSlug) || []
      list.push(a)
      groups.set(a.projectSlug, list)
    }
    return [...groups.entries()].sort((a, b) => a[0].localeCompare(b[0]))
  }, [recorded])

  return (
    <>
      <div className="topbar">
        <div>
          <h1>Образы</h1>
          <p>Дерево Docker Registry по организации — не только текущий проект</p>
        </div>
        <button type="button" className="btn ghost" onClick={load} disabled={loading}>Обновить</button>
      </div>
      {error && <div className="error">{error}</div>}
      {data?.pushHost && (
        <p className="muted mono" style={{ marginTop: 0 }}>push host: {data.pushHost}</p>
      )}

      <div className="panel">
        <h2>Каталог registry</h2>
        <p className="muted" style={{ marginTop: 0 }}>
          Папки = репозитории (`watermelon/storefront`). Тег появляется после stage publish.
        </p>
        {!data?.catalog?.length ? (
          <div className="empty">
            Каталог пуст. Запусти пайплайн storefront/payments с publish — образ появится здесь.
          </div>
        ) : (
          <div className="reg-tree">
            <div className="reg-root mono muted">{data.pushHost || 'registry'}/</div>
            <Folder node={tree} />
          </div>
        )}
      </div>

      <div className="panel">
        <h2>Записи платформы</h2>
        <p className="muted" style={{ marginTop: 0 }}>
          Сгруппировано по проекту. Seed `nginx:alpine` сюда не пишется — это только runtime.
        </p>
        {byProject.length === 0 ? (
          <div className="empty">Пока нет зарегистрированных образов CI.</div>
        ) : (
          <div className="reg-projects">
            {byProject.map(([slug, items]) => (
              <div className="reg-project" key={slug}>
                <div className="reg-project-head mono">
                  <span className="reg-twisty">▾</span>
                  {slug}/
                </div>
                <ul className="reg-project-list">
                  {items.map((a) => (
                    <li key={a.id}>
                      <span className="mono">{a.name}:{a.version}</span>
                      <span className="mono muted">{a.locator}</span>
                      <span className="muted">{a.createdAt ? new Date(a.createdAt).toLocaleString() : ''}</span>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        )}
      </div>

      {(data?.runtimeOnly?.length || 0) > 0 && (
        <div className="panel">
          <h2>Запущено без publish</h2>
          <p className="muted" style={{ marginTop: 0 }}>
            Демо-seed поднял контейнер публичным образом — в registry его нет, пока не соберёте пайплайн.
          </p>
          <table className="table">
            <thead>
              <tr><th>Проект</th><th>Release</th><th>Image</th><th>Env</th><th></th></tr>
            </thead>
            <tbody>
              {data!.runtimeOnly.map((r) => (
                <tr key={`${r.projectSlug}-${r.releaseName}`}>
                  <td className="mono">{r.projectSlug}</td>
                  <td className="mono">{r.releaseName}</td>
                  <td className="mono muted">{r.imageRef}</td>
                  <td>{r.environment}</td>
                  <td><span className="badge warn">seed</span></td>
                </tr>
              ))}
            </tbody>
          </table>
          <p className="muted">
            Чтобы payments появился в каталоге: <Link to="/app/pipelines">Пайплайны</Link>
            {' '}→ манифест payments → Запустить (build/publish/deploy).
          </p>
        </div>
      )}
    </>
  )
}
