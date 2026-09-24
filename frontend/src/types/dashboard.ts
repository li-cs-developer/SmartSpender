import type { PeriodMode } from './period'

export interface SpendingSummary {
  totalExpenses: number
  totalIncome: number
  netFlow: number
  expenseCount: number
  incomeCount: number
  periodDays: number
  averagePerDay: number
  averagePerMonth: number
  periodLabel: string
  periodMode: PeriodMode
  year: number
  month: number | null
}

export interface CategorySpending {
  categoryId: number
  categoryName: string
  categoryColor: string
  total: number
}

export type Utilization = 'OK' | 'WARNING' | 'EXCEEDED'

export interface EnvelopeStatus {
  budgetId: number
  categoryId: number
  categoryName: string
  categoryColor: string
  limitAmount: number
  spent: number
  remaining: number
  overage: number
  percentUsed: number
  utilization: Utilization
  needsAttention: boolean
}