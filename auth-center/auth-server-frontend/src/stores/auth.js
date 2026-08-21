import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { generatePKCE, generateRandomString } from '../utils/pkce'
import { buildAuthorizeUrl, exchangeCodeForToken, refreshAccessToken, postLogout } from '../utils/auth'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('access_token'))
  const refreshToken = ref(localStorage.getItem('refresh_token'))

  const isAuthenticated = computed(() => !!accessToken.value)

  /**
   * 发起 OAuth2 PKCE 登录（跳转到认证中心）
   */
  async function login() {
    const { codeVerifier, codeChallenge } = await generatePKCE()
    const state = generateRandomString(32)

    localStorage.setItem('pkce_code_verifier', codeVerifier)
    localStorage.setItem('oauth_state', state)

    window.location.href = buildAuthorizeUrl(codeChallenge, state)
  }

  /**
   * 处理 OAuth2 回调（用 code 换 token）
   */
  async function handleCallback(code, state) {
    const savedState = localStorage.getItem('oauth_state')
    if (state !== savedState) {
      throw new Error('State mismatch')
    }

    const codeVerifier = localStorage.getItem('pkce_code_verifier')
    const tokenResponse = await exchangeCodeForToken(code, codeVerifier)

    accessToken.value = tokenResponse.access_token
    refreshToken.value = tokenResponse.refresh_token

    localStorage.setItem('access_token', tokenResponse.access_token)
    localStorage.setItem('refresh_token', tokenResponse.refresh_token)

    // 清理临时数据
    localStorage.removeItem('pkce_code_verifier')
    localStorage.removeItem('oauth_state')
  }

  /**
   * 刷新 Token
   */
  async function refresh() {
    if (!refreshToken.value) {
      logout()
      return
    }
    try {
      const tokenResponse = await refreshAccessToken(refreshToken.value)
      accessToken.value = tokenResponse.access_token
      refreshToken.value = tokenResponse.refresh_token
      localStorage.setItem('access_token', tokenResponse.access_token)
      localStorage.setItem('refresh_token', tokenResponse.refresh_token)
    } catch (e) {
      console.error('Token refresh failed', e)
      logout()
    }
  }

  /**
   * 登出（清除本地 Token + POST 方式跳转认证中心登出）
   */
  function logout() {
    accessToken.value = null
    refreshToken.value = null
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    postLogout()
  }

  return {
    accessToken,
    refreshToken,
    isAuthenticated,
    login,
    handleCallback,
    refresh,
    logout
  }
})
