import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  api, getStoredOrgId, setGitOwner, setStoredOrgId, setStoredProjectId,
} from '../lib/api'
import { clearSession, handleAuthCallback, isAuthEnabled, loginWithKeycloak } from '../lib/auth'

export async function bootDemoWorkspace() {
  const demo = await api.seedDemo()
  setStoredOrgId(demo.organizationId)
  setStoredProjectId(demo.projectId)
  setGitOwner(demo.gitOwner || demo.organizationSlug || 'watermelon-demo')
  return demo
}

export default function LandingPage() {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const enter = async () => {
    setBusy(true)
    setError(null)
    try {
      if (isAuthEnabled()) {
        await loginWithKeycloak()
        return
      }
      localStorage.removeItem('wm.accessToken')
      localStorage.removeItem('wm.refreshToken')
      await bootDemoWorkspace()
      window.location.href = '/app'
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
      setBusy(false)
    }
  }

  return (
    <div className="landing">
      <div className="landing-glow" aria-hidden />
      <section className="landing-hero landing-hero-solo">
        <div className="landing-copy">
          <p className="brand-lockup rise">Water<span>melon</span></p>
          <h1 className="rise rise-delay-1">CI/CD консоль</h1>
          <p className="rise rise-delay-2">
            Репозитории, пайплайны, хосты и сервисы — одна панель.
          </p>
          <div className="cta-row rise rise-delay-3">
            <button className="btn primary" type="button" disabled={busy} onClick={() => void enter()}>
              {busy ? 'Загрузка…' : 'Открыть консоль'}
            </button>
          </div>
          <p className="landing-aside rise rise-delay-3">
            <Link to="/app/docs">Спецификация Карбыз 1.4</Link>
          </p>
          {error && <div className="error" style={{ marginTop: 16 }}>{error}</div>}
        </div>
      </section>
    </div>
  )
}

export function AuthCallbackPage() {
  const [error, setError] = useState<string | null>(null)
  const [phase, setPhase] = useState('Обмен кода…')
  const ran = useRef(false)

  useEffect(() => {
    if (ran.current) return
    ran.current = true
    const code = new URLSearchParams(window.location.search).get('code')
    if (!code) {
      setError('Нет code в callback')
      return
    }
    void (async () => {
      try {
        setPhase('Обмен кода…')
        await handleAuthCallback(code)
        setPhase('Создаём workspace…')
        await bootDemoWorkspace()
        window.location.href = '/app'
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e))
      }
    })()
  }, [])

  return (
    <div className="landing landing-hero-solo" style={{ display: 'grid', placeItems: 'center', minHeight: '100vh' }}>
      <div className="panel" style={{ maxWidth: 420 }}>
        {error ? (
          <>
            <div className="error">{error}</div>
            <a className="btn primary" href="/" style={{ marginTop: 12, display: 'inline-block' }}>На главную</a>
          </>
        ) : (
          <p className="muted">{phase}</p>
        )}
      </div>
    </div>
  )
}

export function RequireWorkspace({ children }: { children: React.ReactNode }) {
  const [ready, setReady] = useState(() => Boolean(getStoredOrgId()))
  const [error, setError] = useState<string | null>(null)
  const ran = useRef(false)

  useEffect(() => {
    if (ready || ran.current) return
    ran.current = true
    void bootDemoWorkspace()
      .then(() => setReady(true))
      .catch((e) => setError(e instanceof Error ? e.message : String(e)))
  }, [ready])

  if (error) {
    return (
      <div className="panel">
        <div className="error">{error}</div>
        <a className="btn primary" href="/" style={{ marginTop: 12, display: 'inline-block' }} onClick={() => clearSession()}>
          На главную
        </a>
      </div>
    )
  }
  if (!ready) {
    return <div className="panel"><p className="muted">Готовим workspace…</p></div>
  }
  return <>{children}</>
}

export function useWorkspace() {
  return {
    orgId: getStoredOrgId() || '',
    projectId: localStorage.getItem('wm.projectId') || '',
  }
}
