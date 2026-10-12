import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  api, getStoredOrgId, setGitOwner, setStoredOrgId, setStoredProjectId,
} from '../lib/api'
import { clearSession, getAccessToken, handleAuthCallback, isAuthEnabled, loginWithKeycloak } from '../lib/auth'

function hasSession() {
  if (isAuthEnabled()) return Boolean(getAccessToken())
  return Boolean(getStoredOrgId())
}

export default function LandingPage() {
  const navigate = useNavigate()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const authOn = isAuthEnabled()

  useEffect(() => {
    // Drop stale workspace left without JWT
    if (authOn && getStoredOrgId() && !getAccessToken()) {
      clearSession()
    }
    if (hasSession() && getStoredOrgId()) {
      navigate('/app', { replace: true })
    }
  }, [navigate, authOn])

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

  const primary = () => {
    if (authOn) {
      loginWithKeycloak()
      return
    }
    void enterDemo()
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
            <button className="btn primary" type="button" disabled={busy} onClick={primary}>
              {busy ? 'Загрузка…' : authOn ? 'Войти' : 'Открыть демо'}
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
        const demo = await api.seedDemo()
        setStoredOrgId(demo.organizationId)
        setStoredProjectId(demo.projectId)
        setGitOwner(demo.gitOwner || 'watermelon-demo')
        navigate('/app', { replace: true })
      })
      .catch((e) => setError(e instanceof Error ? e.message : String(e)))
  }, [navigate])

  return (
    <div className="landing landing-hero-solo" style={{ display: 'grid', placeItems: 'center', minHeight: '100vh' }}>
      <div className="panel" style={{ maxWidth: 420 }}>
        {error ? (
          <>
            <div className="error">{error}</div>
            <button className="btn primary" type="button" style={{ marginTop: 12 }} onClick={() => loginWithKeycloak()}>
              Войти снова
            </button>
          </>
        ) : (
          <p className="muted">Вход…</p>
        )}
      </div>
    </div>
  )
}

export function RequireWorkspace({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate()
  const orgId = getStoredOrgId()
  const token = getAccessToken()
  const authOn = isAuthEnabled()

  useEffect(() => {
    if (authOn && !token) {
      clearSession()
      navigate('/', { replace: true })
      return
    }
    if (!orgId && authOn && token) {
      // BootWorkspace below
      return
    }
    if (!orgId && !authOn) {
      navigate('/', { replace: true })
    }
  }, [orgId, token, authOn, navigate])

  if (authOn && !token) return null
  if (!orgId && authOn && token) return <BootWorkspace />
  if (!orgId) return null
  return <>{children}</>
}

function BootWorkspace() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.seedDemo()
      .then((demo) => {
        setStoredOrgId(demo.organizationId)
        setStoredProjectId(demo.projectId)
        setGitOwner(demo.gitOwner || 'watermelon-demo')
        navigate('/app', { replace: true })
      })
      .catch((e) => setError(e instanceof Error ? e.message : String(e)))
  }, [navigate])

  return (
    <div className="panel">
      {error ? <div className="error">{error}</div> : <p className="muted">Готовим workspace…</p>}
    </div>
  )
}

export function useWorkspace() {
  return {
    orgId: getStoredOrgId() || '',
    projectId: getStoredProjectId() || '',
  }
}
