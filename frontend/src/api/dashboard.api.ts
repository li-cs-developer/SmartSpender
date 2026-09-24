import http from './http'
import type { ApiResponse } from '@/types/api'
import type { PeriodMode } from '@/types/period'
import type { CategorySpending, EnvelopeStatus, SpendingSummary } from '@/types/dashboard'

type TxType = 'EXPENSE' | 'INCOME'

function periodParams(mode: PeriodMode, year: number, month: number | null) {
  return mode === 'MONTH'
    ? { period: 'MONTH', year, month }
    : { period: 'YEAR', year }
}

export const dashboardApi = {
  async summary(mode: PeriodMode, year: number, month: number | null): Promise<SpendingSummary> {
    const { data } = await http.get<ApiResponse<SpendingSummary>>(
      '/transactions/summary',
      { params: periodParams(mode, year, month) }
    )
    if (!data.data) throw new Error('Empty summary response')
    return data.data
  },
  async byCategory(mode: PeriodMode, year: number, month: number | null, type: TxType): Promise<CategorySpending[]> {
    const { data } = await http.get<ApiResponse<CategorySpending[]>>(
      '/transactions/by-category',
      { params: { ...periodParams(mode, year, month), type } }
    )
    return data.data ?? []
  },
  async envelopes(year: number, month: number): Promise<EnvelopeStatus[]> {
    const { data } = await http.get<ApiResponse<EnvelopeStatus[]>>(
      '/budgets/envelopes',
      { params: { year, month } }
    )
    return data.data ?? []
  },
}