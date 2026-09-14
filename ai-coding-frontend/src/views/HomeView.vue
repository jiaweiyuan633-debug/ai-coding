<template>
  <div class="home-page">
    <div class="hero">
      <h1>用一句话，生成一个应用</h1>
      <p>AI Coding · 零代码 · 实时预览 · 一键部署分享</p>
    </div>

    <div class="create-box">
      <a-textarea
        v-model="initPrompt"
        :placeholder="'描述你想要的应用，例如：\n· 制作一个个人主页，展示我的介绍和技能\n· 做一个番茄钟小工具，带开始/暂停和计时圈\n· 生成一个产品发布倒计时活动页，科技感风格'"
        :max-length="1000"
        :auto-size="{ minRows: 4, maxRows: 8 }"
      />
      <div class="create-actions">
        <a-select v-model="appName" placeholder="应用名称（可选，AI 会自动取名）" allow-clear class="name-input">
        </a-select>
        <a-button type="primary" size="large" :loading="creating" @click="handleCreate">
          <icon-rocket style="margin-right: 6px" />
          开始生成
        </a-button>
      </div>
    </div>

    <div class="templates">
      <h3>从模板开始</h3>
      <div class="template-grid">
        <a-card v-for="tpl in templates" :key="tpl.id" hoverable class="tpl-card" @click="useTemplate(tpl)">
          <div class="tpl-name">{{ tpl.name }}</div>
          <div class="tpl-desc">{{ tpl.description }}</div>
          <a-tag size="small" color="arcoblue">{{ genTypeText(tpl.codeGenType) }}</a-tag>
        </a-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { addApp, listTemplates } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const initPrompt = ref('')
const appName = ref('')
const creating = ref(false)
const templates = ref([])

onMounted(async () => {
  try {
    const res = await listTemplates()
    templates.value = res.data || []
  } catch (e) {
    /* 模板加载失败不阻塞 */
  }
})

const genTypeText = (type) =>
  ({ HTML_SINGLE: '单文件', HTML_MULTI_FILE: '多文件', VUE_PROJECT: 'Vue 工程' }[type] || type)

const useTemplate = (tpl) => {
  initPrompt.value = tpl.initPrompt
  Message.info(`已填入模板「${tpl.name}」的需求`)
}

const handleCreate = async () => {
  if (!initPrompt.value.trim()) {
    Message.warning('请先描述你的需求')
    return
  }
  if (!userStore.isLogin) {
    Message.info('请先登录')
    router.push({ name: 'login', query: { redirect: '/' } })
    return
  }
  creating.value = true
  try {
    const res = await addApp({ initPrompt: initPrompt.value.trim(), appName: appName.value || undefined })
    router.push({ name: 'workspace', params: { appId: res.data }, query: { autostart: '1' } })
  } catch (e) {
    Message.error(e.response?.data?.message || e.message)
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.home-page { max-width: 860px; margin: 0 auto; padding: 48px 20px; }
.hero { text-align: center; margin-bottom: 32px; }
.hero h1 { font-size: 36px; color: #2d3748; margin-bottom: 12px; }
.hero p { color: #999; font-size: 15px; }
.create-box { background: #fff; border-radius: 16px; padding: 20px; box-shadow: 0 8px 40px rgba(102, 126, 234, 0.12); }
.create-actions { display: flex; gap: 12px; margin-top: 12px; }
.name-input { width: 280px; }
.templates { margin-top: 48px; }
.templates h3 { color: #333; margin-bottom: 16px; }
.template-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; }
.tpl-card { border-radius: 12px; }
.tpl-name { font-weight: bold; color: #333; margin-bottom: 6px; }
.tpl-desc { font-size: 12px; color: #999; margin-bottom: 10px; min-height: 34px; }
</style>
