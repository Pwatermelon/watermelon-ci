import { NavLink, useNavigate } from 'react-router-dom'
import { clearSession } from '../lib/auth'

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

  return (
    <div className="app-root">
      <header className="topnav">
        <button type="button" className="brand-btn topnav-brand" onClick={() => navigate('/app')}>
          <Mark />
        </button>
        <nav className="topnav-links">
          <NavLink to="/app" end>Обзор</NavLink>
          <NavLink to="/app/repos">Репозитории</NavLink>
            <NavLink to="/app/pipelines">Пайплайны</NavLink>
            <NavLink to="/app/ide">Карбыз IDE</NavLink>
            <NavLink to="/app/hosts">Хосты</NavLink>
            <NavLink to="/app/services">Сервисы</NavLink>
            <NavLink to="/app/docs">Документация</NavLink>
        </nav>
        <div className="topnav-right">
          <span className="muted mono topnav-user">demo</span>
          <button
            type="button"
            className="btn ghost btn-sm"
            onClick={() => {
              clearSession()
              window.location.href = '/'
            }}
          >Выйти</button>
        </div>
      </header>
      <main className="topnav-main">{children}</main>
    </div>
  )
}
