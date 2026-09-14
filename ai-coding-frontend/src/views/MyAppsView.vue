<template>
  <div class="my-page">
    <h2>我的应用</h2>
    <a-spin :loading="loading" style="width: 100%">
      <a-empty v-if="!loading && apps.length === 0" description="还没有应用，去首页创作一个吧" />
      <div class="grid">
        <AppCard
          v-for="app in apps"
          :key="app.id"
          :app="app"
          @edit="goWorkspace"
          @open="goWorkspace"
          @delete="handleDelete"
          @deploy="handleDeploy"
          @remix="handleRemix"
        />
      </div>
    </a-spin>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, Message } from '@arco-design/web-vue'
import AppCard from '../components/AppCard.vue'
import { pageMyApps, deleteApp, deployApp, remixApp } from '../api'

const router = useRouter()
const apps = ref([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const res = await pageMyApps({ currentPage: 1, pageSize: 50 })
    apps.value = res.data.records || []
  } finally {
    loading.value = false
  }
}

onMounted(load)

const goWorkspace = (app) => router.push({ name: 'workspace', params: { appId: app.id } })

const handleDelete = (app) => {
  Modal.confirm({
    title: '删除应用',
    content: `确定删除「${app.appName}」？此操作不可恢复。`,
    onOk: async () => {
      await deleteApp(app.id)
      Message.success('已删除')
      load()
    }
  })
}

const handleDeploy = async (app) => {
  try {
    const res = await deployApp(app.id)
    Message.success(`部署成功：${res.data.sharePath}`)
    load()
  } catch (e) {
    Message.error(e.response?.data?.message || e.message)
  }
}

const handleRemix = async (app) => {
  try {
    const res = await remixApp(app.id)
    Message.success('Remix 成功，已创建新应用')
    router.push({ name: 'workspace', params: { appId: res.data } })
  } catch (e) {
    Message.error(e.response?.data?.message || e.message)
  }
}
</script>

<style scoped>
.my-page { max-width: 1280px; margin: 0 auto; padding: 24px 20px; }
.my-page h2 { margin-bottom: 20px; color: #333; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 18px; }
</style>
