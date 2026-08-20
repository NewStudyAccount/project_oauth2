import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/callback',
    name: 'Callback',
    component: () => import('../views/CallbackView.vue'),
    meta: { requiresAuth: false }
  },
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

router.beforeEach((to, from, next) => {
  // 不需要认证的路由直接放行
  if (!to.matched.some(record => record.meta.requiresAuth)) {
    return next()
  }

  // 检查 token 是否存在
  const token = localStorage.getItem('access_token')
  if (token) {
    next()
  } else {
    // 未登录，跳转到认证中心
    const authStore = useAuthStore()
    authStore.login()
  }
})

export default router
