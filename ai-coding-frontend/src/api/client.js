import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  timeout: 30000
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('aicoding_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code !== 'undefined' && body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body.data !== undefined ? body : body
  },
  (error) => Promise.reject(error)
)

export function getToken() {
  return localStorage.getItem('aicoding_token') || ''
}

export default client
