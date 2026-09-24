import { defineStore } from 'pinia'
import { ref } from 'vue'
import { transactionsApi, type CreateTransactionPayload } from '@/api/transactions.api'
import { usePeriodStore } from '@/stores/period'
import type { Transaction } from '@/types/transaction'

export const useTransactionsStore = defineStore('transactions', () => {
  const items = ref<Transaction[]>([])
  const loading = ref(false)

  async function load(): Promise<void> {
    loading.value = true
    const period = usePeriodStore()
    try {
      items.value = await transactionsApi.list(period.mode, period.year, period.monthForApi)
    } finally {
      loading.value = false
    }
  }

  async function create(payload: CreateTransactionPayload): Promise<Transaction> {
    const created = await transactionsApi.create(payload)
    items.value = [created, ...items.value]
    return created
  }

  return { items, loading, load, create }
})