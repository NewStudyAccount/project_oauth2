<template>
  <div class="container">
    <div class="card">
      <h2 style="margin-bottom: 24px;">个人中心</h2>

      <div v-if="authStore.userInfo">
        <div class="info-row">
          <span class="info-label">用户名</span>
          <span class="info-value">{{ authStore.userInfo.username }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">昵称</span>
          <span class="info-value">{{ authStore.userInfo.nickname || '-' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">邮箱</span>
          <span class="info-value">{{ authStore.userInfo.email || '-' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">手机号</span>
          <span class="info-value">{{ authStore.userInfo.phone || '-' }}</span>
        </div>
      </div>

      <div v-else style="text-align: center; color: #666;">
        <p>加载中...</p>
      </div>

      <div style="margin-top: 32px; text-align: center;">
        <button @click="refreshToken" class="btn btn-primary" style="margin-right: 12px;">刷新 Token</button>
        <button @click="logout" class="btn btn-primary" style="background: #dc2626;">登出</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

onMounted(() => {
  if (!authStore.userInfo) {
    authStore.fetchUserInfo()
  }
})

function refreshToken() {
  authStore.refresh()
  alert('Token 已刷新')
}

function logout() {
  authStore.logout()
}
</script>

<style scoped>
.info-row {
  display: flex;
  padding: 16px 0;
  border-bottom: 1px solid #eee;
}

.info-label {
  width: 100px;
  color: #666;
  font-weight: 500;
}

.info-value {
  flex: 1;
  color: #333;
}
</style>
