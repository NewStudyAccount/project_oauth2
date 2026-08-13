import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('../components/AppLayout.vue'),
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

export default router