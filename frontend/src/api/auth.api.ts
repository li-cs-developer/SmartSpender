import http from './http'
import type { ApiResponse } from '@/types/api'
import type { AuthResponse, LoginRequest, RegisterRequest } from '@/types/user'

export const authApi = {
  async login(payload: LoginRequest): Promise<AuthResponse> {
    const { data } = await http.post<ApiResponse<AuthResponse>>('/auth/login', payload)
    if (!data.data) throw new Error('Empty login response')
    return data.data
  },
  async register(payload: RegisterRequest): Promise<AuthResponse> {
    const { data } = await http.post<ApiResponse<AuthResponse>>('/auth/register', payload)
    if (!data.data) throw new Error('Empty register response')
    return data.data
  },
}