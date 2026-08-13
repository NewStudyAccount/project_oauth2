<template>
  <div>
    <h2>审计日志</h2>
    <el-form :inline="true" style="margin-bottom: 16px">
      <el-form-item label="操作类型">
        <el-select v-model="filterAction" clearable placeholder="全部" @change="load">
          <el-option label="登录" value="LOGIN" />
          <el-option label="登出" value="LOGOUT" />
          <el-option label="授权" value="AUTHORIZE" />
          <el-option label="Token 签发" value="TOKEN_ISSUED" />
          <el-option label="Token 撤销" value="TOKEN_REVOKED" />
        </el-select>
      </el-form-item>
      <el-form-item label="用户名">
        <el-input v-model="filterUsername" clearable @clear="load" @keyup.enter="load" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="logs" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="action" label="操作" width="140" />
      <el-table-column prop="detail" label="详情" />
      <el-table-column prop="status" label="状态" width="80" />
      <el-table-column prop="createdAt" label="时间" width="180" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAuditLogs } from '../api/audit'

const logs = ref([])
const filterAction = ref('')
const filterUsername = ref('')

async function load() {
  try {
    const params = {}
    if (filterAction.value) params.action = filterAction.value
    if (filterUsername.value) params.username = filterUsername.value
    const { data } = await getAuditLogs(params)
    logs.value = data
  } catch (e) {
    console.error(e)
  }
}

onMounted(load)
</script>