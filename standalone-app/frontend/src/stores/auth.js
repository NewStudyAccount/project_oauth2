import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { generatePKCE, generateRandomString } from '../utils/pkce'
import { buildAuthorizeUrl, exchangeCodeForToken, refreshAccessToken, getUserInfo, buildLogoutUrl } from '../utils/auth'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('access_token'))
  const refreshToken = ref(localStorage.getItem('refresh_token'))
  const userInfo = ref(JSON.parse(localStorage.getItem('user_info') || 'null'))

  const isAuthenticated = computed(() => !!accessToken.value)

  async function login() {
    const { codeVerifier, codeChallenge } = await generatePKCE()
    const state = generateRandomString(32)

    localStorage.setItem('pkce_code_verifier', codeVerifier)
    localStorage.setItem('oauth_state', state)

    window.location.href = buildAuthorizeUrl(codeChallenge, state)
  }

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

    // 获取用户信息
    await fetchUserInfo()
  }

  async function fetchUserInfo() {
    try {
      const info = await getUserInfo(accessToken.value)
      userInfo.value = info
      localStorage.setItem('user_info', JSON.stringify(info))
    } catch (e) {
      console.error('Failed to fetch user info', e)
    }
  }

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

  function logout() {
    accessToken.value = null
    refreshToken.value = null
    userInfo.value = null
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    localStorage.removeItem('user_info')
    window.location.href = buildLogoutUrl()
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    isAuthenticated,
    login,
    handleCallback,
    fetchUserInfo,
    refresh,
    logout
  }
})
