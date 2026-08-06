import axios from 'axios'
import { getAccessToken, refreshToken, clearTokens } from './auth.js'

const http = axios.create({
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 是否正在刷新 Token
let isRefreshing = false
let refreshSubscribers = []

/**
 * Token 刷新后，重新发起等待中的请求
 */
function onRefreshed(newToken) {
  refreshSubscribers.forEach(callback => callback(newToken))
  refreshSubscribers = []
}

/**
 * 添加到等待队列
 */
function addRefreshSubscriber(callback) {
  refreshSubscribers.push(callback)
}

// 请求拦截器: 自动附加 access_token
http.interceptors.request.use(
  config => {
    const token = getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器: 401 自动刷新 Token
http.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config

    // 如果是 401 且不是刷新请求本身，且没有重试过
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 正在刷新，将请求加入队列
        return new Promise(resolve => {
          addRefreshSubscriber(newToken => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            resolve(http(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const tokens = await refreshToken()
        isRefreshing = false
        onRefreshed(tokens.access_token)

        // 重新发起原始请求
        originalRequest.headers.Authorization = `Bearer ${tokens.access_token}`
        return http(originalRequest)
      } catch (refreshError) {
        isRefreshing = false
        // 刷新失败，跳转登录
        clearTokens()
        window.location.href = '/'
        return Promise.reject(refreshError)
      }
    }

    return Promise.reject(error)
  }
)

export default http
