<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <h2>{{ isEdit ? '编辑客户端' : '新增客户端' }}</h2>
      <el-button @click="$router.push('/clients')">返回列表</el-button>
    </div>

    <el-form :model="form" label-width="160px" style="max-width: 600px">
      <el-form-item label="Client ID" required>
        <el-input v-model="form.clientId" :disabled="isEdit" />
      </el-form-item>

      <el-form-item :label="isEdit ? 'Client Secret（留空不修改）' : 'Client Secret'">
        <el-input v-model="form.clientSecret" type="password" show-password />
      </el-form-item>

      <el-form-item label="客户端名称">
        <el-input v-model="form.clientName" />
      </el-form-item>

      <el-form-item label="认证方式" required>
        <el-checkbox-group v-model="form.clientAuthenticationMethods">
          <el-checkbox label="client_secret_basic" />
          <el-checkbox label="client_secret_post" />
          <el-checkbox label="none" />
        </el-checkbox-group>
      </el-form-item>

      <el-form-item label="授权类型" required>
        <el-checkbox-group v-model="form.authorizationGrantTypes">
          <el-checkbox label="authorization_code" />
          <el-checkbox label="refresh_token" />
          <el-checkbox label="client_credentials" />
        </el-checkbox-group>
      </el-form-item>

      <el-form-item label="重定向 URI">
        <div v-for="(uri, i) in form.redirectUris" :key="i" style="display: flex; margin-bottom: 4px; width: 100%">
          <el-input v-model="form.redirectUris[i]" style="flex: 1" />
          <el-button type="danger" size="small" @click="form.redirectUris.splice(i, 1)" style="margin-left: 4px">删除</el-button>
        </div>
        <el-button size="small" @click="form.redirectUris.push('')">添加 URI</el-button>
      </el-form-item>

      <el-form-item label="Scopes">
        <div v-for="(scope, i) in form.scopes" :key="i" style="display: flex; margin-bottom: 4px; width: 100%">
          <el-input v-model="form.scopes[i]" style="flex: 1" />
          <el-button type="danger" size="small" @click="form.scopes.splice(i, 1)" style="margin-left: 4px">删除</el-button>
        </div>
        <el-button size="small" @click="form.scopes.push('')">添加 Scope</el-button>
      </el-form-item>

      <el-form-item label="Access Token 有效期（秒）">
        <el-input-number v-model="form.accessTokenTtl" :min="60" />
      </el-form-item>

      <el-form-item label="Refresh Token 有效期（秒）">
        <el-input-number v-model="form.refreshTokenTtl" :min="60" />
      </el-form-item>

      <el-form-item label="Auth Code 有效期（秒）">
        <el-input-number v-model="form.authorizationCodeTtl" :min="10" />
      </el-form-item>

      <el-form-item label="需要 PKCE">
        <el-switch v-model="form.requireProofKey" />
      </el-form-item>

      <el-form-item label="需要授权确认">
        <el-switch v-model="form.requireAuthorizationConsent" />
      </el-form-item>

      <el-form-item label="启用">
        <el-switch v-model="form.enabled" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getClient, createClient, updateClient } from '../api/client'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)

const form = ref({
  clientId: '',
  clientSecret: '',
  clientName: '',
  clientAuthenticationMethods: ['client_secret_basic'],
  authorizationGrantTypes: ['authorization_code', 'refresh_token'],
  redirectUris: [''],
  scopes: ['openid', 'profile', 'email'],
  accessTokenTtl: 1800,
  refreshTokenTtl: 604800,
  authorizationCodeTtl: 300,
  requireProofKey: false,
  requireAuthorizationConsent: false,
  enabled: true
})

onMounted(async () => {
  if (isEdit.value) {
    try {
      const { data } = await getClient(route.params.id)
      data.clientSecret = ''
      if (!data.redirectUris || data.redirectUris.length === 0) data.redirectUris = ['']
      if (!data.scopes || data.scopes.length === 0) data.scopes = ['']
      form.value = data
    } catch (e) {
      ElMessage.error('加载客户端失败')
    }
  }
})

async function handleSubmit() {
  try {
    const payload = { ...form.value }
    payload.redirectUris = payload.redirectUris.filter(u => u.trim())
    payload.scopes = payload.scopes.filter(s => s.trim())
    if (!payload.clientSecret) delete payload.clientSecret

    if (isEdit.value) {
      await updateClient(route.params.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createClient(payload)
      ElMessage.success('创建成功')
    }
    router.push('/clients')
  } catch (e) {
    ElMessage.error('保存失败')
    console.error(e)
  }
}
</script>