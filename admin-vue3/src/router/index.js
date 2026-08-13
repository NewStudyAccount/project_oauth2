import { createRouter, createWebHistory } from 'vue-router'
import axios from 'axios'

const routes = [
  {
    path: '/',
    component: () => import('../components/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Dashboard', component: () => import('../views/DashboardView.vue') },
      { path: 'clients', name: 'Clients', component: () => import('../views/ClientListView.vue') },
      { path: 'clients/add', name: 'ClientAdd', component: () => import('../views/ClientFormView.vue') },
      { path: 'clients/:id/edit', name: 'ClientEdit', component: () => import('../views/ClientFormView.vue') },
      { path: 'users', name: 'Users', component: () => import('../views/UserListView.vue') },
      { path: 'access', name: 'Access', component: () => import('../views/AccessView.vue') },
      { path: 'audit-logs', name: 'AuditLogs', component: () => import('../views/AuditLogView.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录状态缓存：避免每次导航都请求 API
let isLoggedIn = null

async function checkLoginStatus() {
  if (isLoggedIn !== null) return isLoggedIn
  try {
    await axios.get('/api/admin/stats', { withCredentials: true })
    isLoggedIn = true
    return true
  } catch (e) {
    isLoggedIn = false
    return false
  }
}

// 导出给 axios 拦截器调用，401 时清除缓存
export function clearLoginCache() {
  isLoggedIn = false
}

router.beforeEach(async (to, from, next) => {
  // 不需要认证的路由直接放行
  if (!to.matched.some(record => record.meta.requiresAuth)) {
    return next()
  }

  const loggedIn = await checkLoginStatus()
  if (loggedIn) {
    next()
  } else {
    window.location.href = '/login'
  }
})

export default router