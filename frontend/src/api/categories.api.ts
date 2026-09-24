import http from './http'
import type { ApiResponse } from '@/types/api'
import type { Category, CategoryRequest } from '@/types/category'

export const categoriesApi = {
  async list(): Promise<Category[]> {
    const { data } = await http.get<ApiResponse<Category[]>>('/categories')
    return data.data ?? []
  },
  async create(payload: CategoryRequest): Promise<Category> {
    const { data } = await http.post<ApiResponse<Category>>('/categories', payload)
    if (!data.data) throw new Error('Empty create response')
    return data.data
  },
  async update(id: number, payload: CategoryRequest): Promise<Category> {
    const { data } = await http.put<ApiResponse<Category>>(`/categories/${id}`, payload)
    if (!data.data) throw new Error('Empty update response')
    return data.data
  },
  async remove(id: number): Promise<void> {
    await http.delete(`/categories/${id}`)
  },
}