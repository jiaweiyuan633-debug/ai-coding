import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi, getLoginUser } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('aicoding_token') || '',
    user: null
  }),
  getters: {
    isLogin: (state) => !!state.token,
    isAdmin: (state) => state.user?.userRole === 'admin'
  },
  actions: {
    async login(userAccount, userPassword) {
      const res = await loginApi({ userAccount, userPassword })
      this.token = res.data.token
      this.user = res.data.user
      localStorage.setItem('aicoding_token', this.token)
    },
    async register(userAccount, userPassword, checkPassword) {
      await registerApi({ userAccount, userPassword, checkPassword })
    },
    async fetchUser() {
      if (!this.token) return
      try {
        const res = await getLoginUser()
        this.user = res.data
      } catch (e) {
        this.logout()
      }
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('aicoding_token')
    }
  }
})
