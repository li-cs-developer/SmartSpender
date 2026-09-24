import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { budgetsApi, type BudgetRequest } from '@/api/budgets.api'
import type { EnvelopeStatus } from '@/types/dashboard'

export const useBudgetsStore = defineStore('budgets', () => {
  // The currently viewed period. Budgets are per-month, so we always
  // carry a specific year+month regardless of the dashboard's mode.
  const now = new Date()
  const year = ref(now.getFullYear())
  const month = ref(now.getMonth() + 1)

  const items = ref<EnvelopeStatus[]>([])
  const loading = ref(false)
  const loaded = ref(false)

  /** The previous month (handles year rollover). */
  const previousPeriod = computed(() => {
    if (month.value === 1) return { year: year.value - 1, month: 12 }
    return { year: year.value, month: month.value - 1 }
  })

  async function load(force = false): Promise<void> {
    if (loaded.value && !force) return
    loading.value = true
    try {
      items.value = await budgetsApi.envelopes(year.value, month.value)
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  async function reload(): Promise<void> {
    return load(true)
  }

  async function setPeriod(y: number, m: number): Promise<void> {
    year.value = y
    month.value = m
    loaded.value = false
    await load(true)
  }

  async function upsert(payload: BudgetRequest): Promise<void> {
    await budgetsApi.upsert(payload)
    await reload()
  }

  async function remove(budgetId: number): Promise<void> {
    await budgetsApi.remove(budgetId)
    items.value = items.value.filter((e) => e.budgetId !== budgetId)
  }

  async function copyFromPrevious(): Promise<number> {
    const prev = previousPeriod.value
    const copied = await budgetsApi.copyFrom(
      prev.year,
      prev.month,
      year.value,
      month.value
    )
    await reload()
    return copied
  }

  return {
    year,
    month,
    items,
    loading,
    loaded,
    previousPeriod,
    load,
    reload,
    setPeriod,
    upsert,
    remove,
    copyFromPrevious,
  }
})