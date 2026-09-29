import { Link, NavLink, Navigate, Route, Routes, useNavigate, useParams } from 'react-router-dom'
import { useEffect, useState } from 'react'
import {
  api,
  Artifact,
  Cluster,
  Container,
  getStoredOrgId,
  getStoredProjectId,
  GitOpsApp,
  Issue,
  Org,
  Pipeline,
  Project,
  SecretMeta,
  setStoredOrgId,
  setStoredProjectId,
  WorkerNode,
} from './lib/api'
import DocsPage from './pages/DocsPage'
import KarbyzIdePage from './pages/KarbyzIdePage'
import { KARBYZ_EXAMPLES } from './lib/karbyzExamples'
import {
  STOREFRONT_GUIDE,
  STOREFRONT_KARBYZ,
  STOREFRONT_PROJECT,
  STOREFRONT_YAML,
} from './lib/storefrontGuide'

function badge(status: string) {
  const s = (status || '').toLowerCase()
  const cls = ['success', 'healthy', 'online', 'running', 'synced', 'done', 'open'].some((x) => s.includes(x))
    ? 'ok'
    : ['queued', 'pending', 'progress', 'warn', 'draining'].some((x) => s.includes(x))
      ? 'warn'
      : ['fail', 'error', 'off', 'dead', 'denied', 'exited', 'closed'].some((x) => s.includes(x))
        ? 'danger'
        : 'muted'
  return <span className={`badge ${cls}`}>{status}</span>
}

function Mark({ size = 'md' }: { size?: 'md' | 'lg' }) {
  return (
    <span className="mark">
      <span className="mark-orb" style={size === 'lg' ? { width: '1.35rem', height: '1.35rem' } : undefined} />
      <span className="logo">Water<span>melon</span></span>
    </span>
  )
}

function Landing() {
  const navigate = useNavigate()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const enter = async (seed: boolean) => {
    setBusy(true)
    setError(null)
    try {
      if (seed) {
        const demo = await api.seedDemo()
        setStoredOrgId(demo.organizationId)
        setStoredProjectId(demo.projectId)
      } else {
        const orgs = await api.listOrgs()
        if (orgs[0]) setStoredOrgId(orgs[0].id)
        else {
          const org = await api.createOrg('Watermelon Demo')
          setStoredOrgId(org.id)
        }
      }
      navigate('/app')
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="landing">
      <div className="landing-glow" aria-hidden />
      <nav className="landing-nav rise">
        <Mark />
        <div className="btn-row">
          <NavLink className="btn ghost" to="/app/docs">Спецификация</NavLink>
          <NavLink className="btn ghost" to="/app/ide">Карбыз IDE</NavLink>
          <button className="btn ghost" onClick={() => enter(false)} disabled={busy}>Войти</button>
          <button className="btn primary" onClick={() => enter(true)} disabled={busy}>
            {busy ? 'Запуск…' : 'Демо'}
          </button>
        </div>
      </nav>
      <section className="landing-hero">
        <div className="landing-copy">
          <p className="brand-lockup rise">Water<span>melon</span></p>
          <h1 className="rise rise-delay-1">Платформа CI/CD без разрозненных инструментов</h1>
          <p className="rise rise-delay-2">
            Манифесты, секреты, GitOps и управление флотом — одна консоль для поставки и эксплуатации.
          </p>
          <div className="cta-row rise rise-delay-3">
            <button className="btn primary" onClick={() => enter(true)} disabled={busy}>
              {busy ? 'Готовим workspace…' : 'Открыть демо'}
            </button>
            <button className="btn" onClick={() => enter(false)} disabled={busy}>
              Пустой workspace
            </button>
          </div>
          {error && <div className="error" style={{ marginTop: 16 }}>{error}</div>}
        </div>
        <div className="product-stage rise rise-delay-2" aria-hidden>
          <div className="mock-top">
            <div className="mock-dot" /><div className="mock-dot" /><div className="mock-dot" />
            <span className="mock-title">Watermelon Console</span>
          </div>
          <div className="mock-grid">
            <div className="mock-side">
              <div className="mock-line accent" />
              <div className="mock-line" />
              <div className="mock-line short" />
              <div className="mock-line" />
              <div className="mock-line short" />
              <div className="mock-line" />
            </div>
            <div className="mock-main">
              <div className="mock-line accent" />
              <div className="mock-card">
                <div className="mono">pipeline #42 · SUCCESS</div>
                <div className="muted" style={{ marginTop: 6, fontSize: 13 }}>GitOps · Vault · tracked</div>
              </div>
              <div className="mock-card">
                <div className="mono">fleet · 3 online · 12 containers</div>
                <div className="muted" style={{ marginTop: 6, fontSize: 13 }}>start · stop · logs · restart</div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}

function Shell({ orgId, children }: { orgId: string; children: React.ReactNode }) {
  const navigate = useNavigate()
  return (
    <div className="shell">
      <aside className="side">
        <button className="brand-btn" onClick={() => navigate('/app')}>
          <Mark />
        </button>
        <nav className="nav">
          <NavLink to="/app" end>Обзор</NavLink>
          <NavLink to="/app/projects">Проекты</NavLink>
          <NavLink to="/app/studio">Storefront</NavLink>
          <NavLink to="/app/ide">Карбыз IDE</NavLink>
          <NavLink to="/app/docs">Спецификация</NavLink>
          <NavLink to="/app/fleet">Флот</NavLink>
        </nav>
        <div className="side-foot">
          <div className="side-foot-label">Организация</div>
          <div className="mono side-foot-id">{orgId.slice(0, 12)}…</div>
          <button className="btn ghost side-exit" onClick={() => navigate('/')}>
            Выйти
          </button>
        </div>
      </aside>
      <div className="main-wrap">
        <header className="app-chrome">
          <div className="app-chrome-left">
            <span className="chrome-pill">Workspace</span>
            <span className="chrome-sep" />
            <span className="muted mono chrome-org">{orgId.slice(0, 8)}</span>
          </div>
          <div className="btn-row">
            <button className="btn ghost btn-sm" onClick={() => navigate('/app/ide')}>Карбыз IDE</button>
            <button className="btn ghost btn-sm" onClick={() => navigate('/app/docs')}>Спека</button>
            <button className="btn primary btn-sm" onClick={() => navigate('/app/studio')}>Storefront</button>
          </div>
        </header>
        <div className="main">{children}</div>
      </div>
    </div>
  )
}

function Overview({ orgId }: { orgId: string }) {
  const [org, setOrg] = useState<Org | null>(null)
  const [projects, setProjects] = useState<Project[]>([])
  const [fleet, setFleet] = useState({ clusters: 0, nodes: 0, onlineNodes: 0, runningContainers: 0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    setLoading(true)
    Promise.all([
      api.listOrgs().then((list) => list.find((o) => o.id === orgId) || list[0] || null),
      api.listProjects(orgId),
      api.fleetOverview(orgId).catch(() => ({ clusters: 0, nodes: 0, onlineNodes: 0, runningContainers: 0, containers: 0 })),
    ])
      .then(([o, p, f]) => { setOrg(o); setProjects(p); setFleet(f) })
      .catch((e) => setError(String(e.message || e)))
      .finally(() => setLoading(false))
  }, [orgId])

  return (
    <>
      <div className="topbar">
        <div>
          <h1>{org?.name || 'Workspace'}</h1>
          <p>Пайплайны, секреты, GitOps и флот — в одной консоли.</p>
        </div>
        <div className="btn-row">
          <button className="btn" onClick={() => navigate('/app/fleet')}>Флот</button>
          <button className="btn primary" onClick={() => navigate('/app/studio')}>Storefront · гайд</button>
          <button className="btn" onClick={() => navigate('/app/projects')}>Проекты</button>
        </div>
      </div>
      {error && <div className="error">{error}</div>}
      <div className="grid stats">
        {[
          ['Проекты', loading ? '—' : projects.length],
          ['Кластеры', loading ? '—' : fleet.clusters],
          ['Онлайн', loading ? '—' : fleet.onlineNodes],
          ['Running', loading ? '—' : fleet.runningContainers],
        ].map(([label, value]) => (
          <div className="stat rise" key={String(label)}>
            <div className="label">{label}</div>
            <div className="value">{value}</div>
          </div>
        ))}
      </div>
      <div className="panel rise">
        <h2>Готовый проект Storefront</h2>
        <p className="muted" style={{ marginTop: 0 }}>
          Пояснения по шагам: код в Git, Maven-сборка, пайплайн на Карбызе (и YAML рядом),
          push в registry, Vault-секреты, раскатка через ArgoCD.
        </p>
        <div className="btn-row">
          <button className="btn primary" onClick={() => navigate('/app/studio')}>Открыть гайд</button>
          {projects[0] && (
            <button
              className="btn"
              onClick={() => {
                setStoredProjectId(projects[0].id)
                navigate(`/app/projects/${projects[0].id}`)
              }}
            >Проект в консоли</button>
          )}
        </div>
      </div>
      <div className="panel rise rise-delay-1">
        <h2>Проекты</h2>
        {loading ? (
          <div><div className="skeleton" /><div className="skeleton" style={{ width: '70%' }} /></div>
        ) : (
          <div className="grid cards">
            {projects.map((p) => (
              <button
                key={p.id}
                className="project-card"
                onClick={() => { setStoredProjectId(p.id); navigate(`/app/projects/${p.id}`) }}
              >
                <strong>{p.name}</strong>
                <div className="muted mono" style={{ marginTop: 6 }}>{p.slug}</div>
                <div className="muted" style={{ marginTop: 10, fontSize: 13 }}>{p.description || 'No description'}</div>
              </button>
            ))}
            {projects.length === 0 && (
              <div className="empty">
                No projects yet.
                <div style={{ marginTop: 10 }}>
                  <button className="btn primary" onClick={() => navigate('/app/projects')}>Create first project</button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </>
  )
}

function ProjectsPage({ orgId }: { orgId: string }) {
  const [projects, setProjects] = useState<Project[]>([])
  const [name, setName] = useState('')
  const [desc, setDesc] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const navigate = useNavigate()
  const reload = () => api.listProjects(orgId).then(setProjects).catch((e) => setError(String(e.message || e)))
  useEffect(() => { reload() }, [orgId])

  return (
    <>
      <div className="topbar">
        <div><h1>Projects</h1><p>Repositories, pipelines and release ownership.</p></div>
      </div>
      {error && <div className="error">{error}</div>}
      <div className="grid two">
        <div className="panel">
          <h2>All projects</h2>
          {projects.length === 0 ? (
            <div className="empty">Create a project to start shipping.</div>
          ) : (
            <table className="table">
              <thead><tr><th>Name</th><th>Slug</th><th></th></tr></thead>
              <tbody>
                {projects.map((p) => (
                  <tr key={p.id}>
                    <td>{p.name}</td>
                    <td className="mono">{p.slug}</td>
                    <td>
                      <button className="btn" onClick={() => { setStoredProjectId(p.id); navigate(`/app/projects/${p.id}`) }}>
                        Open
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
        <div className="panel form">
          <h2>New project</h2>
          <label>Name<input value={name} onChange={(e) => setName(e.target.value)} placeholder="Billing API" /></label>
          <label>Description<textarea value={desc} onChange={(e) => setDesc(e.target.value)} style={{ minHeight: 90 }} /></label>
          <button
            className="btn primary"
            disabled={!name.trim() || busy}
            onClick={async () => {
              setBusy(true)
              try {
                const p = await api.createProject(orgId, name.trim(), desc || undefined)
                setName(''); setDesc('')
                await reload()
                setStoredProjectId(p.id)
                navigate(`/app/projects/${p.id}`)
              } catch (e) {
                setError(e instanceof Error ? e.message : String(e))
              } finally {
                setBusy(false)
              }
            }}
          >{busy ? 'Creating…' : 'Create project'}</button>
        </div>
      </div>
    </>
  )
}

function ProjectPage() {
  const { projectId = '' } = useParams()
  const navigate = useNavigate()
  const [tab, setTab] = useState<'pipelines' | 'issues' | 'secrets' | 'artifacts' | 'gitops'>('pipelines')
  const [project, setProject] = useState<Project | null>(null)
  const [pipelines, setPipelines] = useState<Pipeline[]>([])
  const [issues, setIssues] = useState<Issue[]>([])
  const [secrets, setSecrets] = useState<SecretMeta[]>([])
  const [artifacts, setArtifacts] = useState<Artifact[]>([])
  const [gitops, setGitops] = useState<GitOpsApp[]>([])
  const [manifest, setManifest] = useState('')
  const [preview, setPreview] = useState('')
  const [secretForm, setSecretForm] = useState({ environment: 'production', name: '', value: '' })
  const [issueTitle, setIssueTitle] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)

  const reload = async () => {
    const [p, pipes, iss, sec, arts, apps] = await Promise.all([
      api.getProject(projectId),
      api.listPipelines(projectId),
      api.listIssues(projectId),
      api.listSecrets(projectId).then((r) => r.secrets).catch(() => []),
      api.listArtifacts(projectId).catch(() => []),
      api.listGitOps(projectId).catch(() => []),
    ])
    setProject(p); setPipelines(pipes); setIssues(iss); setSecrets(sec); setArtifacts(arts); setGitops(apps)
    setStoredProjectId(projectId)
    if (!manifest) {
      const t = await api.templates()
      setManifest(t['argocd-app'] || t['java-maven'] || '')
    }
  }

  useEffect(() => {
    setLoading(true)
    reload()
      .catch((e) => setError(String(e.message || e)))
      .finally(() => setLoading(false))
  }, [projectId])

  const tabs = [
    { id: 'pipelines' as const, label: 'Pipelines', count: pipelines.length },
    { id: 'issues' as const, label: 'Issues', count: issues.length },
    { id: 'secrets' as const, label: 'Secrets', count: secrets.length },
    { id: 'artifacts' as const, label: 'Artifacts', count: artifacts.length },
    { id: 'gitops' as const, label: 'GitOps', count: gitops.length },
  ]

  return (
    <>
      <div className="topbar">
        <div>
          <button className="btn ghost" style={{ padding: '0.2rem 0', marginBottom: 6, color: 'var(--muted)' }} onClick={() => navigate('/app/projects')}>
            ← Projects
          </button>
          <h1>{project?.name || (loading ? 'Loading…' : 'Project')}</h1>
          <p className="mono">{project?.slug || '—'} · pipelines · secrets · gitops</p>
        </div>
        <button className="btn" onClick={() => reload().catch((e) => setError(String(e.message || e)))}>Refresh</button>
      </div>
      {error && <div className="error">{error}</div>}
      <div className="tabs">
        {tabs.map((t) => (
          <button key={t.id} className={tab === t.id ? 'active' : ''} onClick={() => setTab(t.id)}>
            {t.label} {t.count > 0 ? <span className="mono" style={{ opacity: 0.7 }}>· {t.count}</span> : null}
          </button>
        ))}
      </div>

      {tab === 'pipelines' && (
        <div className="grid two">
          <div className="panel">
            <h2>Runs</h2>
            {pipelines.length === 0 ? (
              <div className="empty">No runs yet — compile a manifest and ship one.</div>
            ) : (
              <table className="table">
                <thead><tr><th>#</th><th>Ref</th><th>Status</th><th>Created</th></tr></thead>
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
          <div className="panel form">
            <h2>Start pipeline</h2>
            <label>Manifest YAML<textarea value={manifest} onChange={(e) => setManifest(e.target.value)} /></label>
            <div className="btn-row">
              <button
                className="btn"
                disabled={busy || !manifest.trim()}
                onClick={async () => {
                  setBusy(true); setError(null)
                  try {
                    const r = await api.previewManifest(projectId, manifest)
                    setPreview(r.jenkinsfile)
                  } catch (e) { setError(e instanceof Error ? e.message : String(e)) }
                  finally { setBusy(false) }
                }}
              >Preview Jenkinsfile</button>
              <button
                className="btn primary"
                disabled={busy || !manifest.trim()}
                onClick={async () => {
                  setBusy(true); setError(null)
                  try {
                    await api.startPipeline(projectId, 'main', manifest, 'demo')
                    await reload()
                  } catch (e) { setError(e instanceof Error ? e.message : String(e)) }
                  finally { setBusy(false) }
                }}
              >{busy ? 'Running…' : 'Run pipeline'}</button>
            </div>
            {preview && <div className="codebox" style={{ marginTop: 8 }}>{preview}</div>}
          </div>
        </div>
      )}

      {tab === 'issues' && (
        <div className="grid two">
          <div className="panel">
            <h2>Board</h2>
            {issues.length === 0 ? <div className="empty">No issues yet.</div> : (
              <table className="table">
                <thead><tr><th>#</th><th>Title</th><th>Status</th></tr></thead>
                <tbody>
                  {issues.map((i) => (
                    <tr key={i.id}><td className="mono">{i.number}</td><td>{i.title}</td><td>{badge(i.status)}</td></tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
          <div className="panel form">
            <h2>New issue</h2>
            <label>Title<input value={issueTitle} onChange={(e) => setIssueTitle(e.target.value)} placeholder="Fix deploy timeout" /></label>
            <button
              className="btn primary"
              disabled={!issueTitle.trim()}
              onClick={async () => {
                try {
                  await api.createIssue(projectId, issueTitle.trim())
                  setIssueTitle('')
                  await reload()
                } catch (e) { setError(e instanceof Error ? e.message : String(e)) }
              }}
            >Create</button>
          </div>
        </div>
      )}

      {tab === 'secrets' && (
        <div className="grid two">
          <div className="panel">
            <h2>Vault-backed secrets</h2>
            {secrets.length === 0 ? <div className="empty">No secrets stored for this project.</div> : (
              <table className="table">
                <thead><tr><th>Env</th><th>Name</th><th>Path</th></tr></thead>
                <tbody>
                  {secrets.map((s) => (
                    <tr key={s.id}>
                      <td className="mono">{s.environment}</td>
                      <td className="mono">{s.name}</td>
                      <td className="mono muted">{s.vaultPath}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
          <div className="panel form">
            <h2>Upsert secret</h2>
            <label>Environment<input value={secretForm.environment} onChange={(e) => setSecretForm({ ...secretForm, environment: e.target.value })} /></label>
            <label>Name<input value={secretForm.name} onChange={(e) => setSecretForm({ ...secretForm, name: e.target.value })} /></label>
            <label>Value<input type="password" value={secretForm.value} onChange={(e) => setSecretForm({ ...secretForm, value: e.target.value })} /></label>
            <button
              className="btn primary"
              disabled={!secretForm.name.trim() || !secretForm.value}
              onClick={async () => {
                try {
                  await api.upsertSecret(projectId, secretForm.environment, secretForm.name, secretForm.value)
                  setSecretForm({ ...secretForm, name: '', value: '' })
                  await reload()
                } catch (e) { setError(e instanceof Error ? e.message : String(e)) }
              }}
            >Save to Vault</button>
          </div>
        </div>
      )}

      {tab === 'artifacts' && (
        <div className="panel">
          <h2>Registry catalog</h2>
          {artifacts.length === 0 ? <div className="empty">Artifacts appear after pipeline publishes.</div> : (
            <table className="table">
              <thead><tr><th>Kind</th><th>Name</th><th>Version</th><th>Locator</th></tr></thead>
              <tbody>
                {artifacts.map((a) => (
                  <tr key={a.id}>
                    <td className="mono">{a.kind}</td>
                    <td>{a.name}</td>
                    <td className="mono">{a.version}</td>
                    <td className="mono muted">{a.locator}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {tab === 'gitops' && (
        <div className="grid two">
          <div className="panel">
            <h2>ArgoCD applications</h2>
            {gitops.length === 0 ? <div className="empty">No GitOps apps yet — deploy without writing Helm.</div> : (
              <table className="table">
                <thead><tr><th>Release</th><th>Env</th><th>Sync</th><th>Health</th></tr></thead>
                <tbody>
                  {gitops.map((g) => (
                    <tr key={g.id}>
                      <td className="mono">{g.releaseName}</td>
                      <td>{g.environment}</td>
                      <td>{badge(g.syncStatus)}</td>
                      <td>{badge(g.healthStatus)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
          <div className="panel form">
            <h2>Deploy without Helm</h2>
            <p className="muted" style={{ margin: 0 }}>Renders Deployment/Service + Argo Application from a short form.</p>
            <button
              className="btn primary"
              disabled={busy}
              onClick={async () => {
                setBusy(true); setError(null)
                try {
                  await api.gitOpsDeploy(projectId, {
                    environment: 'production',
                    release: project?.slug || 'app',
                    image: `registry.local/watermelon/${project?.slug || 'app'}:latest`,
                    namespace: `${project?.slug || 'app'}-prod`,
                    replicas: 2,
                    ports: ['8080'],
                    secrets: ['DATABASE_URL', 'API_TOKEN'],
                  })
                  await reload()
                } catch (e) { setError(e instanceof Error ? e.message : String(e)) }
                finally { setBusy(false) }
              }}
            >{busy ? 'Deploying…' : 'Deploy via ArgoCD'}</button>
          </div>
        </div>
      )}
    </>
  )
}

function StudioPage() {
  const navigate = useNavigate()
  const projectId = getStoredProjectId()
  const [mode, setMode] = useState<'guide' | 'yaml' | 'karbyz'>('guide')
  const [stepId, setStepId] = useState(STOREFRONT_GUIDE[0].id)
  const [templates, setTemplates] = useState<Record<string, string>>({})
  const [selected, setSelected] = useState('storefront')
  const [karbyzId, setKarbyzId] = useState('devops')
  const [yaml, setYaml] = useState(STOREFRONT_YAML)
  const [preview, setPreview] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [compileLang, setCompileLang] = useState<'yaml' | 'karbyz'>('karbyz')

  const step = STOREFRONT_GUIDE.find((s) => s.id === stepId) ?? STOREFRONT_GUIDE[0]
  const karbyz = KARBYZ_EXAMPLES.find((e) => e.id === karbyzId) ?? KARBYZ_EXAMPLES[0]

  useEffect(() => {
    api.templates().then((t) => {
      setTemplates(t)
      if (t.storefront) {
        setSelected('storefront')
        setYaml(t.storefront)
      }
    }).catch(() => { /* offline templates fallback already in state */ })
  }, [])

  const compile = async (manifest: string) => {
    if (!projectId) return
    setBusy(true)
    setError(null)
    try {
      const r = await api.previewManifest(projectId, manifest)
      setPreview(r.jenkinsfile)
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const runPipeline = async (manifest: string) => {
    if (!projectId) return
    setBusy(true)
    setError(null)
    try {
      await api.startPipeline(projectId, 'main', manifest, 'demo')
      navigate(`/app/projects/${projectId}`)
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div className="topbar studio-topbar">
        <div>
          <h1>Storefront · готовый проект</h1>
          <p>
            Git → сборка → registry → Vault → ArgoCD. Основной манифест — Карбыз; YAML — тот же пайплайн рядом.
          </p>
        </div>
      </div>
      <div className="tabs studio-mode-tabs">
        <button className={mode === 'guide' ? 'active' : ''} onClick={() => setMode('guide')}>Гайд</button>
        <button className={mode === 'yaml' ? 'active' : ''} onClick={() => { setMode('yaml'); setPreview('') }}>YAML</button>
        <button className={mode === 'karbyz' ? 'active' : ''} onClick={() => { setMode('karbyz'); setPreview('') }}>Карбыз</button>
      </div>
      {error && <div className="error">{error}</div>}

      {mode === 'guide' && (
        <div className="guide-layout">
          <aside className="guide-nav panel">
            <div className="guide-project">
              <div className="label">Проект демо</div>
              <strong>{STOREFRONT_PROJECT.name}</strong>
              <div className="muted mono" style={{ marginTop: 4 }}>{STOREFRONT_PROJECT.org}/{STOREFRONT_PROJECT.slug}</div>
              <pre className="docs-code guide-tree">{STOREFRONT_PROJECT.tree}</pre>
            </div>
            <h2>Шаги</h2>
            {STOREFRONT_GUIDE.map((s) => (
              <button
                key={s.id}
                className={`guide-step-btn ${s.id === step.id ? 'active' : ''}`}
                onClick={() => { setStepId(s.id); setPreview('') }}
              >
                <span className="guide-eyebrow">{s.eyebrow}</span>
                <strong>{s.title}</strong>
              </button>
            ))}
            <div className="btn-row" style={{ marginTop: '0.75rem', flexWrap: 'wrap' }}>
              <button className="btn ghost btn-sm" onClick={() => projectId && navigate(`/app/projects/${projectId}`)}>
                Открыть проект
              </button>
              <button className="btn ghost btn-sm" onClick={() => navigate('/app/ide')}>Карбыз IDE</button>
              <button className="btn ghost btn-sm" onClick={() => navigate('/app/docs')}>Спецификация</button>
            </div>
          </aside>

          <div className="guide-main">
            <div className="panel">
              <div className="guide-eyebrow">{step.eyebrow}</div>
              <h2 style={{ margin: '0.25rem 0 0.5rem' }}>{step.title}</h2>
              <p className="muted" style={{ marginTop: 0 }}>{step.summary}</p>
              <ul className="docs-list">
                {step.bullets.map((b) => <li key={b}>{b}</li>)}
              </ul>
              <p className="guide-where"><span className="label">Где в UI</span> {step.where}</p>
              {step.tip && <div className="guide-tip">{step.tip}</div>}
            </div>

            {(step.karbyz || step.yaml) && (
              <div className="panel">
                <h2 style={{ marginTop: 0 }}>Один смысл — два синтаксиса</h2>
                <div className="guide-dialects">
                  {step.karbyz && (
                    <div>
                      <div className="label">Карбыз (основной)</div>
                      {step.karbyz.note && <p className="muted" style={{ margin: '0.35rem 0' }}>{step.karbyz.note}</p>}
                      <pre className="docs-code karbyz-src">{step.karbyz.code}</pre>
                    </div>
                  )}
                  {step.yaml && (
                    <div>
                      <div className="label">Watermelon YAML (эквивалент)</div>
                      {step.yaml.note && <p className="muted" style={{ margin: '0.35rem 0' }}>{step.yaml.note}</p>}
                      <pre className="docs-code karbyz-src">{step.yaml.code}</pre>
                    </div>
                  )}
                </div>
                {step.id === 'full' && (
                  <>
                    <div className="btn-row" style={{ marginTop: '0.85rem', flexWrap: 'wrap' }}>
                      <button
                        className={`btn ${compileLang === 'karbyz' ? 'primary' : ''}`}
                        onClick={() => setCompileLang('karbyz')}
                        type="button"
                      >Компилировать Карбыз</button>
                      <button
                        className={`btn ${compileLang === 'yaml' ? 'primary' : ''}`}
                        onClick={() => setCompileLang('yaml')}
                        type="button"
                      >Компилировать YAML</button>
                      <button
                        className="btn"
                        disabled={!projectId || busy}
                        onClick={() => compile(compileLang === 'yaml' ? STOREFRONT_YAML : STOREFRONT_KARBYZ)}
                      >{busy ? '…' : 'Compile → Jenkinsfile'}</button>
                      <button
                        className="btn primary"
                        disabled={!projectId || busy}
                        onClick={() => runPipeline(compileLang === 'yaml' ? STOREFRONT_YAML : STOREFRONT_KARBYZ)}
                      >{busy ? '…' : 'Запустить pipeline'}</button>
                    </div>
                    {!projectId && <p className="muted">Сначала откройте демо (кнопка «Демо»).</p>}
                    {preview && (
                      <>
                        <div className="label" style={{ marginTop: '0.85rem' }}>Jenkinsfile</div>
                        <div className="codebox">{preview}</div>
                      </>
                    )}
                  </>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {mode === 'yaml' && (
        <div className="grid two">
          <div className="panel form">
            <label>Шаблон
              <select
                value={selected}
                onChange={(e) => {
                  setSelected(e.target.value)
                  setYaml(templates[e.target.value] || '')
                  setPreview('')
                }}
              >
                {Object.keys(templates).length === 0 && <option value="storefront">storefront</option>}
                {Object.keys(templates).map((k) => <option key={k} value={k}>{k}</option>)}
              </select>
            </label>
            <label>Манифест (YAML)<textarea value={yaml} onChange={(e) => setYaml(e.target.value)} /></label>
            <div className="btn-row">
              <button className="btn" disabled={!projectId || busy || !yaml.trim()} onClick={() => compile(yaml)}>
                {busy ? '…' : 'Compile → Jenkinsfile'}
              </button>
              <button className="btn primary" disabled={!projectId || busy || !yaml.trim()} onClick={() => runPipeline(yaml)}>
                Run pipeline
              </button>
            </div>
            {!projectId && <div className="muted">Откройте демо-проект.</div>}
          </div>
          <div className="panel">
            <h2>Jenkinsfile</h2>
            <div className="codebox">{preview || '// Compile preview'}</div>
          </div>
        </div>
      )}

      {mode === 'karbyz' && (
        <div className="karbyz-layout">
          <aside className="karbyz-list panel">
            <h2>Примеры .kbz</h2>
            <button
              className={`karbyz-item ${karbyzId === 'devops' ? 'active' : ''}`}
              onClick={() => { setKarbyzId('devops'); setPreview('') }}
            >
              <strong>Storefront CI-манифест</strong>
              <span className="muted">Полный цикл на Карбызе</span>
            </button>
            {KARBYZ_EXAMPLES.filter((e) => e.id !== 'devops').map((item) => (
              <button
                key={item.id}
                className={`karbyz-item ${item.id === karbyz.id ? 'active' : ''}`}
                onClick={() => { setKarbyzId(item.id); setPreview('') }}
              >
                <strong>{item.title}</strong>
                <span className="muted">{item.summary}</span>
              </button>
            ))}
          </aside>
          <div className="karbyz-detail">
            <div className="panel">
              <h2 style={{ marginTop: 0 }}>
                {karbyzId === 'devops' ? 'Storefront · Карбыз' : karbyz.title}
              </h2>
              <p className="muted">
                {karbyzId === 'devops'
                  ? 'Тот же манифест Watermelon CI, что YAML storefront — татарский синтаксис.'
                  : karbyz.summary}
              </p>
              <pre className="docs-code karbyz-src">
                {karbyzId === 'devops' ? STOREFRONT_KARBYZ : karbyz.source}
              </pre>
              {karbyzId !== 'devops' && karbyz.expected && (
                <>
                  <div className="label" style={{ marginTop: '0.85rem' }}>Ожидаемый результат</div>
                  <pre className="docs-code karbyz-out">{karbyz.expected}</pre>
                </>
              )}
            </div>
            <div className="panel form">
              {(karbyzId === 'devops' || karbyz.pipeline) ? (
                <>
                  <h2>Compile / Run</h2>
                  <p className="muted" style={{ marginTop: 0 }}>
                    Платформа разбирает <code>.kbz</code> напрямую → Jenkinsfile.
                  </p>
                  <div className="btn-row">
                    <button
                      className="btn"
                      disabled={!projectId || busy}
                      onClick={() => compile(karbyzId === 'devops' ? STOREFRONT_KARBYZ : karbyz.source)}
                    >Compile</button>
                    <button
                      className="btn primary"
                      disabled={!projectId || busy}
                      onClick={() => runPipeline(karbyzId === 'devops' ? STOREFRONT_KARBYZ : karbyz.source)}
                    >Run pipeline</button>
                  </div>
                  {preview && <div className="codebox" style={{ marginTop: 12 }}>{preview}</div>}
                </>
              ) : (
                <>
                  <h2>Синтаксис</h2>
                  <p className="muted" style={{ marginTop: 0 }}>
                    Для пайплайна откройте Storefront CI-манифест или вкладку «Гайд».
                  </p>
                  <Link className="btn ghost" to="/app/docs">Спецификация</Link>
                  <Link className="btn primary" to="/app/ide" style={{ marginLeft: 8 }}>Открыть IDE</Link>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  )
}

function FleetPage({ orgId }: { orgId: string }) {
  const [clusters, setClusters] = useState<Cluster[]>([])
  const [selected, setSelected] = useState<string | null>(null)
  const [containers, setContainers] = useState<Container[]>([])
  const [nodes, setNodes] = useState<WorkerNode[]>([])
  const [logs, setLogs] = useState<string | null>(null)
  const [name, setName] = useState('production-fleet')
  const [nodeForm, setNodeForm] = useState({ name: 'worker-1', engineUrl: 'unix:///var/run/docker.sock' })
  const [joinHint, setJoinHint] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const reloadClusters = async () => {
    const list = await api.listClusters(orgId)
    setClusters(list)
    if (!selected && list[0]) setSelected(list[0].id)
    if (selected && !list.find((c) => c.id === selected) && list[0]) setSelected(list[0].id)
  }

  const reloadDetail = async (clusterId: string) => {
    const [c, n] = await Promise.all([
      api.listContainers(clusterId).catch(() => [] as Container[]),
      api.listNodes(clusterId).catch(() => [] as WorkerNode[]),
    ])
    setContainers(c)
    setNodes(n)
  }

  useEffect(() => {
    reloadClusters().catch((e) => setError(String(e.message || e)))
  }, [orgId])

  useEffect(() => {
    if (!selected) return
    reloadDetail(selected).catch((e) => setError(String(e.message || e)))
  }, [selected])

  const act = async (c: Container, action: string) => {
    if (!selected) return
    setBusy(true); setError(null)
    try {
      await api.containerAction(selected, c.nodeId, c.id, action)
      await reloadDetail(selected)
      await reloadClusters()
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div className="topbar">
        <div><h1>Fleet console</h1><p>Clusters, workers and containers — manage without leaving CI.</p></div>
        <button
          className="btn"
          disabled={!selected || busy}
          onClick={() => selected && reloadDetail(selected).catch((e) => setError(String(e.message || e)))}
        >
          Refresh
        </button>
      </div>
      {error && <div className="error">{error}</div>}
      <div className="grid two">
        <div className="panel">
          <h2>Clusters</h2>
          {clusters.length === 0 ? <div className="empty">Add a cluster to start managing workers.</div> : (
            <table className="table">
              <thead><tr><th>Name</th><th>Kind</th><th>Nodes</th><th>Containers</th></tr></thead>
              <tbody>
                {clusters.map((c) => (
                  <tr
                    key={c.id}
                    style={{ cursor: 'pointer', background: selected === c.id ? 'rgba(62,207,122,0.08)' : undefined }}
                    onClick={() => { setSelected(c.id); setLogs(null); setJoinHint(null) }}
                  >
                    <td>{c.name}</td>
                    <td className="mono">{c.kind}</td>
                    <td>{c.onlineNodes}/{c.nodeCount}</td>
                    <td>{c.containerCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <div className="form" style={{ marginTop: 16 }}>
            <h2>Add cluster</h2>
            <label>Name<input value={name} onChange={(e) => setName(e.target.value)} /></label>
            <button
              className="btn primary"
              disabled={!name.trim() || busy}
              onClick={async () => {
                setBusy(true)
                try {
                  const c = await api.createCluster(orgId, name.trim())
                  setName('')
                  await reloadClusters()
                  setSelected(c.id)
                } catch (e) { setError(e instanceof Error ? e.message : String(e)) }
                finally { setBusy(false) }
              }}
            >Create</button>
          </div>
        </div>

        <div className="panel form">
          <h2>Add worker node</h2>
          {!selected ? <div className="muted">Select a cluster first.</div> : (
            <>
              <label>Name<input value={nodeForm.name} onChange={(e) => setNodeForm({ ...nodeForm, name: e.target.value })} /></label>
              <label>Engine URL<input value={nodeForm.engineUrl} onChange={(e) => setNodeForm({ ...nodeForm, engineUrl: e.target.value })} /></label>
              <button
                className="btn primary"
                disabled={busy}
                onClick={async () => {
                  if (!selected) return
                  setBusy(true); setError(null)
                  try {
                    const n = await api.addNode(selected, nodeForm.name, nodeForm.engineUrl)
                    setJoinHint(n.installHint || `Join token: ${n.joinToken}`)
                    await reloadDetail(selected)
                    await reloadClusters()
                  } catch (e) { setError(e instanceof Error ? e.message : String(e)) }
                  finally { setBusy(false) }
                }}
              >Register node</button>
              {joinHint && <div className="codebox">{joinHint}</div>}
            </>
          )}
        </div>
      </div>

      {selected && (
        <>
          <div className="panel rise">
            <h2>Nodes</h2>
            {nodes.length === 0 ? <div className="empty">No workers registered on this cluster.</div> : (
              <table className="table">
                <thead><tr><th>Name</th><th>Role</th><th>Status</th><th>Host</th></tr></thead>
                <tbody>
                  {nodes.map((n) => (
                    <tr key={n.id}>
                      <td>{n.name}</td>
                      <td className="mono">{n.role}</td>
                      <td>{badge(n.status)}</td>
                      <td className="mono muted">{n.hostname || n.engineUrl || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          <div className="panel rise rise-delay-1">
            <h2>Containers</h2>
            {containers.length === 0 ? (
              <div className="empty">No containers visible — join an online node with Docker access.</div>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Image</th>
                    <th>State</th>
                    <th>Node</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {containers.map((c) => (
                    <tr key={`${c.nodeId}-${c.id}`}>
                      <td>
                        <div>{c.name}</div>
                        <div className="mono muted">{c.shortId}</div>
                      </td>
                      <td className="mono muted">{c.image}</td>
                      <td>{badge(c.state)}</td>
                      <td className="mono">{c.nodeName}</td>
                      <td>
                        <div className="btn-row">
                          <button className="btn" disabled={busy} onClick={() => act(c, 'start')}>Start</button>
                          <button className="btn" disabled={busy} onClick={() => act(c, 'stop')}>Stop</button>
                          <button className="btn" disabled={busy} onClick={() => act(c, 'restart')}>Restart</button>
                          <button
                            className="btn"
                            disabled={busy}
                            onClick={async () => {
                              setBusy(true); setError(null)
                              try {
                                const r = await api.containerLogs(selected, c.nodeId, c.id)
                                setLogs(`# ${c.name}\n${r.logs || '(empty)'}`)
                              } catch (e) { setError(e instanceof Error ? e.message : String(e)) }
                              finally { setBusy(false) }
                            }}
                          >Logs</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            {logs && (
              <div style={{ marginTop: 12 }}>
                <div className="btn-row" style={{ marginBottom: 8, justifyContent: 'space-between' }}>
                  <h2 style={{ margin: 0 }}>Logs</h2>
                  <button className="btn ghost" onClick={() => setLogs(null)}>Close</button>
                </div>
                <div className="codebox">{logs}</div>
              </div>
            )}
          </div>
        </>
      )}
    </>
  )
}

function RequireOrg({ children }: { children: (orgId: string) => React.ReactNode }) {
  const orgId = getStoredOrgId()
  if (!orgId) return <Navigate to="/" replace />
  return <>{children(orgId)}</>
}

/** Спека / IDE доступны и без демо — с оболочкой, если org уже есть. */
function SoftShell({ children }: { children: React.ReactNode }) {
  const orgId = getStoredOrgId()
  if (orgId) return <Shell orgId={orgId}>{children}</Shell>
  return (
    <div className="docs">
      <header className="docs-top">
        <Link to="/" className="mark">
          <span className="mark-orb" />
          <span className="logo">Water<span>melon</span></span>
        </Link>
        <div className="docs-top-meta">
          <Link className="btn ghost" to="/app/ide">Карбыз IDE</Link>
          <Link className="btn ghost" to="/app/docs">Спецификация</Link>
          <Link className="btn primary" to="/">Демо</Link>
        </div>
      </header>
      <div className="main" style={{ maxWidth: 1200, margin: '0 auto', paddingBottom: '2.5rem' }}>{children}</div>
    </div>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/docs/karbyz" element={<Navigate to="/app/docs" replace />} />
      <Route path="/examples/karbyz" element={<Navigate to="/app/ide" replace />} />
      <Route path="/app" element={<RequireOrg>{(orgId) => <Shell orgId={orgId}><Overview orgId={orgId} /></Shell>}</RequireOrg>} />
      <Route path="/app/projects" element={<RequireOrg>{(orgId) => <Shell orgId={orgId}><ProjectsPage orgId={orgId} /></Shell>}</RequireOrg>} />
      <Route path="/app/projects/:projectId" element={<RequireOrg>{(orgId) => <Shell orgId={orgId}><ProjectPage /></Shell>}</RequireOrg>} />
      <Route path="/app/studio" element={<RequireOrg>{(orgId) => <Shell orgId={orgId}><StudioPage /></Shell>}</RequireOrg>} />
      <Route path="/app/ide" element={<SoftShell><KarbyzIdePage /></SoftShell>} />
      <Route path="/app/docs" element={<SoftShell><DocsPage embedded /></SoftShell>} />
      <Route path="/app/karbyz" element={<Navigate to="/app/ide" replace />} />
      <Route path="/app/fleet" element={<RequireOrg>{(orgId) => <Shell orgId={orgId}><FleetPage orgId={orgId} /></Shell>}</RequireOrg>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
