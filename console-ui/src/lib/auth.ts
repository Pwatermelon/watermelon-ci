const KC_URL = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8083'
const KC_REALM = import.meta.env.VITE_KEYCLOAK_REALM || 'watermelon'
const KC_CLIENT = import.meta.env.VITE_KEYCLOAK_CLIENT || 'watermelon-console'
const TOKEN_KEY = 'wm.accessToken'
const REFRESH_KEY = 'wm.refreshToken'
const PROFILE_KEY = 'wm.userLabel'
const VERIFIER_KEY = 'wm.pkce.verifier'
const AUTH_ENABLED = (import.meta.env.VITE_AUTH_ENABLED || 'true') === 'true'

function b64url(buf: ArrayBuffer) {
  const bytes = new Uint8Array(buf)
  let s = ''
  bytes.forEach((b) => { s += String.fromCharCode(b) })
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

async function pkceChallenge(verifier: string) {
  const data = new TextEncoder().encode(verifier)
  const dig = await crypto.subtle.digest('SHA-256', data)
  return b64url(dig)
}

function randomString(len = 64) {
  const arr = new Uint8Array(len)
  crypto.getRandomValues(arr)
  return b64url(arr.buffer)
}

export function isAuthEnabled() {
  return AUTH_ENABLED
}

export function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getUserLabel() {
  return localStorage.getItem(PROFILE_KEY) || 'user'
}

export function clearTokens() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(PROFILE_KEY)
}

export async function loginWithKeycloak() {
  if (!AUTH_ENABLED) {
    window.location.href = '/'
    return
  }
  const verifier = randomString(64)
  sessionStorage.setItem(VERIFIER_KEY, verifier)
  const challenge = await pkceChallenge(verifier)
  const redirect = `${window.location.origin}/auth/callback`
  const url = new URL(`${KC_URL}/realms/${KC_REALM}/protocol/openid-connect/auth`)
  url.searchParams.set('client_id', KC_CLIENT)
  url.searchParams.set('redirect_uri', redirect)
  url.searchParams.set('response_type', 'code')
  url.searchParams.set('scope', 'openid profile email')
  url.searchParams.set('code_challenge', challenge)
  url.searchParams.set('code_challenge_method', 'S256')
  window.location.href = url.toString()
}

export async function handleAuthCallback(code: string) {
  const verifier = sessionStorage.getItem(VERIFIER_KEY) || ''
  const redirect = `${window.location.origin}/auth/callback`
  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    client_id: KC_CLIENT,
    code,
    redirect_uri: redirect,
    code_verifier: verifier,
  })
  const res = await fetch(`${KC_URL}/realms/${KC_REALM}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })
  if (!res.ok) {
    throw new Error(await res.text())
  }
  const json = await res.json() as { access_token: string; refresh_token?: string }
  localStorage.setItem(TOKEN_KEY, json.access_token)
  if (json.refresh_token) localStorage.setItem(REFRESH_KEY, json.refresh_token)
  try {
    const payload = JSON.parse(atob(json.access_token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    localStorage.setItem(PROFILE_KEY, payload.preferred_username || payload.email || payload.sub || 'user')
  } catch {
    localStorage.setItem(PROFILE_KEY, 'user')
  }
  sessionStorage.removeItem(VERIFIER_KEY)
}

export function logoutKeycloak() {
  clearTokens()
  if (!AUTH_ENABLED) {
    window.location.href = '/'
    return
  }
  const redirect = encodeURIComponent(window.location.origin + '/')
  window.location.href =
    `${KC_URL}/realms/${KC_REALM}/protocol/openid-connect/logout?client_id=${KC_CLIENT}&post_logout_redirect_uri=${redirect}`
}

export function authHeaders(): Record<string, string> {
  const t = getAccessToken()
  return t ? { Authorization: `Bearer ${t}` } : {}
}
