<template>
  <div>
    <h2>权限管理</h2>
    <el-form :inline="true" style="margin-bottom: 16px">
      <el-form-item label="选择用户">
        <el-select v-model="selectedUserId" placeholder="请选择用户" @change="loadAccess">
          <el-option v-for="u in users" :key="u.id" :label="u.username" :value="u.id" />
        </el-select>
      </el-form-item>
    </el-form>

    <el-table v-if="accesses.length > 0" :data="accesses" border stripe>
      <el-table-column prop="clientId" label="客户端 ID" />
      <el-table-column label="权限">
        <template #default="{ row }">
          <el-switch :model-value="row.allowed === 1" @change="(val) => handleToggle(row, val)" />
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else-if="selectedUserId" description="暂无权限记录" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listUsers } from '../api/user'
import { getUserAccess, setAccess } from '../api/access'
import { ElMessage } from 'element-plus'

const users = ref([])
const selectedUserId = ref(null)
const accesses = ref([])

async function loadUsers() {
  try {
    const { data } = await listUsers()
    users.value = data
  } catch (e) {
    console.error(e)
  }
}

async function loadAccess() {
  if (!selectedUserId.value) return
  try {
    const { data } = await getUserAccess(selectedUserId.value)
    accesses.value = data
  } catch (e) {
    console.error(e)
  }
}

async function handleToggle(row, val) {
  try {
    await setAccess(selectedUserId.value, row.clientId, val)
    row.allowed = val ? 1 : 0
    ElMessage.success('权限更新成功')
  } catch (e) {
    console.error(e)
  }
}

onMounted(loadUsers)
</script>