<template>
  <div class="container">
    <div class="card">
      <h2 style="margin-bottom: 24px;">外部 API 调用演示</h2>

      <div v-if="!authStore.isAuthenticated" style="text-align: center; color: #666;">
        <p>请先登录以访问受保护的 API</p>
        <button @click="login" class="btn btn-primary">登录</button>
      </div>

      <div v-else>
        <!-- 公开 API 测试 -->
        <div class="api-section">
          <h3>公开 API (无需认证)</h3>
          <button @click="callPublicApi" class="btn btn-primary" :disabled="loading">
            调用 /api/public
          </button>
          <button @click="callHealthApi" class="btn btn-primary" style="margin-left: 12px;" :disabled="loading">
            健康检查
          </button>
          <div v-if="publicResult" class="result">
            <pre>{{ JSON.stringify(publicResult, null, 2) }}</pre>
          </div>
        </div>

        <!-- 受保护 API 测试 -->
        <div class="api-section">
          <h3>受保护 API (需要 Token)</h3>
          <button @click="callProtectedApi" class="btn btn-primary" :disabled="loading">
            调用 /api/protected
          </button>
          <button @click="callProtectedData" class="btn btn-primary" style="margin-left: 12px;" :disabled="loading">
            获取受保护数据
          </button>
          <div v-if="protectedResult" class="result">
            <pre>{{ JSON.stringify(protectedResult, null, 2) }}</pre>
          </div>
        </div>

        <!-- 用户信息 API 测试 -->
        <div class="api-section">
          <h3>用户信息 API</h3>
          <button @click="callUserInfoApi" class="btn btn-primary" :disabled="loading">
            获取用户信息
          </button>
          <button @click="callClaimsApi" class="btn btn-primary" style="margin-left: 12px;" :disabled="loading">
            获取 Token Claims
          </button>
          <div v-if="userResult" class="result">
            <pre>{{ JSON.stringify(userResult, null, 2) }}</pre>
          </div>
        </div>

        <!-- 错误信息 -->
        <div v-if="error" class="error">
          <p>错误: {{ error }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { getPublicData, getHealthStatus, getProtectedData, getProtectedResource, getUserInfoFromBackend, getTokenClaims } from '../utils/api'

const authStore = useAuthStore()
const loading = ref(false)
const publicResult = ref(null)
const protectedResult = ref(null)
const userResult = ref(null)
const error = ref(null)

function login() {
  authStore.login()
}

async function callPublicApi() {
  loading.value = true
  error.value = null
  try {
    publicResult.value = await getPublicData()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function callHealthApi() {
  loading.value = true
  error.value = null
  try {
    publicResult.value = await getHealthStatus()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function callProtectedApi() {
  loading.value = true
  error.value = null
  try {
    protectedResult.value = await getProtectedData()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function callProtectedData() {
  loading.value = true
  error.value = null
  try {
    protectedResult.value = await getProtectedResource()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function callUserInfoApi() {
  loading.value = true
  error.value = null
  try {
    userResult.value = await getUserInfoFromBackend()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function callClaimsApi() {
  loading.value = true
  error.value = null
  try {
    userResult.value = await getTokenClaims()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.api-section {
  margin-bottom: 32px;
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 8px;
}

.api-section h3 {
  margin-bottom: 16px;
  color: #333;
}

.result {
  margin-top: 16px;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 4px;
  overflow-x: auto;
}

.result pre {
  margin: 0;
  font-size: 14px;
  white-space: pre-wrap;
}

.error {
  margin-top: 16px;
  padding: 16px;
  background: #fee;
  border: 1px solid #fcc;
  border-radius: 4px;
  color: #c00;
}
</style>
