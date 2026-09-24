export type TransactionType = 'EXPENSE' | 'INCOME'

export interface Transaction {
  id: number
  amount: number
  type: TransactionType
  name: string
  notes: string | null
  date: string
  categoryId: number
  categoryName: string
  categoryColor: string
}