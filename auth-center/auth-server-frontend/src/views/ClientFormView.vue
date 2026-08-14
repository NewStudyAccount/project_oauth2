<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <h2>{{ isEdit ? '编辑客户端' : '新增客户端' }}</h2>
      <el-button @click="$router.push('/clients')">返回列表</el-button>
    </div>

    <el-form :model="form" label-width="180px" style="max-width: 650px">
      <el-form-item label="Client ID" required>
        <el-input v-model="form.clientId" :disabled="isEdit" />
        <div class="form-tip">客户端唯一标识符，用于 OAuth2 授权请求中的 client_id 参数。创建后不可修改。</div>
      </el-form-item>

      <el-form-item :label="isEdit ? 'Client Secret（留空不修改）' : 'Client Secret'">
        <el-input v-model="form.clientSecret" type="password" show-password />
        <div class="form-tip">客户端密钥，用于机密客户端的身份验证。BCrypt 加密存储，编辑时留空表示不修改。公开客户端（如纯前端 SPA）无需密钥。</div>
      </el-form-item>

      <el-form-item label="密钥过期时间">
        <el-date-picker v-model="form.clientSecretExpiresAt" type="datetime" placeholder="留空表示永不过期" value-format="YYYY-MM-DDTHH:mm:ssZ" clearable />
        <div class="form-tip">客户端密钥的过期时间，过期后需重新生成密钥。留空表示永不过期。用于密钥轮换安全策略。</div>
      </el-form-item>

      <el-form-item label="客户端名称">
        <el-input v-model="form.clientName" />
        <div class="form-tip">客户端的人类可读名称，显示在授权确认页面和审计日志中，帮助用户识别应用。</div>
      </el-form-item>

      <el-form-item label="客户端认证方式" required>
        <el-checkbox-group v-model="form.clientAuthenticationMethods">
          <el-checkbox label="client_secret_basic" />
          <el-checkbox label="client_secret_post" />
          <el-checkbox label="none" />
        </el-checkbox-group>
        <div class="form-tip">客户端向认证服务器证明自己身份的方式：<br/>• client_secret_basic — 通过 HTTP Basic Auth 传递凭证（最常用，适合服务端应用）<br/>• client_secret_post — 通过 POST 请求体传递凭证<br/>• none — 公开客户端，无需认证（适合纯前端 SPA/PKCE 场景）</div>
      </el-form-item>

      <el-form-item label="授权类型" required>
        <el-checkbox-group v-model="form.authorizationGrantTypes">
          <el-checkbox label="authorization_code" />
          <el-checkbox label="refresh_token" />
          <el-checkbox label="client_credentials" />
        </el-checkbox-group>
        <div class="form-tip">客户端可使用的 OAuth2 授权流程：<br/>• authorization_code — 授权码模式，最安全，用户登录授权后获取授权码再换取令牌<br/>• refresh_token — 刷新令牌，access_token 过期后免登录获取新令牌<br/>• client_credentials — 客户端凭证模式，无用户参与，适合服务间调用</div>
      </el-form-item>

      <el-form-item label="重定向 URI">
        <div v-for="(uri, i) in form.redirectUris" :key="i" style="display: flex; margin-bottom: 4px; width: 100%">
          <el-input v-model="form.redirectUris[i]" style="flex: 1" />
          <el-button type="danger" size="small" @click="form.redirectUris.splice(i, 1)" style="margin-left: 4px">删除</el-button>
        </div>
        <el-button size="small" @click="form.redirectUris.push('')">添加 URI</el-button>
        <div class="form-tip">授权完成后认证服务器将用户重定向回客户端的 URI。必须与客户端实际地址完全匹配（含协议、域名、端口、路径），防止开放重定向攻击。</div>
      </el-form-item>

      <el-form-item label="Scopes">
        <div v-for="(scope, i) in form.scopes" :key="i" style="display: flex; margin-bottom: 4px; width: 100%">
          <el-input v-model="form.scopes[i]" style="flex: 1" />
          <el-button type="danger" size="small" @click="form.scopes.splice(i, 1)" style="margin-left: 4px">删除</el-button>
        </div>
        <el-button size="small" @click="form.scopes.push('')">添加 Scope</el-button>
        <div class="form-tip">客户端可请求的权限范围：openid（OIDC 标识，请求 ID Token）、profile（用户基本信息）、email（用户邮箱），可自定义业务 scope。</div>
      </el-form-item>

      <el-form-item label="需要 PKCE">
        <el-switch v-model="form.requireProofKey" />
        <div class="form-tip">是否强制使用 PKCE（Proof Key for Code Exchange）增强安全，防止授权码被截获重放。适用于纯前端 SPA 等公开客户端，机密客户端通常关闭。</div>
      </el-form-item>

      <el-form-item label="需要授权确认">
        <el-switch v-model="form.requireAuthorizationConsent" />
        <div class="form-tip">用户首次使用该客户端时，是否弹出授权确认页面让用户同意分享信息。第三方应用建议开启，内部可信应用可关闭以跳过确认。</div>
      </el-form-item>

      <el-form-item label="启用">
        <el-switch v-model="form.enabled" />
        <div class="form-tip">客户端的启用/禁用状态。禁用后该客户端的所有 OAuth2 请求将被拒绝，已颁发的令牌不受影响。</div>
      </el-form-item>

      <el-form-item label="Access Token 有效期（秒）">
        <el-input-number v-model="form.accessTokenTtl" :min="60" />
        <div class="form-tip">访问令牌的有效时长，过期后需用 refresh_token 重新获取。较短更安全但增加刷新频率。默认 1800 秒（30 分钟）。</div>
      </el-form-item>

      <el-form-item label="Refresh Token 有效期（秒）">
        <el-input-number v-model="form.refreshTokenTtl" :min="60" />
        <div class="form-tip">刷新令牌的有效时长，决定用户最长可免登录保持会话的时间。默认 604800 秒（7 天）。</div>
      </el-form-item>

      <el-form-item label="Auth Code 有效期（秒）">
        <el-input-number v-model="form.authorizationCodeTtl" :min="10" />
        <div class="form-tip">授权码的有效时长，授权码是一次性的且有效期极短。默认 300 秒（5 分钟），一般无需修改。</div>
      </el-form-item>

      <el-form-item label="复用刷新令牌">
        <el-switch v-model="form.reuseRefreshTokens" />
        <div class="form-tip">刷新时是否返回相同的 refresh_token。开启（默认）：客户端只需存储一次，使用更简单；关闭（Rotation）：每次刷新返回新的，旧的立即失效，安全性更高，可检测令牌被盗用。</div>
      </el-form-item>

      <el-form-item label="ID Token 签名算法">
        <el-select v-model="form.idTokenSignatureAlgorithm">
          <el-option label="RS256 (RSA + SHA-256)" value="RS256" />
          <el-option label="RS384 (RSA + SHA-384)" value="RS384" />
          <el-option label="RS512 (RSA + SHA-512)" value="RS512" />
          <el-option label="ES256 (ECDSA + SHA-256)" value="ES256" />
          <el-option label="ES384 (ECDSA + SHA-384)" value="ES384" />
          <el-option label="ES512 (ECDSA + SHA-512)" value="ES512" />
        </el-select>
        <div class="form-tip">OIDC ID Token 的签名算法。RS256（非对称 RSA，最常用）资源服务器用公钥验证；ES 系列为椭圆曲线签名，密钥更短性能更好。</div>
      </el-form-item>

      <el-form-item label="访问令牌格式">
        <el-select v-model="form.accessTokenFormat">
          <el-option label="JWT (self-contained)" value="self-contained" />
          <el-option label="Opaque (reference)" value="reference" />
        </el-select>
        <div class="form-tip">访问令牌的编码格式。JWT（self-contained）：令牌自包含所有信息，资源服务器可独立验证，最常用；Opaque（reference）：令牌是随机字符串，资源服务器需通过认证服务器的 introspection 端点验证，更可控但增加网络调用。</div>
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
  clientSecretExpiresAt: null,
  clientName: '',
  clientAuthenticationMethods: ['client_secret_basic'],
  authorizationGrantTypes: ['authorization_code', 'refresh_token'],
  redirectUris: [''],
  scopes: ['openid', 'profile', 'email'],
  requireProofKey: false,
  requireAuthorizationConsent: false,
  enabled: true,
  accessTokenTtl: 1800,
  refreshTokenTtl: 604800,
  authorizationCodeTtl: 300,
  reuseRefreshTokens: true,
  idTokenSignatureAlgorithm: 'RS256',
  accessTokenFormat: 'self-contained'
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

<style scoped>
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.5;
}
</style>