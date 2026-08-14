<template>
  <div class="container">
    <div class="card" style="text-align: center;">
      <h1 style="margin-bottom: 16px;">Vue SPA 应用</h1>
      <p style="color: #666; margin-bottom: 32px;">OAuth2 SSO 单点登录示例</p>

      <div v-if="authStore.isAuthenticated">
        <p style="margin-bottom: 16px;">欢迎回来，{{ authStore.userInfo?.nickname || authStore.userInfo?.username }}</p>
        <router-link to="/profile" class="btn btn-primary">查看个人中心</router-link>
      </div>

      <div v-else>
        <p style="margin-bottom: 16px;">请登录以访问应用</p>
        <button @click="login" class="btn btn-primary">登录</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

function login() {
  authStore.login()
}
</script>
