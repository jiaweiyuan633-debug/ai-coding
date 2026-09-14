<template>
  <div class="square-page">
    <h2>应用广场</h2>
    <p class="sub">精选应用 · 一键 Remix 二次创作</p>
    <a-spin :loading="loading" style="width: 100%">
      <a-empty v-if="!loading && apps.length === 0" description="暂无精选应用" />
      <div class="grid">
        <a-card v-for="app in apps" :key="app.id" hoverable class="sq-card">
          <template #cover>
            <div class="cover">
              <img v-if="app.cover" :src="app.cover" alt="" />
              <div v-else class="cover-placeholder"><icon-code /></div>
            </div>
          </template>
          <div class="sq-name">{{ app.appName || '未命名应用' }}</div>
          <div class="sq-meta">
            <a-avatar :size="20" :style="{ backgroundColor: '#667eea' }">
              {{ (app.user?.userName || '匿')[0] }}
            </a-avatar>
            <span>{{ app.user?.userName || '匿名用户' }}</span>
          </div>
          <template #actions>
            <span class="action" @click="openPreview(app)"><icon-eye /> 预览</span>
            <span class="action" @click="handleRemix(app)"><icon-copy /> Remix</span>
          </template>
        </a-card>
      </div>
    </a-spin>

    <h2 style="margin-top: 40px">模板市场</h2>
    <p class="sub">选一个模板快速开始</p>
    <div class="grid">
      <a-card v-for="tpl in templates" :key="tpl.id" hoverable class="sq-card">
        <div class="sq-name">{{ tpl.name }}</div>
        <div class="tpl-desc">{{ tpl.description }}</div>
        <template #actions>
          <span class="action" @click="useTemplate(tpl)"><icon-mind /> 用它创作</span>
        </template>
      </a-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { pageSquareApps, listTemplates, remixApp } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const apps = ref([])
const templates = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await pageSquareApps({ currentPage: 1, pageSize: 50 })
    apps.value = res.data.records || []
    const tplRes = await listTemplates()
    templates.value = tplRes.data || []
  } finally {
    loading.value = false
  }
})

const openPreview = (app) => {
  window.open(`/api/preview/${app.id}/index.html`, '_blank')
}

const handleRemix = async (app) => {
  if (!userStore.isLogin) {
    Message.info('请先登录')
    return router.push({ name: 'login' })
  }
  try {
    const res = await remixApp(app.id)
    Message.success('Remix 成功')
    router.push({ name: 'workspace', params: { appId: res.data } })
  } catch (e) {
    Message.error(e.response?.data?.message || e.message)
  }
}

const useTemplate = (tpl) => {
  router.push({ name: 'home', query: { prompt: tpl.initPrompt } })
}
</script>

<style scoped>
.square-page { max-width: 1280px; margin: 0 auto; padding: 24px 20px; }
.square-page h2 { color: #333; }
.sub { color: #999; font-size: 13px; margin: 8px 0 20px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 18px; margin-bottom: 24px; }
.sq-card { border-radius: 12px; }
.cover { height: 130px; background: linear-gradient(135deg, #667eea22, #764ba222); display: flex; align-items: center; justify-content: center; }
.cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder { font-size: 36px; color: #667eea66; }
.sq-name { font-weight: bold; color: #333; margin-bottom: 8px; }
.sq-meta { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #888; }
.tpl-desc { font-size: 12px; color: #999; min-height: 34px; }
.action { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #666; cursor: pointer; }
.action:hover { color: #667eea; }
</style>
