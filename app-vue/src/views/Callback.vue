<template>
  <div class="callback">
    <div class="card">
      <div v-if="loading" class="loading">
        <p>正在处理登录...</p>
      </div>
      <div v-else-if="error" class="error">
        <h2>❌ 登录失败</h2>
        <p>{{ error }}</p>
        <router-link to="/" class="btn">返回首页</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  try {
    const code = route.query.code
    const state = route.query.state

    if (!code) {
      throw new Error('缺少授权码参数')
    }

    await authStore.handleCallback(code, state)
    // 登录成功，跳转首页
    router.push('/')
  } catch (e) {
    error.value = e.message || '登录处理失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.callback {
  max-width: 400px;
  margin: 100px auto;
  padding: 0 20px;
}
.card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  padding: 40px;
  text-align: center;
}
.loading p {
  color: #666;
}
.error h2 {
  color: #c33;
  margin-bottom: 12px;
}
.error p {
  color: #666;
  margin-bottom: 20px;
}
.btn {
  display: inline-block;
  padding: 12px 24px;
  background: #667eea;
  color: white;
  border-radius: 8px;
  text-decoration: none;
}
</style>
