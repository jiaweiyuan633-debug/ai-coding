<template>
  <a-layout style="height: 100vh">
    <a-layout-header class="nav-header">
      <div class="nav-inner">
        <div class="logo" @click="$router.push('/')">
          <icon-code class="logo-icon" />
          <span>AI Coding</span>
          <span class="logo-sub">AI 零代码应用生成平台</span>
        </div>
        <div class="nav-menu">
          <router-link to="/" class="nav-link">创作</router-link>
          <router-link to="/my" class="nav-link">我的应用</router-link>
          <router-link to="/square" class="nav-link">广场</router-link>
        </div>
        <div class="nav-right">
          <template v-if="userStore.isLogin">
            <a-avatar :size="28" :style="{ backgroundColor: '#667eea' }">
              {{ (userStore.user?.userName || '用')[0] }}
            </a-avatar>
            <span class="user-name">{{ userStore.user?.userName }}</span>
            <a-button size="small" type="text" @click="handleLogout">退出</a-button>
          </template>
          <a-button v-else type="primary" size="small" @click="$router.push('/login')">登录</a-button>
        </div>
      </div>
    </a-layout-header>
    <a-layout-content class="page-content">
      <router-view />
    </a-layout-content>
  </a-layout>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const router = useRouter()

onMounted(() => userStore.fetchUser())

const handleLogout = () => {
  userStore.logout()
  router.push('/')
}
</script>

<style scoped>
.nav-header {
  background: #fff;
  border-bottom: 1px solid #e8e8ee;
  height: 56px;
  line-height: 56px;
  padding: 0;
}
.nav-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 28px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: bold;
  color: #667eea;
  cursor: pointer;
}
.logo-icon { font-size: 22px; }
.logo-sub { font-size: 12px; color: #999; font-weight: normal; }
.nav-menu { display: flex; gap: 20px; flex: 1; }
.nav-link { color: #555; text-decoration: none; font-size: 14px; }
.nav-link.router-link-active { color: #667eea; font-weight: bold; }
.nav-right { display: flex; align-items: center; gap: 8px; }
.user-name { font-size: 14px; color: #333; }
.page-content { height: calc(100vh - 56px); overflow: auto; }
</style>
