import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  api, getStoredOrgId, getStoredProjectId, setGitOwner, setStoredOrgId, setStoredProjectId,
} from '../lib/api'
import { handleAuthCallback, isAuthEnabled, loginWithKeycloak } from '../lib/auth'

export default function LandingPage() {
  const navigate = useNavigate()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const enterDemo = async () => {
    setBusy(true)
    setError(null)
    try {
      const demo = await api.seedDemo()
      setStoredOrgId(demo.organizationId)
      setStoredProjectId(demo.projectId)
      setGitOwner(demo.gitOwner || demo.organizationSlug || 'watermelon-demo')
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
      <section className="landing-hero landing-hero-solo">
        <div className="landing-copy">
          <p className="brand-lockup rise">Water<span>melon</span></p>
          <h1 className="rise rise-delay-1">CI/CD консоль</h1>
          <p className="rise rise-delay-2">
            Репозитории, пайплайны, хосты Docker и статусы сервисов — в одной панели.
          </p>
          <div className="cta-row rise rise-delay-3">
            {isAuthEnabled() ? (
              <button className="btn primary" type="button" onClick={() => loginWithKeycloak()}>
                Войти через Keycloak
              </button>
            ) : null}
            <button
              className={isAuthEnabled() ? 'btn ghost' : 'btn primary'}
              type="button"
              disabled={busy}
              onClick={enterDemo}
            >
              {busy ? 'Загрузка…' : 'Открыть демо'}
            </button>
            <Link className="btn ghost" to="/app/docs">Спецификация Карбыз</Link>
          </div>
          {error && <div className="error" style={{ marginTop: 16 }}>{error}</div>}
        </div>
      </section>
    </div>
  )
}

export function AuthCallbackPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const code = params.get('code')
    if (!code) {
      setError('Нет code в callback')
      return
    }
    handleAuthCallback(code)
      .then(async () => {
        try {
          const demo = await api.seedDemo()
          setStoredOrgId(demo.organizationId)
          setStoredProjectId(demo.projectId)
          setGitOwner(demo.gitOwner || 'watermelon-demo')
        } catch { /* ignore */ }
        navigate('/app', { replace: true })
      })
      .catch((e) => setError(e instanceof Error ? e.message : String(e)))
  }, [navigate])

  return (
    <div className="topnav-main">
      <div className="panel">{error ? <div className="error">{error}</div> : 'Вход…'}</div>
    </div>
  )
}

export function RequireWorkspace({ children }: { children: React.ReactNode }) {
  const orgId = getStoredOrgId()
  if (!orgId) return <LandingPage />
  return <>{children}</>
}

export function useWorkspace() {
  return {
    orgId: getStoredOrgId() || '',
    projectId: getStoredProjectId() || '',
  }
}
