<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <h2>客户端管理</h2>
      <el-button type="primary" @click="$router.push('/clients/add')">新增客户端</el-button>
    </div>

    <el-table :data="clients" border stripe>
      <el-table-column prop="clientId" label="Client ID" />
      <el-table-column prop="clientName" label="名称" />
      <el-table-column label="认证方式">
        <template #default="{ row }">
          <el-tag v-for="m in row.clientAuthenticationMethods" :key="m" size="small" style="margin-right: 4px">{{ m }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="授权类型">
        <template #default="{ row }">
          <el-tag v-for="g in row.authorizationGrantTypes" :key="g" type="success" size="small" style="margin-right: 4px">{{ g }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态">
        <template #default="{ row }">
          <el-switch :model-value="row.enabled" @change="(val) => handleToggle(row, val)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/clients/${row.id}/edit`)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listClients, setClientStatus, deleteClient } from '../api/client'
import { ElMessage, ElMessageBox } from 'element-plus'

const clients = ref([])

async function load() {
  try {
    const { data } = await listClients()
    clients.value = data
  } catch (e) {
    console.error(e)
    ElMessage.error('加载客户端列表失败')
  }
}

async function handleToggle(row, val) {
  try {
    await setClientStatus(row.id, val)
    row.enabled = val
    ElMessage.success(val ? '已启用' : '已禁用')
  } catch (e) {
    console.error(e)
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除客户端 ${row.clientId} 吗？`, '确认删除', { type: 'warning' })
    await deleteClient(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

onMounted(load)
</script>