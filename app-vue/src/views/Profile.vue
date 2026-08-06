<template>
  <div class="profile">
    <div class="card">
      <h1>👤 用户资料</h1>
      <p class="subtitle">从 id_token 解析的用户信息</p>

      <div class="info-list">
        <div class="info-item">
          <span class="label">用户名 (sub)</span>
          <span class="value">{{ userInfo?.sub || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="label">签发者 (iss)</span>
          <span class="value">{{ userInfo?.iss || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="label">受众 (aud)</span>
          <span class="value">{{ userInfo?.aud || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="label">过期时间 (exp)</span>
          <span class="value">{{ formatTime(userInfo?.exp) }}</span>
        </div>
        <div class="info-item">
          <span class="label">签发时间 (iat)</span>
          <span class="value">{{ formatTime(userInfo?.iat) }}</span>
        </div>
      </div>

      <div class="token-section">
        <h3>Access Token (JWT)</h3>
        <textarea readonly :value="accessToken" class="token-box"></textarea>
      </div>

      <router-link to="/" class="btn">返回首页</router-link>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '../stores/auth.js'
import { parseJwt } from '../utils/auth.js'

const authStore = useAuthStore()
const userInfo = computed(() => authStore.userInfo)
const accessToken = computed(() => authStore.accessToken)

function formatTime(timestamp) {
  if (!timestamp) return '-'
  return new Date(timestamp * 1000).toLocaleString('zh-CN')
}
</script>

<style scoped>
.profile {
  max-width: 600px;
  margin: 60px auto;
  padding: 0 20px;
}
.card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  padding: 40px;
}
h1 {
  text-align: center;
  color: #333;
  margin-bottom: 8px;
}
.subtitle {
  text-align: center;
  color: #666;
  margin-bottom: 30px;
}
.info-list {
  margin-bottom: 24px;
}
.info-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}
.label {
  color: #666;
  font-size: 14px;
}
.value {
  color: #333;
  font-weight: 500;
  font-size: 14px;
  word-break: break-all;
  max-width: 60%;
  text-align: right;
}
.token-section {
  margin: 24px 0;
}
.token-section h3 {
  color: #333;
  margin-bottom: 8px;
}
.token-box {
  width: 100%;
  height: 120px;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-family: monospace;
  font-size: 12px;
  resize: vertical;
  background: #f8f9fa;
}
.btn {
  display: block;
  text-align: center;
  padding: 12px 24px;
  background: #667eea;
  color: white;
  border-radius: 8px;
  text-decoration: none;
  margin-top: 16px;
}
</style>
