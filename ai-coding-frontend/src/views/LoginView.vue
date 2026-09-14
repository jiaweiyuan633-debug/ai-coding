<template>
  <div class="login-page">
    <a-card class="login-card" :bordered="false">
      <div class="login-title">
        <icon-code class="logo-icon" />
        <h2>登录 AI Coding</h2>
        <p>用一句话，生成一个应用</p>
      </div>
      <a-tabs v-model:active-key="mode">
        <a-tab-pane key="login" title="登录">
          <a-form :model="loginForm" layout="vertical" @submit-success="handleLogin">
            <a-form-item field="userAccount" label="账号" :rules="[{ required: true, message: '请输入账号' }]">
              <a-input v-model="loginForm.userAccount" placeholder="请输入账号" />
            </a-form-item>
            <a-form-item field="userPassword" label="密码" :rules="[{ required: true, message: '请输入密码' }]">
              <a-input-password v-model="loginForm.userPassword" placeholder="请输入密码" />
            </a-form-item>
            <a-button type="primary" html-type="submit" long :loading="loading">登录</a-button>
          </a-form>
        </a-tab-pane>
        <a-tab-pane key="register" title="注册">
          <a-form :model="regForm" layout="vertical" @submit-success="handleRegister">
            <a-form-item field="userAccount" label="账号" :rules="[{ required: true, message: '至少 4 位' }]">
              <a-input v-model="regForm.userAccount" placeholder="至少 4 位" />
            </a-form-item>
            <a-form-item field="userPassword" label="密码" :rules="[{ required: true, message: '至少 8 位' }]">
              <a-input-password v-model="regForm.userPassword" placeholder="至少 8 位" />
            </a-form-item>
            <a-form-item field="checkPassword" label="确认密码" :rules="[{ required: true, message: '请再次输入密码' }]">
              <a-input-password v-model="regForm.checkPassword" placeholder="再次输入密码" />
            </a-form-item>
            <a-button type="primary" html-type="submit" long :loading="loading">注册并登录</a-button>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const mode = ref('login')
const loading = ref(false)

const loginForm = reactive({ userAccount: '', userPassword: '' })
const regForm = reactive({ userAccount: '', userPassword: '', checkPassword: '' })

const redirectAfter = () => {
  router.push(route.query.redirect || '/')
}

const handleLogin = async () => {
  loading.value = true
  try {
    await userStore.login(loginForm.userAccount, loginForm.userPassword)
    Message.success('登录成功')
    redirectAfter()
  } catch (e) {
    Message.error(e.response?.data?.message || e.message)
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  loading.value = true
  try {
    await userStore.register(regForm.userAccount, regForm.userPassword, regForm.checkPassword)
    await userStore.login(regForm.userAccount, regForm.userPassword)
    Message.success('注册成功')
    redirectAfter()
  } catch (e) {
    Message.error(e.response?.data?.message || e.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card { width: 420px; border-radius: 16px; padding: 8px 12px; }
.login-title { text-align: center; margin-bottom: 16px; }
.login-title h2 { color: #333; margin: 8px 0 4px; }
.login-title p { color: #999; font-size: 13px; }
.logo-icon { font-size: 36px; color: #667eea; }
</style>
