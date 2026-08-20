import axios from 'axios'
import { useAuthStore } from '../stores/auth'

const api = axios.create({
  baseURL: '',
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：自动添加 Bearer Token
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器：401 时触发登录
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      // Token 无效或过期，清除本地 token 并跳转登录
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
      const authStore = useAuthStore()
      authStore.login()
    }
    return Promise.reject(error)
  }
)

export default api
