import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import HomeView from '../views/HomeView.vue'
import ProfileView from '../views/ProfileView.vue'

const routes = [
  {
    path: '/callback',
    name: 'Callback',
    component: () => import('../views/CallbackView.vue'),
    meta: { requiresAuth: false }
  },
  { path: '/', name: 'Home', component: HomeView, meta: { requiresAuth: false } },
  { path: '/profile', name: 'Profile', component: ProfileView, meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (!to.matched.some(record => record.meta.requiresAuth)) {
    return next()
  }
  const token = localStorage.getItem('access_token')
  if (token) {
    next()
  } else {
    const authStore = useAuthStore()
    authStore.login()
  }
})

export default router
