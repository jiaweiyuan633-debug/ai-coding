<template>
  <div class="workspace">
    <!-- 顶栏 -->
    <div class="ws-header">
      <div class="ws-title">
        <a-button size="mini" type="text" @click="$router.push('/my')"><icon-left /></a-button>
        <span class="name">{{ app?.appName || '工作台' }}</span>
        <a-tag v-if="app?.codeGenType" size="small" color="arcoblue">{{ genTypeText(app.codeGenType) }}</a-tag>
        <a-tag v-if="latestVersion" size="small" color="green">v{{ latestVersion }}</a-tag>
      </div>
      <div class="ws-actions">
        <a-button size="small" :type="editMode ? 'primary' : 'outline'" @click="toggleEditMode">
          <icon-highlight style="margin-right: 4px" /> 可视化编辑
        </a-button>
        <a-button size="small" type="outline" @click="refreshPreview"><icon-refresh /> 刷新预览</a-button>
        <a-button size="small" type="primary" :loading="deploying" @click="handleDeploy">
          <icon-rocket style="margin-right: 4px" /> {{ app?.deployKey ? '重新部署' : '一键部署' }}
        </a-button>
      </div>
    </div>

    <div class="ws-body">
      <!-- 左：对话 -->
      <div class="chat-panel">
        <div class="chat-messages" ref="chatBox">
          <div v-for="(msg, i) in messages" :key="i" class="chat-msg" :class="msg.role">
            <div class="bubble">
              <div v-if="msg.role === 'workflow'" class="wf-node">
                <span class="wf-dot" :class="msg.status" />
                <span><b>{{ msg.node }}</b> · {{ msg.summary }}</span>
              </div>
              <template v-else>{{ msg.content }}</template>
            </div>
          </div>
          <div v-if="generating" class="generating-tip">
            <a-spin :size="14" /> AI 工作流执行中…
          </div>
        </div>
        <div v-if="selectedElement" class="selected-chip">
          <icon-highlight />
          已选中元素：{{ selectedElement }}
          <icon-close-circle @click="selectedElement = ''" style="cursor: pointer" />
        </div>
        <div class="chat-input">
          <a-textarea
            v-model="inputText"
            :placeholder="placeholderText"
            :max-length="2000"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            @keydown.enter.ctrl="sendMessage"
          />
          <a-button type="primary" :loading="generating" :disabled="generating" @click="sendMessage">
            <icon-send />
          </a-button>
        </div>
      </div>

      <!-- 中：实时预览 -->
      <div class="preview-panel">
        <div class="preview-toolbar">
          <span class="panel-title">实时预览</span>
          <a-button size="mini" type="text" @click="showCode = !showCode">
            {{ showCode ? '隐藏代码流' : '查看代码流' }}
          </a-button>
        </div>
        <div v-if="showCode" class="code-stream">
          <pre>{{ streamingCode || '（生成时代码将在此实时打印）' }}</pre>
        </div>
        <iframe
          ref="previewFrame"
          class="preview-frame"
          :src="previewUrl"
          @load="onPreviewLoad"
        ></iframe>
      </div>

      <!-- 右：文件 / 版本 / 用量 -->
      <div class="side-panel">
        <a-tabs default-active-key="files" size="mini">
          <a-tab-pane key="files" title="文件">
            <a-empty v-if="files.length === 0" description="生成后显示文件" style="margin-top: 40px" />
            <div v-for="f in files" :key="f" class="file-item">
              <icon-file /> {{ f }}
            </div>
          </a-tab-pane>
          <a-tab-pane key="versions" title="版本时光机">
            <a-empty v-if="versions.length === 0" description="暂无版本" style="margin-top: 40px" />
            <div v-for="v in versions" :key="v.id" class="version-item">
              <div>
                <b>v{{ v.version }}</b>
                <span class="v-msg">{{ v.message }}</span>
              </div>
              <a-button size="mini" type="text" @click="handleRollback(v)">回滚</a-button>
            </div>
          </a-tab-pane>
          <a-tab-pane key="usage" title="用量">
            <div v-if="usage" class="usage-box">
              <div class="usage-row"><span>调用次数</span><b>{{ usage.totalInvocations }}</b></div>
              <div class="usage-row"><span>输入 tokens</span><b>{{ usage.inputTokens }}</b></div>
              <div class="usage-row"><span>输出 tokens</span><b>{{ usage.outputTokens }}</b></div>
              <div class="usage-row"><span>总耗时</span><b>{{ (usage.totalCostMs / 1000).toFixed(1) }}s</b></div>
              <div v-for="(agg, purpose) in usage.byPurpose" :key="purpose" class="usage-row sub">
                <span>{{ purpose }}</span><b>{{ agg[0] }} 次 / {{ agg[1] }} tokens</b>
              </div>
            </div>
            <a-button v-else size="mini" long @click="loadUsage">加载用量</a-button>
          </a-tab-pane>
        </a-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Message, Modal } from '@arco-design/web-vue'
import {
  getApp, chatHistory, openChatStream, deployApp, listVersions, rollbackVersion, usageSummary
} from '../api'

const route = useRoute()
const appId = route.params.appId

const app = ref(null)
const messages = ref([])
const inputText = ref('')
const generating = ref(false)
const files = ref([])
const versions = ref([])
const usage = ref(null)
const deploying = ref(false)
const showCode = ref(false)
const streamingCode = ref('')
const selectedElement = ref('')
const editMode = ref(false)
const previewFrame = ref(null)
const chatBox = ref(null)

const previewUrl = computed(() => `/api/preview/${appId}/index.html`)
const latestVersion = computed(() => (versions.value[0] ? versions.value[0].version : null))
const placeholderText = computed(() =>
  selectedElement.value
    ? `描述对已选中元素（${selectedElement.value}）的修改…`
    : '继续对话修改应用，例如：把主色改成橙色、加一个页脚…'
)

const genTypeText = (type) =>
  ({ HTML_SINGLE: 'HTML 单文件', HTML_MULTI_FILE: 'HTML 多文件', VUE_PROJECT: 'Vue 工程' }[type] || type)

const scrollToBottom = () => nextTick(() => {
  if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
})

onMounted(async () => {
  const res = await getApp(appId)
  app.value = res.data
  const historyRes = await chatHistory(appId, undefined, 20)
  const history = (historyRes.data || []).slice().reverse()
  messages.value = history.map((h) => ({
    role: h.messageType === 'user' ? 'user' : 'ai',
    content: h.message
  }))
  await loadVersions()
  scrollToBottom()
  // 创作页「开始生成」进入：自动以初始需求触发首次生成
  if (route.query.autostart && versions.value.length === 0 && !generating.value) {
    inputText.value = app.value.initPrompt
    await sendMessage()
  }
})

const loadVersions = async () => {
  const res = await listVersions(appId)
  versions.value = res.data || []
}

const loadUsage = async () => {
  const res = await usageSummary({ appId })
  usage.value = res.data
}

const refreshPreview = () => {
  if (previewFrame.value) previewFrame.value.src = `${previewUrl.value}?t=${Date.now()}`
}

/** ========== 可视化编辑 ========== */
const toggleEditMode = () => {
  editMode.value = !editMode.value
  if (editMode.value) {
    injectEditMode()
    Message.info('已开启：在预览中点击想修改的元素')
  } else {
    selectedElement.value = ''
  }
}

const injectEditMode = () => {
  const frame = previewFrame.value
  if (!frame || !frame.contentDocument) {
    setTimeout(injectEditMode, 500)
    return
  }
  try {
    const doc = frame.contentDocument
    if (doc.getElementById('__aic_edit_probe__')) return
    const script = doc.createElement('script')
    script.id = '__aic_edit_probe__'
    script.textContent = `
      (function () {
        let last = null;
        function selectorOf(el) {
          const parts = [];
          let node = el;
          while (node && node !== document.documentElement && parts.length < 4) {
            let seg = node.tagName ? node.tagName.toLowerCase() : '';
            if (node.id) { parts.unshift('#' + node.id); break; }
            if (node.className && typeof node.className === 'string') {
              seg += '.' + node.className.trim().split(/\\s+/).slice(0, 2).join('.');
            }
            parts.unshift(seg);
            node = node.parentElement;
          }
          return parts.join(' > ');
        }
        document.addEventListener('click', function (e) {
          if (!window.__aic_edit_mode__) return;
          e.preventDefault(); e.stopPropagation();
          if (last) last.style.outline = '';
          last = e.target;
          last.style.outline = '2px solid #667eea';
          parent.postMessage({
            type: '__aic_select__',
            selector: selectorOf(e.target),
            text: (e.target.textContent || '').trim().slice(0, 60)
          }, '*');
        }, true);
        Object.defineProperty(window, '__aic_edit_mode__', { value: true, writable: true });
      })();
    `
    doc.documentElement.appendChild(script)
    window.addEventListener('message', (event) => {
      if (event.data?.type === '__aic_select__') {
        selectedElement.value = event.data.selector
        if (event.data.text) {
          selectedElement.value += `（文本：${event.data.text}）`
        }
      }
    })
  } catch (e) {
    /* 跨域等情况忽略 */
  }
}

const onPreviewLoad = () => {
  if (editMode.value) injectEditMode()
}

/** ========== 对话与 SSE ========== */
const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || generating.value) return
  let message = text
  if (selectedElement.value) {
    message = `[可视化编辑] 选中元素：${selectedElement.value}。修改要求：${text}`
    selectedElement.value = ''
  }
  inputText.value = ''
  messages.value.push({ role: 'user', content: text })
  generating.value = true
  streamingCode.value = ''
  showCode.value = true
  scrollToBottom()

  const es = openChatStream(appId, message)
  es.addEventListener('router', (e) => {
    const data = JSON.parse(e.data)
    messages.value.push({ role: 'workflow', node: 'route', status: 'success', summary: `生成类型：${data.text || data.codeGenType}` })
    scrollToBottom()
  })
  es.addEventListener('workflow', (e) => {
    const data = JSON.parse(e.data)
    messages.value.push({ role: 'workflow', node: data.node, status: data.status, summary: data.summary })
    scrollToBottom()
  })
  es.addEventListener('delta', (e) => {
    const data = JSON.parse(e.data)
    streamingCode.value += data.text
  })
  es.addEventListener('tool', (e) => {
    const data = JSON.parse(e.data)
    files.value.push(data.path)
  })
  es.addEventListener('done', (e) => {
    const data = JSON.parse(e.data)
    files.value = data.files || files.value
    messages.value.push({ role: 'ai', content: `生成完成（v${data.version}，共 ${data.files?.length || 0} 个文件），可继续对话修改。` })
    generating.value = false
    loadVersions()
    refreshPreview()
    getApp(appId).then((res) => { app.value = res.data }).catch(() => {})
    es.close()
  })
  es.addEventListener('error', (e) => {
    let msg = '生成失败'
    try { msg = JSON.parse(e.data).message || msg } catch (err) { /* noop */ }
    messages.value.push({ role: 'ai', content: `❌ ${msg}` })
    generating.value = false
    es.close()
  })
  es.onerror = () => {
    if (generating.value) {
      generating.value = false
      es.close()
    }
  }
}

/** ========== 部署与回滚 ========== */
const handleDeploy = async () => {
  deploying.value = true
  try {
    const res = await deployApp(appId)
    const shareUrl = `${location.origin}${res.data.sharePath}`
    app.value.deployKey = res.data.deployKey
    Modal.success({
      title: '部署成功 🎉',
      content: `分享链接：${shareUrl}（已自动复制，他人可直接访问）`,
      okText: '打开链接',
      onOk: () => window.open(shareUrl, '_blank')
    })
    await navigator.clipboard?.writeText(shareUrl).catch(() => {})
  } catch (e) {
    Message.error(e.response?.data?.message || e.message)
  } finally {
    deploying.value = false
  }
}

const handleRollback = (v) => {
  Modal.confirm({
    title: `回滚到 v${v.version}`,
    content: `将把应用恢复到版本 v${v.version}（${v.message}），并生成新版本记录。`,
    onOk: async () => {
      try {
        await rollbackVersion(appId, v.version)
        Message.success('回滚成功')
        await loadVersions()
        refreshPreview()
      } catch (e) {
        Message.error(e.response?.data?.message || e.message)
      }
    }
  })
}
</script>

<style scoped>
.workspace { height: calc(100vh - 56px); display: flex; flex-direction: column; background: #f5f6f8; }
.ws-header { background: #fff; border-bottom: 1px solid #ececf2; padding: 10px 16px; display: flex; justify-content: space-between; align-items: center; }
.ws-title { display: flex; align-items: center; gap: 8px; }
.ws-title .name { font-weight: bold; color: #333; font-size: 15px; }
.ws-actions { display: flex; gap: 8px; }
.ws-body { flex: 1; display: grid; grid-template-columns: 380px 1fr 260px; gap: 10px; padding: 10px; min-height: 0; }

.chat-panel { background: #fff; border-radius: 12px; display: flex; flex-direction: column; overflow: hidden; }
.chat-messages { flex: 1; overflow-y: auto; padding: 14px; display: flex; flex-direction: column; gap: 10px; }
.chat-msg { display: flex; }
.chat-msg.user { justify-content: flex-end; }
.chat-msg.ai { justify-content: flex-start; }
.bubble { max-width: 88%; padding: 8px 12px; border-radius: 12px; font-size: 13px; line-height: 1.7; white-space: pre-wrap; word-break: break-word; }
.chat-msg.user .bubble { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
.chat-msg.ai .bubble { background: #f4f5f9; color: #333; }
.wf-node { display: flex; align-items: flex-start; gap: 6px; font-size: 12px; }
.wf-dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 5px; background: #c9cdd4; }
.wf-dot.running { background: #ff9a2e; animation: pulse 1s infinite; }
.wf-dot.success { background: #00b42a; }
.wf-dot.failed { background: #f53f3f; }
@keyframes pulse { 50% { opacity: 0.4; } }
.generating-tip { font-size: 12px; color: #999; display: flex; gap: 6px; align-items: center; padding: 4px 6px; }
.selected-chip { margin: 0 12px 6px; background: #f0f1f9; color: #667eea; font-size: 12px; padding: 6px 10px; border-radius: 8px; display: flex; gap: 6px; align-items: center; }
.chat-input { padding: 10px; border-top: 1px solid #f0f1f5; display: flex; gap: 8px; align-items: flex-end; }

.preview-panel { background: #fff; border-radius: 12px; display: flex; flex-direction: column; overflow: hidden; }
.preview-toolbar { padding: 8px 14px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f0f1f5; }
.panel-title { font-size: 13px; font-weight: bold; color: #555; }
.code-stream { height: 160px; overflow: auto; background: #1d2129; padding: 10px; }
.code-stream pre { color: #7ee787; font-size: 11px; font-family: Consolas, monospace; white-space: pre-wrap; word-break: break-all; }
.preview-frame { flex: 1; border: none; width: 100%; background: #fff; }

.side-panel { background: #fff; border-radius: 12px; padding: 8px; overflow: auto; }
.file-item { font-size: 12px; padding: 6px 8px; border-radius: 6px; color: #444; display: flex; align-items: center; gap: 6px; }
.file-item:hover { background: #f5f6fa; }
.version-item { display: flex; justify-content: space-between; align-items: center; padding: 8px; font-size: 12px; border-bottom: 1px dashed #eee; }
.v-msg { display: block; color: #999; max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.usage-box { padding: 10px; }
.usage-row { display: flex; justify-content: space-between; font-size: 12px; padding: 6px 0; color: #555; }
.usage-row.sub { color: #999; border-top: 1px dashed #f0f0f0; }
</style>
