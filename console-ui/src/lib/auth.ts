const KC_URL = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8083'
const KC_REALM = import.meta.env.VITE_KEYCLOAK_REALM || 'watermelon'
const KC_CLIENT = import.meta.env.VITE_KEYCLOAK_CLIENT || 'watermelon-console'
const TOKEN_KEY = 'wm.accessToken'
const REFRESH_KEY = 'wm.refreshToken'
const PROFILE_KEY = 'wm.userLabel'
const VERIFIER_KEY = 'wm.pkce.verifier'
const AUTH_ENABLED = (import.meta.env.VITE_AUTH_ENABLED || 'false') === 'true'

function b64urlBytes(bytes: Uint8Array) {
  let s = ''
  bytes.forEach((b) => { s += String.fromCharCode(b) })
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function randomString(len = 64) {
  const arr = new Uint8Array(len)
  crypto.getRandomValues(arr)
  return b64urlBytes(arr)
}

async function pkceChallenge(verifier: string) {
  const data = new TextEncoder().encode(verifier)
  if (globalThis.crypto?.subtle?.digest) {
    const dig = await crypto.subtle.digest('SHA-256', data)
    return b64urlBytes(new Uint8Array(dig))
  }
  // fallback (non-secure contexts): use S256 via subtle is preferred; otherwise fail clearly
  throw new Error('WebCrypto недоступен — открой http://localhost:8088 (не IP и не file://)')
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

export function clearSession() {
  clearTokens()
  localStorage.removeItem('wm.organizationId')
  localStorage.removeItem('wm.projectId')
}

export async function loginWithKeycloak() {
  if (!AUTH_ENABLED) {
    throw new Error('Keycloak выключен (VITE_AUTH_ENABLED=false)')
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
  window.location.assign(url.toString())
}

export async function handleAuthCallback(code: string) {
  const verifier = sessionStorage.getItem(VERIFIER_KEY) || ''
  const redirect = `${window.location.origin}/auth/callback`
  const res = await fetch('/api/v1/auth/exchange', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code, codeVerifier: verifier, redirectUri: redirect }),
  })
  if (!res.ok) {
    throw new Error(await res.text())
  }
  const json = await res.json() as { access_token: string; refresh_token?: string }
  if (!json.access_token) {
    throw new Error('Нет access_token в ответе')
  }
  localStorage.setItem(TOKEN_KEY, json.access_token)
  if (json.refresh_token) localStorage.setItem(REFRESH_KEY, json.refresh_token)
  try {
    const raw = json.access_token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const payload = JSON.parse(atob(raw.padEnd(raw.length + (4 - raw.length % 4) % 4, '=')))
    localStorage.setItem(PROFILE_KEY, payload.preferred_username || payload.email || payload.sub || 'user')
  } catch {
    localStorage.setItem(PROFILE_KEY, 'user')
  }
  sessionStorage.removeItem(VERIFIER_KEY)
}

export function logoutKeycloak() {
  clearSession()
  if (!AUTH_ENABLED) {
    window.location.href = '/'
    return
  }
  const redirect = encodeURIComponent(window.location.origin + '/')
  window.location.assign(
    `${KC_URL}/realms/${KC_REALM}/protocol/openid-connect/logout?client_id=${KC_CLIENT}&post_logout_redirect_uri=${redirect}`,
  )
}

export function authHeaders(): Record<string, string> {
  const t = getAccessToken()
  return t ? { Authorization: `Bearer ${t}` } : {}
}
