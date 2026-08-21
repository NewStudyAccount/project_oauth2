/**
 * OAuth2 认证工具 - 授权 URL 构建、Token 交换、刷新、登出
 */

const AUTH_SERVER = 'http://auth.local:9000'
const CLIENT_ID = 'admin-frontend'
const REDIRECT_URI = `${window.location.origin}/callback`
const SCOPES = 'openid profile email'

/**
 * 构建 OAuth2 授权 URL
 */
export function buildAuthorizeUrl(codeChallenge, state) {
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    scope: SCOPES,
    state: state,
    code_challenge: codeChallenge,
    code_challenge_method: 'S256'
  })
  return `${AUTH_SERVER}/oauth2/authorize?${params.toString()}`
}

/**
 * 用授权码交换 Token
 */
export async function exchangeCodeForToken(code, codeVerifier) {
  const params = new URLSearchParams({
    grant_type: 'authorization_code',
    code: code,
    redirect_uri: REDIRECT_URI,
    client_id: CLIENT_ID,
    code_verifier: codeVerifier
  })

  const response = await fetch(`${AUTH_SERVER}/oauth2/token`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: params.toString()
  })

  if (!response.ok) {
    throw new Error(`Token exchange failed: ${response.status}`)
  }

  return response.json()
}

/**
 * 刷新 Token
 */
export async function refreshAccessToken(refreshToken) {
  const params = new URLSearchParams({
    grant_type: 'refresh_token',
    refresh_token: refreshToken,
    client_id: CLIENT_ID
  })

  const response = await fetch(`${AUTH_SERVER}/oauth2/token`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: params.toString()
  })

  if (!response.ok) {
    throw new Error(`Token refresh failed: ${response.status}`)
  }

  return response.json()
}

/**
 * POST 方式登出（动态创建 form 表单提交，绕过 CORS 限制）
 */
export function postLogout() {
  const form = document.createElement('form')
  form.method = 'POST'
  form.action = `${AUTH_SERVER}/logout`
  document.body.appendChild(form)
  form.submit()
}
