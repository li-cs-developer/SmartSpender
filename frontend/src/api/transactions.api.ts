import http from './http'
import type { ApiResponse } from '@/types/api'
import type { PeriodMode } from '@/types/period'
import type { Transaction, TransactionType } from '@/types/transaction'

export interface CreateTransactionPayload {
  amount: number
  type: TransactionType
  name: string
  notes?: string | null
  date: string
  categoryId: number
}

function periodParams(mode: PeriodMode, year: number, month: number | null) {
  return mode === 'MONTH'
    ? { period: 'MONTH', year, month }
    : { period: 'YEAR', year }
}

export const transactionsApi = {
  async list(mode: PeriodMode, year: number, month: number | null): Promise<Transaction[]> {
    const { data } = await http.get<ApiResponse<Transaction[]>>('/transactions', {
      params: periodParams(mode, year, month),
    })
    return data.data ?? []
  },
  async create(payload: CreateTransactionPayload): Promise<Transaction> {
    const { data } = await http.post<ApiResponse<Transaction>>('/transactions', payload)
    if (!data.data) throw new Error('Empty create response')
    return data.data
  },
}