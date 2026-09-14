import client from './client'

/** 用户 */
export const register = (data) => client.post('/user/register', data)
export const login = (data) => client.post('/user/login', data)
export const getLoginUser = () => client.get('/user/get/login')

/** 应用 */
export const addApp = (data) => client.post('/app/add', data)
export const updateApp = (data) => client.post('/app/update', data)
export const deleteApp = (id) => client.post('/app/delete', { id })
export const getApp = (id) => client.get('/app/get', { params: { id } })
export const pageMyApps = (params) => client.get('/app/my/page', { params })
export const pageSquareApps = (params) => client.get('/app/square/page', { params })

/** 部署与 Remix */
export const deployApp = (appId) => client.post(`/app/${appId}/deploy`)
export const remixApp = (appId) => client.post(`/app/${appId}/remix`)

/** 版本时光机 */
export const listVersions = (appId) => client.get('/app/version/list', { params: { appId } })
export const rollbackVersion = (appId, version) =>
  client.post('/app/version/rollback', { appId, version })

/** 用量 */
export const usageSummary = (params) => client.get('/usage/summary', { params })

/** 模板 */
export const listTemplates = () => client.get('/template/list')

/** 对话历史（游标分页） */
export const chatHistory = (appId, cursor, pageSize = 20) =>
  client.get('/chat/history', { params: { appId, cursor, pageSize } })

/**
 * SSE 对话生成（EventSource，token 走查询参数）
 * @returns {EventSource}
 */
export function openChatStream(appId, message) {
  const token = localStorage.getItem('aicoding_token') || ''
  const url = `/api/chat/stream?appId=${appId}&message=${encodeURIComponent(message)}&token=${encodeURIComponent(token)}`
  return new EventSource(url)
}
