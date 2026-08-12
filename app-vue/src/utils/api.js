/**
 * 外部后端 API 调用工具
 */

const API_BASE_URL = 'http://client.a.local:8082'

/**
 * 通用 API 调用函数
 */
async function apiCall(endpoint, options = {}) {
  const accessToken = localStorage.getItem('access_token')

  const headers = {
    'Content-Type': 'application/json',
    ...options.headers
  }

  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`
  }

  let response
  try {
    response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers
    })
  } catch (error) {
    throw new Error(`Network error: ${error.message}`)
  }

  if (!response.ok) {
    switch (response.status) {
      case 401:
        throw new Error('Unauthorized - Token may be invalid or expired')
      case 403:
        throw new Error('Forbidden - You don\'t have permission to access this resource')
      case 404:
        throw new Error('Not Found - The requested endpoint does not exist')
      case 500:
        throw new Error('Server Error - The server encountered an internal error')
      default:
        throw new Error(`API call failed: ${response.status} ${response.statusText}`)
    }
  }

  return response.json()
}

/**
 * 调用公开 API
 */
export async function getPublicData() {
  return apiCall('/api/public')
}

/**
 * 调用健康检查 API
 */
export async function getHealthStatus() {
  return apiCall('/api/public/health')
}

/**
 * 调用受保护 API
 */
export async function getProtectedData() {
  return apiCall('/api/protected')
}

/**
 * 获取受保护的数据
 */
export async function getProtectedResource() {
  return apiCall('/api/protected/data')
}

/**
 * 获取用户信息
 */
export async function getUserInfoFromBackend() {
  return apiCall('/api/userinfo')
}

/**
 * 获取所有 Token Claims
 */
export async function getTokenClaims() {
  return apiCall('/api/userinfo/claims')
}
