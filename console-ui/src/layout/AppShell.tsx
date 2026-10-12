import { NavLink, useNavigate } from 'react-router-dom'
import { getStoredOrgId } from '../lib/api'
import { clearTokens, getAccessToken, getUserLabel, loginWithKeycloak, logoutKeycloak } from '../lib/auth'

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
  const orgId = getStoredOrgId()
  const user = getUserLabel()
  const authed = Boolean(getAccessToken()) || Boolean(orgId)

  return (
    <div className="app-root">
      <header className="topnav">
        <button type="button" className="brand-btn topnav-brand" onClick={() => navigate(authed ? '/app' : '/')}>
          <Mark />
        </button>
        {authed && (
          <nav className="topnav-links">
            <NavLink to="/app" end>Обзор</NavLink>
            <NavLink to="/app/repos">Репозитории</NavLink>
            <NavLink to="/app/pipelines">Пайплайны</NavLink>
            <NavLink to="/app/hosts">Хосты</NavLink>
            <NavLink to="/app/services">Сервисы</NavLink>
            <NavLink to="/app/docs">Документация</NavLink>
          </nav>
        )}
        <div className="topnav-right">
          {authed ? (
            <>
              <span className="muted mono topnav-user">{user}</span>
              <button
                type="button"
                className="btn ghost btn-sm"
                onClick={() => {
                  clearTokens()
                  logoutKeycloak()
                }}
              >Выйти</button>
            </>
          ) : (
            <button type="button" className="btn primary btn-sm" onClick={() => loginWithKeycloak()}>
              Войти
            </button>
          )}
        </div>
      </header>
      <main className="topnav-main">{children}</main>
    </div>
  )
}
