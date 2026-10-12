import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { clearSession, getAccessToken, getUserLabel, isAuthEnabled, logoutKeycloak } from '../lib/auth'

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
  const token = getAccessToken()
  const authOn = isAuthEnabled()
  const showNav = pathname.startsWith('/app') && (authOn ? Boolean(token) : true)
  const user = getUserLabel()

  const logout = () => {
    clearSession()
    if (authOn) logoutKeycloak()
    else navigate('/')
  }

  return (
    <div className="app-root">
      {showNav ? (
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
      <main className={showNav ? 'topnav-main' : 'bare-main'}>{children}</main>
    </div>
  )
}
