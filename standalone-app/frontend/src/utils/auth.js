/**
 * OAuth2 认证工具
 */

const AUTH_SERVER = 'http://auth.local:9000'
const CLIENT_ID = 'vue-app'
const REDIRECT_URI = 'http://client.b.local:5173/callback'

/**
 * 构建授权 URL
 */
export function buildAuthorizeUrl(codeChallenge, state) {
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    scope: 'openid profile email offline_access',
    state: state,
    code_challenge: codeChallenge,
    code_challenge_method: 'S256'
  })

  return `${AUTH_SERVER}/oauth2/authorize?${params.toString()}`
}

/**
 * 用授权码换取 Token
 */
export async function exchangeCodeForToken(code, codeVerifier) {
  const response = await fetch(`${AUTH_SERVER}/oauth2/token`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: CLIENT_ID,
      code: code,
      redirect_uri: REDIRECT_URI,
      code_verifier: codeVerifier
    })
  })

  if (!response.ok) {
    throw new Error('Token exchange failed')
  }

  return response.json()
}

/**
 * 刷新 Token
 */
export async function refreshAccessToken(refreshToken) {
  const response = await fetch(`${AUTH_SERVER}/oauth2/token`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: new URLSearchParams({
      grant_type: 'refresh_token',
      client_id: CLIENT_ID,
      refresh_token: refreshToken
    })
  })

  if (!response.ok) {
    throw new Error('Token refresh failed')
  }

  return response.json()
}

/**
 * 获取用户信息
 */
export async function getUserInfo(accessToken) {
  const response = await fetch(`${AUTH_SERVER}/userinfo`, {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  })

  if (!response.ok) {
    throw new Error('Failed to fetch user info')
  }

  return response.json()
}

/**
 * 构建登出 URL
 */
export function buildLogoutUrl() {
  return `${AUTH_SERVER}/logout`
}
