import http from './http'
import type { ApiResponse } from '@/types/api'
import type { EnvelopeStatus } from '@/types/dashboard'

export interface BudgetRequest {
  categoryId: number
  limitAmount: number
  month: number
  year: number
}

export const budgetsApi = {
  async envelopes(year: number, month: number): Promise<EnvelopeStatus[]> {
    const { data } = await http.get<ApiResponse<EnvelopeStatus[]>>(
      '/budgets/envelopes',
      { params: { year, month } }
    )
    return data.data ?? []
  },

  async upsert(payload: BudgetRequest): Promise<EnvelopeStatus> {
    const { data } = await http.put<ApiResponse<EnvelopeStatus>>(
      '/budgets',
      payload
    )
    if (!data.data) throw new Error('Empty upsert response')
    return data.data
  },

  async remove(budgetId: number): Promise<void> {
    await http.delete(`/budgets/${budgetId}`)
  },

  async copyFrom(
    fromYear: number,
    fromMonth: number,
    toYear: number,
    toMonth: number
  ): Promise<number> {
    const { data } = await http.post<ApiResponse<number>>(
      '/budgets/copy-from',
      null,
      { params: { fromYear, fromMonth, toYear, toMonth } }
    )
    return data.data ?? 0
  },
}