/**
 * OAuth2 认证工具 - 授权 URL 构建、Token 交换、刷新、登出
 */

const AUTH_SERVER = 'http://auth.local:9000'
const CLIENT_ID = 'springboot-app'
const REDIRECT_URI = `${window.location.origin}/callback`
const SCOPES = 'openid profile email'

export function buildAuthorizeUrl(codeChallenge, state) {
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    scope: SCOPES,
    state,
    code_challenge: codeChallenge,
    code_challenge_method: 'S256'
  })
  return `${AUTH_SERVER}/oauth2/authorize?${params.toString()}`
}

export async function exchangeCodeForToken(code, codeVerifier) {
  const params = new URLSearchParams({
    grant_type: 'authorization_code',
    code,
    redirect_uri: REDIRECT_URI,
    client_id: CLIENT_ID,
    code_verifier: codeVerifier
  })

  const response = await fetch(`${AUTH_SERVER}/oauth2/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: params.toString()
  })

  if (!response.ok) {
    throw new Error(`Token exchange failed: ${response.status}`)
  }
  return response.json()
}

export async function refreshAccessToken(refreshToken) {
  const params = new URLSearchParams({
    grant_type: 'refresh_token',
    refresh_token: refreshToken,
    client_id: CLIENT_ID
  })

  const response = await fetch(`${AUTH_SERVER}/oauth2/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: params.toString()
  })

  if (!response.ok) {
    throw new Error(`Token refresh failed: ${response.status}`)
  }
  return response.json()
}

export function buildLogoutUrl() {
  return `${AUTH_SERVER}/logout`
}
