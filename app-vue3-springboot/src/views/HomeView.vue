<template>
  <div class="home-view">
    <template v-if="user">
      <div class="welcome-card">
        <h2>欢迎回来</h2>
        <div class="user-info">
          <p><strong>昵称：</strong>{{ user.nickname }}</p>
          <p><strong>用户名：</strong>{{ user.username }}</p>
          <p><strong>邮箱：</strong>{{ user.email }}</p>
        </div>
        <router-link to="/profile" class="btn btn-primary">查看完整资料</router-link>
      </div>
    </template>
    <template v-else>
      <div class="login-card">
        <h2>欢迎访问</h2>
        <p>请登录以访问受保护的资源</p>
        <a href="/oauth2/authorization/springboot-app" class="btn btn-primary">登录</a>
      </div>
    </template>
  </div>
</template>

<script setup>
defineProps(['user'])
</script>
