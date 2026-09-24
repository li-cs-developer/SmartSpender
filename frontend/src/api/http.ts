import axios, { type AxiosInstance, type AxiosError } from 'axios'
import type { ApiResponse } from '@/types/api'

export class ApiError extends Error {
  readonly code: string
  readonly status: number | undefined

  constructor(code: string, message: string, status?: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }
}

const http: AxiosInstance = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('sp_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('sp_token')
      localStorage.removeItem('sp_user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    const envelope = error.response?.data
    const code = envelope?.error?.code ?? 'UNKNOWN'
    const message = envelope?.error?.message ?? error.message ?? 'Unexpected error'
    return Promise.reject(new ApiError(code, message, error.response?.status))
  }
)

export default http