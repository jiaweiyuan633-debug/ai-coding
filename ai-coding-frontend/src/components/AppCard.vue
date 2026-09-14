<template>
  <a-card class="app-card" hoverable @click="$emit('open', app)">
    <template #cover>
      <div class="cover">
        <img v-if="app.cover" :src="app.cover" alt="" />
        <div v-else class="cover-placeholder">
          <icon-code />
        </div>
      </div>
    </template>
    <a-card-item>
      <div class="card-body">
        <div class="app-name">{{ app.appName || '未命名应用' }}</div>
        <div class="app-meta">
          <span class="gen-type">{{ genTypeText }}</span>
          <span v-if="app.deployKey" class="deployed"><icon-link /> 已部署</span>
        </div>
      </div>
    </a-card-item>
    <template #actions>
      <span class="action" @click.stop="$emit('edit', app)"><icon-edit /> 编辑</span>
      <span v-if="!app.deployKey" class="action" @click.stop="$emit('deploy', app)"><icon-rocket /> 部署</span>
      <a v-else :href="`/api/s/${app.deployKey}/`" target="_blank" class="action" @click.stop><icon-link /> 访问</a>
      <span class="action" @click.stop="$emit('remix', app)"><icon-copy /> Remix</span>
      <span class="action danger" @click.stop="$emit('delete', app)"><icon-delete /> 删除</span>
    </template>
  </a-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ app: { type: Object, required: true } })
defineEmits(['open', 'edit', 'deploy', 'remix', 'delete'])

const genTypeText = computed(() => {
  const map = {
    HTML_SINGLE: 'HTML 单文件',
    HTML_MULTI_FILE: 'HTML 多文件',
    VUE_PROJECT: 'Vue 工程'
  }
  return map[props.app.codeGenType] || props.app.codeGenType || '待生成'
})
</script>

<style scoped>
.app-card { border-radius: 12px; overflow: hidden; }
.cover { height: 140px; background: linear-gradient(135deg, #667eea22, #764ba222); display: flex; align-items: center; justify-content: center; }
.cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder { font-size: 40px; color: #667eea66; }
.card-body { padding: 4px 0; }
.app-name { font-size: 15px; font-weight: bold; color: #333; margin-bottom: 8px; }
.app-meta { display: flex; gap: 10px; font-size: 12px; color: #999; align-items: center; }
.gen-type { background: #f0f1f9; color: #667eea; padding: 2px 8px; border-radius: 10px; }
.deployed { color: #00b42a; }
.action { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #666; cursor: pointer; }
.action:hover { color: #667eea; }
.action.danger:hover { color: #f53f3f; }
</style>
