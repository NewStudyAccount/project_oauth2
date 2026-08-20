<template>
  <div class="callback-container">
    <div v-if="loading" class="loading">
      <p>正在登录...</p>
    </div>
    <div v-if="error" class="error">
      <p>登录失败: {{ error }}</p>
      <el-button @click="goLogin">重新登录</el-button>
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

  if (!code) {
    error.value = '缺少授权码'
    loading.value = false
    return
  }

  try {
    await authStore.handleCallback(code, state)
    router.push('/')
  } catch (e) {
    error.value = e.message
    loading.value = false
  }
})

function goLogin() {
  authStore.login()
}
</script>

<style scoped>
.callback-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}
.loading, .error {
  text-align: center;
  font-size: 18px;
}
.error {
  color: #f56c6c;
}
</style>
