<template>
  <div class="profile-view">
    <h2>用户资料</h2>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="claims" class="claims-table">
      <table>
        <thead>
          <tr>
            <th>Claim</th>
            <th>Value</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(value, key) in claims" :key="key">
            <td class="claim-key">{{ key }}</td>
            <td class="claim-value">{{ formatValue(value) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-else class="error">
      <p>无法获取用户信息，请重新登录</p>
      <button @click="login" class="btn btn-primary">登录</button>
    </div>
    <router-link to="/" class="btn btn-back">返回首页</router-link>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const claims = ref(null)
const loading = ref(true)

const formatValue = (val) => {
  if (typeof val === 'object') return JSON.stringify(val)
  return String(val)
}

const login = () => {
  window.location.href = '/oauth2/authorization/springboot-app'
}

onMounted(async () => {
  try {
    const { data } = await api.get('/api/userinfo/claims')
    claims.value = data.claims
  } catch {
    claims.value = null
  } finally {
    loading.value = false
  }
})
</script>
