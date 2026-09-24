import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi } from '@/api/auth.api'
import type { LoginRequest, RegisterRequest, User } from '@/types/user'

const TOKEN_KEY = 'sp_token'
const USER_KEY = 'sp_user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const user = ref<User | null>(readStoredUser())

  const isAuthenticated = computed(() => !!token.value)

  async function login(payload: LoginRequest): Promise<void> {
    const auth = await authApi.login(payload)
    applyAuth(auth)
  }

  async function register(payload: RegisterRequest): Promise<void> {
    const auth = await authApi.register(payload)
    applyAuth(auth)
  }

  function logout(): void {
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  function applyAuth(auth: {
    token: string
    userId: number
    email: string
    displayName: string
  }): void {
    token.value = auth.token
    user.value = {
      id: auth.userId,
      email: auth.email,
      displayName: auth.displayName,
      currencyPref: 'USD',
    }
    localStorage.setItem(TOKEN_KEY, auth.token)
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  function readStoredUser(): User | null {
    const raw = localStorage.getItem(USER_KEY)
    if (!raw) return null
    try { return JSON.parse(raw) as User }
    catch { localStorage.removeItem(USER_KEY); return null }
  }

  return { token, user, isAuthenticated, login, register, logout }
})