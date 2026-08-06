import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '../utils/auth.js'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/callback',
    name: 'Callback',
    component: () => import('../views/Callback.vue')
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    // 未登录，跳转首页 (首页有登录按钮)
    next({ name: 'Home' })
  } else {
    next()
  }
})

export default router
