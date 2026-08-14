<template>
  <div>
    <h2>用户管理</h2>
    <el-table :data="users" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" @click="handleToggle(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listUsers, setUserStatus } from '../api/user'
import { ElMessage } from 'element-plus'

const users = ref([])

async function load() {
  try {
    const { data } = await listUsers()
    users.value = data
  } catch (e) {
    console.error(e)
    ElMessage.error('加载用户列表失败')
  }
}

async function handleToggle(row) {
  const enabled = row.status !== 1
  try {
    await setUserStatus(row.id, enabled)
    row.status = enabled ? 1 : 0
    ElMessage.success(enabled ? '已启用' : '已禁用')
  } catch (e) {
    console.error(e)
  }
}

onMounted(load)
</script>