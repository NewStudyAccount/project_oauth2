<template>
  <el-container style="height: 100vh">
    <el-header style="background: #409eff; color: white; display: flex; align-items: center; justify-content: space-between">
      <h2 style="color: white; margin: 0">统一认证中心 - 管理后台</h2>
      <div>
        <el-button type="danger" size="small" @click="logout">登出</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px">
        <el-menu :default-active="$route.path" router>
          <el-menu-item index="/">
            <span>系统概览</span>
          </el-menu-item>
          <el-menu-item index="/clients">
            <span>客户端管理</span>
          </el-menu-item>
          <el-menu-item index="/users">
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/access">
            <span>权限管理</span>
          </el-menu-item>
          <el-menu-item index="/audit-logs">
            <span>审计日志</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

function logout() {
  authStore.logout()
}
</script>
