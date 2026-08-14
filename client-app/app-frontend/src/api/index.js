import axios from 'axios'

const api = axios.create({
  baseURL: '',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

// CSRF token interceptor
api.interceptors.request.use(config => {
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/)
  if (match) {
    config.headers['X-XSRF-TOKEN'] = decodeURIComponent(match[1])
  }
  return config
})

// 401 response interceptor
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      window.location.href = '/oauth2/authorization/springboot-app'
    }
    return Promise.reject(error)
  }
)

export default api
