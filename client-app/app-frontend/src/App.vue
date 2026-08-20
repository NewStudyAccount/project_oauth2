<template>
  <div id="app-layout">
    <nav class="navbar">
      <div class="nav-brand">App SpringBoot</div>
      <div class="nav-links">
        <router-link to="/">首页</router-link>
        <router-link v-if="user" to="/profile">用户资料</router-link>
      </div>
      <div class="nav-auth">
        <template v-if="user">
          <span class="user-name">{{ user.nickname || user.username }}</span>
          <button @click="handleLogout" class="btn btn-logout">登出</button>
        </template>
        <template v-else>
          <button @click="handleLogin" class="btn btn-login">登录</button>
        </template>
      </div>
    </nav>
    <main class="main-content">
      <router-view :user="user" @update-user="user = $event" />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from './stores/auth'
import api from './api'

const authStore = useAuthStore()
const user = ref(null)

const checkAuth = async () => {
  if (!authStore.isAuthenticated) {
    user.value = null
    return
  }
  try {
    const { data } = await api.get('/api/userinfo')
    user.value = data
  } catch {
    user.value = null
  }
}

const handleLogin = () => {
  authStore.login()
}

const handleLogout = () => {
  user.value = null
  authStore.logout()
}

onMounted(checkAuth)
</script>
