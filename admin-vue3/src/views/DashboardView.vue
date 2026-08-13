<template>
  <div>
    <h2>系统概览</h2>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>用户数</template>
          <div style="font-size: 36px; text-align: center">{{ stats.userCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>客户端数</template>
          <div style="font-size: 36px; text-align: center">{{ stats.clientCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>审计日志数</template>
          <div style="font-size: 36px; text-align: center">{{ stats.auditLogCount }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getStats } from '../api/audit'

const stats = ref({ userCount: 0, clientCount: 0, auditLogCount: 0 })

onMounted(async () => {
  try {
    const { data } = await getStats()
    stats.value = data
  } catch (e) {
    console.error('Failed to load stats', e)
  }
})
</script>