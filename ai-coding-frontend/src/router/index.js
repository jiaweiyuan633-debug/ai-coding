import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '../components/BasicLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: BasicLayout,
    children: [
      { path: '', name: 'home', component: () => import('../views/HomeView.vue'), meta: { title: '创作' } },
      { path: 'my', name: 'my', component: () => import('../views/MyAppsView.vue'), meta: { title: '我的应用', auth: true } },
      { path: 'square', name: 'square', component: () => import('../views/SquareView.vue'), meta: { title: '广场' } },
      { path: 'workspace/:appId', name: 'workspace', component: () => import('../views/WorkspaceView.vue'), meta: { title: '工作台', auth: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('aicoding_token')
  if (to.meta.auth && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})

router.afterEach((to) => {
  document.title = `${to.meta.title || ''} · AI Coding`
})

export default router
