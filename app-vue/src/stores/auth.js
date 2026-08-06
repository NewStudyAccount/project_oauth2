import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  login as authLogin,
  handleCallback as authHandleCallback,
  refreshToken as authRefreshToken,
  revokeToken as authRevokeToken,
  getAccessToken,
  getUserInfo,
  isLoggedIn as checkLoggedIn,
  clearTokens
} from '../utils/auth.js'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const accessToken = ref(getAccessToken())
  const userInfo = ref(getUserInfo())

  // 计算属性
  const isLoggedIn = computed(() => !!accessToken.value)
  const username = computed(() => userInfo.value?.sub || '未知用户')
  const email = computed(() => userInfo.value?.email || '')

  /**
   * 发起登录
   */
  async function login() {
    await authLogin()
  }

  /**
   * 处理回调
   */
  async function handleCallback(code, state) {
    const tokens = await authHandleCallback(code, state)
    accessToken.value = tokens.access_token
    userInfo.value = getUserInfo()
    return tokens
  }

  /**
   * 刷新 Token
   */
  async function refreshToken() {
    const tokens = await authRefreshToken()
    accessToken.value = tokens.access_token
    userInfo.value = getUserInfo()
    return tokens
  }

  /**
   * 登出
   */
  async function logout() {
    await authRevokeToken()
    accessToken.value = null
    userInfo.value = null
  }

  /**
   * 强制清除 (Token 失效时)
   */
  function forceLogout() {
    clearTokens()
    accessToken.value = null
    userInfo.value = null
  }

  return {
    accessToken,
    userInfo,
    isLoggedIn,
    username,
    email,
    login,
    handleCallback,
    refreshToken,
    logout,
    forceLogout
  }
})
