import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { api, Project, setStoredProjectId } from '../lib/api'
import { clearSession } from '../lib/auth'
import { useWorkspace } from '../pages/LandingPage'
import { useEffect, useState } from 'react'

function Mark() {
  return (
    <span className="mark">
      <span className="mark-orb" />
      <span className="logo">Water<span>melon</span></span>
    </span>
  )
}

type NavItem = { to: string; end?: boolean; label: string; hint?: string }
type NavGroup = { title: string; items: NavItem[] }

const NAV: NavGroup[] = [
  {
    title: 'Работа',
    items: [
      { to: '/app', end: true, label: 'Обзор', hint: 'Статус демо' },
      { to: '/app/pipelines', label: 'Пайплайны', hint: 'Сборка и раскатка' },
      { to: '/app/repos', label: 'Репозитории', hint: 'Код и манифесты' },
    ],
  },
  {
    title: 'Среда',
    items: [
      { to: '/app/hosts', label: 'Хосты', hint: 'Demo VM' },
      { to: '/app/services', label: 'Сервисы', hint: 'Контейнеры' },
      { to: '/app/registry', label: 'Образы', hint: 'Registry' },
    ],
  },
  {
    title: 'Справка',
    items: [
      { to: '/app/ide', label: 'Карбыз IDE' },
      { to: '/app/docs', label: 'Документация' },
    ],
  },
]

const PAGE_TITLE: { test: (path: string) => boolean; title: string; blurb: string }[] = [
  { test: (p) => p === '/app', title: 'Обзор', blurb: 'Что запущено и куда идти дальше' },
  { test: (p) => p.startsWith('/app/pipelines'), title: 'Пайплайны', blurb: 'Манифест → Jenkins → стадии' },
  { test: (p) => p.startsWith('/app/repos'), title: 'Репозитории', blurb: 'Исходники демо-сервисов' },
  { test: (p) => p.startsWith('/app/hosts'), title: 'Хосты', blurb: 'Docker VM для раскатки' },
  { test: (p) => p.startsWith('/app/services'), title: 'Сервисы', blurb: 'Контейнеры на хостах' },
  { test: (p) => p.startsWith('/app/registry'), title: 'Образы', blurb: 'Что лежит в registry' },
  { test: (p) => p.startsWith('/app/ide'), title: 'Карбыз IDE', blurb: 'Песочница языка манифестов' },
  { test: (p) => p.startsWith('/app/docs'), title: 'Документация', blurb: 'Спека и примеры' },
]

export function AppShell({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate()
  const location = useLocation()
  const { orgId, projectId } = useWorkspace()
  const [navOpen, setNavOpen] = useState(false)
  const [projects, setProjects] = useState<Project[]>([])

  useEffect(() => {
    setNavOpen(false)
  }, [location.pathname])

  useEffect(() => {
    if (!orgId) return
    api.listProjects(orgId).then(setProjects).catch(() => setProjects([]))
  }, [orgId])

  const page = PAGE_TITLE.find((x) => x.test(location.pathname)) ?? {
    title: 'Консоль',
    blurb: 'Watermelon CI',
  }
  const activeProject = projects.find((p) => p.id === projectId)

  return (
    <div className={`shell${navOpen ? ' shell-nav-open' : ''}`}>
      <aside className="side">
        <div className="side-head">
          <button type="button" className="brand-btn" onClick={() => navigate('/app')}>
            <Mark />
          </button>
          <button
            type="button"
            className="btn ghost btn-sm side-toggle"
            aria-label={navOpen ? 'Скрыть меню' : 'Меню'}
            onClick={() => setNavOpen((v) => !v)}
          >
            {navOpen ? '✕' : '☰'}
          </button>
        </div>

        <nav className="nav side-nav">
          {NAV.map((group) => (
            <div className="nav-group" key={group.title}>
              <div className="nav-group-label">{group.title}</div>
              {group.items.map((l) => (
                <NavLink key={l.to} to={l.to} end={l.end} title={l.hint}>
                  <span className="nav-label">{l.label}</span>
                  {l.hint ? <span className="nav-hint">{l.hint}</span> : null}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        <div className="side-foot">
          <div className="side-foot-label">Проект</div>
          {projects.length > 0 ? (
            <select
              className="side-project"
              value={projectId || ''}
              onChange={(e) => {
                setStoredProjectId(e.target.value)
                window.location.reload()
              }}
            >
              {projects.map((p) => (
                <option key={p.id} value={p.id}>{p.slug}</option>
              ))}
            </select>
          ) : (
            <div className="side-foot-id mono">{activeProject?.slug || 'demo'}</div>
          )}
          <button
            type="button"
            className="btn ghost side-exit"
            onClick={() => {
              clearSession()
              window.location.href = '/'
            }}
          >
            Выйти
          </button>
        </div>
      </aside>

      {navOpen ? (
        <button type="button" className="side-scrim" aria-label="Закрыть меню" onClick={() => setNavOpen(false)} />
      ) : null}

      <div className="main-wrap">
        <div className="app-chrome">
          <div className="app-chrome-left">
            <button
              type="button"
              className="btn ghost btn-sm side-toggle-inline"
              aria-label="Меню"
              onClick={() => setNavOpen(true)}
            >
              ☰
            </button>
            <span className="chrome-pill">CI</span>
            <span className="chrome-sep" />
            <div className="chrome-page">
              <strong>{page.title}</strong>
              <span className="muted chrome-blurb">{page.blurb}</span>
            </div>
          </div>
          <div className="topnav-right">
            <span className="muted mono topnav-user">demo</span>
          </div>
        </div>
        <main className="main">{children}</main>
      </div>
    </div>
  )
}
