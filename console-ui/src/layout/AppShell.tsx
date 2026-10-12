import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { getStoredOrgId, setStoredOrgId, setStoredProjectId } from '../lib/api'
import { clearTokens, getAccessToken, getUserLabel, logoutKeycloak } from '../lib/auth'

function Mark() {
  return (
    <span className="mark">
      <span className="mark-orb" />
      <span className="logo">Water<span>melon</span></span>
    </span>
  )
}

export function AppShell({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const orgId = getStoredOrgId()
  const token = getAccessToken()
  const inApp = pathname.startsWith('/app') && Boolean(orgId || token)
  const user = getUserLabel()

  const logout = () => {
    clearTokens()
    setStoredOrgId('')
    setStoredProjectId('')
    localStorage.removeItem('wm.organizationId')
    localStorage.removeItem('wm.projectId')
    logoutKeycloak()
  }

  return (
    <div className="app-root">
      {inApp ? (
        <header className="topnav">
          <button type="button" className="brand-btn topnav-brand" onClick={() => navigate('/app')}>
            <Mark />
          </button>
          <nav className="topnav-links">
            <NavLink to="/app" end>Обзор</NavLink>
            <NavLink to="/app/repos">Репозитории</NavLink>
            <NavLink to="/app/pipelines">Пайплайны</NavLink>
            <NavLink to="/app/hosts">Хосты</NavLink>
            <NavLink to="/app/services">Сервисы</NavLink>
            <NavLink to="/app/docs">Документация</NavLink>
          </nav>
          <div className="topnav-right">
            <span className="muted mono topnav-user">{user}</span>
            <button type="button" className="btn ghost btn-sm" onClick={logout}>Выйти</button>
          </div>
        </header>
      ) : null}
      <main className={inApp ? 'topnav-main' : 'bare-main'}>{children}</main>
    </div>
  )
}
