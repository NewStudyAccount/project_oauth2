import axios from 'axios'
import { clearLoginCache } from '../router'

const api = axios.create({
  baseURL: '',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.response.use(
  response => {
    // 检测非 JSON 响应（如 Vite 代理跟随 302 返回的登录页 HTML）
    const contentType = response.headers['content-type'] || ''
    if (contentType.includes('text/html')) {
      window.location.href = '/login'
      return Promise.reject(new Error('未登录，正在跳转到登录页'))
    }
    return response
  },
  error => {
    if (error.response && error.response.status === 401) {
      clearLoginCache()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api