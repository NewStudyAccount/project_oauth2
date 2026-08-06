/**
 * OAuth2 认证模块
 * 封装 OAuth2 PKCE 授权码流程
 */
import { generateCodeVerifier, generateCodeChallenge, generateState } from './pkce.js'

// OAuth2 配置
const AUTH_CONFIG = {
  authorizationEndpoint: 'http://auth.local:9000/oauth2/authorize',
  tokenEndpoint: 'http://auth.local:9000/oauth2/token',
  revocationEndpoint: 'http://auth.local:9000/oauth2/revoke',
  userinfoEndpoint: 'http://auth.local:9000/userinfo',
  clientId: 'vue-app',
  redirectUri: 'http://app-a.local:5173/callback',
  scope: 'openid profile email'
}

// sessionStorage 键名
const STORAGE_KEYS = {
  CODE_VERIFIER: 'oauth2_code_verifier',
  STATE: 'oauth2_state',
  ACCESS_TOKEN: 'oauth2_access_token',
  ID_TOKEN: 'oauth2_id_token',
  REFRESH_TOKEN: 'oauth2_refresh_token'
}

/**
 * 发起 OAuth2 登录 (跳转到认证中心)
 */
export async function login() {
  const codeVerifier = generateCodeVerifier()
  const codeChallenge = await generateCodeChallenge(codeVerifier)
  const state = generateState()

  // 存储到 sessionStorage，回调时使用
  sessionStorage.setItem(STORAGE_KEYS.CODE_VERIFIER, codeVerifier)
  sessionStorage.setItem(STORAGE_KEYS.STATE, state)

  // 构建授权请求 URL
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: AUTH_CONFIG.clientId,
    redirect_uri: AUTH_CONFIG.redirectUri,
    scope: AUTH_CONFIG.scope,
    state: state,
    code_challenge: codeChallenge,
    code_challenge_method: 'S256'
  })

  // 跳转到认证中心
  window.location.href = `${AUTH_CONFIG.authorizationEndpoint}?${params.toString()}`
}

/**
 * 处理授权回调 (用 code 换 token)
 * @param {string} code - 授权码
 * @param {string} state - state 参数
 * @returns {Promise<Object>} - Token 响应
 */
export async function handleCallback(code, state) {
  // 验证 state
  const savedState = sessionStorage.getItem(STORAGE_KEYS.STATE)
  if (state !== savedState) {
    throw new Error('State 参数不匹配，可能存在 CSRF 攻击')
  }

  // 获取 code_verifier
  const codeVerifier = sessionStorage.getItem(STORAGE_KEYS.CODE_VERIFIER)
  if (!codeVerifier) {
    throw new Error('找不到 code_verifier，授权流程异常')
  }

  // 用授权码换 Token
  const response = await fetch(AUTH_CONFIG.tokenEndpoint, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      code: code,
      redirect_uri: AUTH_CONFIG.redirectUri,
      client_id: AUTH_CONFIG.clientId,
      code_verifier: codeVerifier
    })
  })

  if (!response.ok) {
    const error = await response.json()
    throw new Error(error.error_description || 'Token 请求失败')
  }

  const tokens = await response.json()

  // 存储 Token
  saveTokens(tokens)

  // 清理临时数据
  sessionStorage.removeItem(STORAGE_KEYS.CODE_VERIFIER)
  sessionStorage.removeItem(STORAGE_KEYS.STATE)

  return tokens
}

/**
 * 刷新 access_token
 * @returns {Promise<Object>} - 新的 Token
 */
export async function refreshToken() {
  const refreshToken = sessionStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN)
  if (!refreshToken) {
    throw new Error('没有 refresh_token')
  }

  const response = await fetch(AUTH_CONFIG.tokenEndpoint, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: new URLSearchParams({
      grant_type: 'refresh_token',
      refresh_token: refreshToken,
      client_id: AUTH_CONFIG.clientId
    })
  })

  if (!response.ok) {
    // refresh_token 也失效，需要重新登录
    clearTokens()
    throw new Error('Refresh token 失效，需要重新登录')
  }

  const tokens = await response.json()
  saveTokens(tokens)
  return tokens
}

/**
 * 撤销 Token (登出)
 */
export async function revokeToken() {
  const refreshToken = sessionStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN)
  if (refreshToken) {
    try {
      await fetch(AUTH_CONFIG.revocationEndpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: new URLSearchParams({
          token: refreshToken,
          client_id: AUTH_CONFIG.clientId
        })
      })
    } catch (e) {
      console.warn('Token 撤销失败:', e)
    }
  }
  clearTokens()
}

/**
 * 保存 Token
 */
function saveTokens(tokens) {
  if (tokens.access_token) {
    sessionStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, tokens.access_token)
  }
  if (tokens.id_token) {
    sessionStorage.setItem(STORAGE_KEYS.ID_TOKEN, tokens.id_token)
  }
  if (tokens.refresh_token) {
    sessionStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, tokens.refresh_token)
  }
}

/**
 * 清除 Token
 */
export function clearTokens() {
  sessionStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN)
  sessionStorage.removeItem(STORAGE_KEYS.ID_TOKEN)
  sessionStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN)
}

/**
 * 获取 access_token
 * @returns {string|null}
 */
export function getAccessToken() {
  return sessionStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)
}

/**
 * 获取 id_token
 * @returns {string|null}
 */
export function getIdToken() {
  return sessionStorage.getItem(STORAGE_KEYS.ID_TOKEN)
}

/**
 * 获取 refresh_token
 * @returns {string|null}
 */
export function getRefreshToken() {
  return sessionStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN)
}

/**
 * 检查是否已登录
 * @returns {boolean}
 */
export function isLoggedIn() {
  return !!getAccessToken()
}

/**
 * 解析 JWT Token
 * @param {string} token - JWT token
 * @returns {Object} - payload
 */
export function parseJwt(token) {
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch (e) {
    return null
  }
}

/**
 * 获取用户信息 (从 id_token 解析)
 * @returns {Object|null}
 */
export function getUserInfo() {
  const idToken = getIdToken()
  if (!idToken) return null
  return parseJwt(idToken)
}
