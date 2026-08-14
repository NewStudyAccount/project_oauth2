<template>
  <div class="container">
    <div class="card" style="text-align: center;">
      <div v-if="loading">
        <h2>正在登录...</h2>
        <p style="color: #666; margin-top: 16px;">请稍候，正在处理授权回调</p>
      </div>
      <div v-else-if="error">
        <h2 style="color: #dc2626;">登录失败</h2>
        <p style="color: #666; margin-top: 16px;">{{ error }}</p>
        <router-link to="/" class="btn btn-primary" style="margin-top: 24px;">返回首页</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  const code = route.query.code
  const state = route.query.state
  const errorParam = route.query.error

  if (errorParam) {
    loading.value = false
    error.value = route.query.error_description || '授权被拒绝'
    return
  }

  if (!code) {
    loading.value = false
    error.value = '缺少授权码'
    return
  }

  try {
    await authStore.handleCallback(code, state)
    router.push('/profile')
  } catch (e) {
    loading.value = false
    error.value = e.message || '登录处理失败'
  }
})
</script>
