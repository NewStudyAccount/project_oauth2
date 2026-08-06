<template>
  <div class="home">
    <div class="card">
      <h1>🔐 OAuth2 SSO 客户端</h1>
      <p class="subtitle">Vue SPA + PKCE 授权码流程</p>

      <div v-if="authStore.isLoggedIn" class="logged-in">
        <div class="user-info">
          <span class="avatar">{{ authStore.username.charAt(0).toUpperCase() }}</span>
          <div>
            <p class="name">{{ authStore.username }}</p>
            <p class="email">{{ authStore.email }}</p>
          </div>
        </div>
        <div class="actions">
          <router-link to="/profile" class="btn btn-primary">查看资料</router-link>
          <button @click="handleLogout" class="btn btn-secondary">退出登录</button>
        </div>
      </div>

      <div v-else class="not-logged-in">
        <p>您尚未登录</p>
        <button @click="handleLogin" class="btn btn-primary">登录</button>
      </div>
    </div>

    <div class="info-card">
      <h3>SSO 演示</h3>
      <p>登录后，访问 <a href="http://app-b.local:8082" target="_blank">app-b.local:8082</a> 无需再次输入密码即可自动登录。</p>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from '../stores/auth.js'

const authStore = useAuthStore()

async function handleLogin() {
  await authStore.login()
}

async function handleLogout() {
  await authStore.logout()
}
</script>

<style scoped>
.home {
  max-width: 600px;
  margin: 80px auto;
  padding: 0 20px;
}
.card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  padding: 40px;
  text-align: center;
}
h1 {
  color: #333;
  margin-bottom: 8px;
}
.subtitle {
  color: #666;
  margin-bottom: 30px;
}
.user-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 24px;
}
.avatar {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
}
.name {
  font-weight: 600;
  color: #333;
}
.email {
  color: #666;
  font-size: 14px;
}
.actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
.btn {
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
  border: none;
}
.btn-primary {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}
.btn-secondary {
  background: #f0f0f0;
  color: #333;
}
.not-logged-in p {
  color: #666;
  margin-bottom: 20px;
}
.info-card {
  margin-top: 24px;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}
.info-card h3 {
  color: #333;
  margin-bottom: 8px;
}
.info-card p {
  color: #666;
  font-size: 14px;
}
.info-card a {
  color: #667eea;
}
</style>
