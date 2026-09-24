import { defineStore } from 'pinia'
import { ref } from 'vue'
import { dashboardApi } from '@/api/dashboard.api'
import { usePeriodStore } from '@/stores/period'
import type { CategorySpending, EnvelopeStatus, SpendingSummary } from '@/types/dashboard'

export const useDashboardStore = defineStore('dashboard', () => {
  const summary = ref<SpendingSummary | null>(null)
  const expenseByCategory = ref<CategorySpending[]>([])
  const incomeByCategory = ref<CategorySpending[]>([])
  const envelopes = ref<EnvelopeStatus[]>([])
  const loading = ref(false)

  async function load(): Promise<void> {
    loading.value = true
    const period = usePeriodStore()
    const now = new Date()
    try {
      const [s, exp, inc, e] = await Promise.all([
        dashboardApi.summary(period.mode, period.year, period.monthForApi),
        dashboardApi.byCategory(period.mode, period.year, period.monthForApi, 'EXPENSE'),
        dashboardApi.byCategory(period.mode, period.year, period.monthForApi, 'INCOME'),
        dashboardApi.envelopes(now.getFullYear(), now.getMonth() + 1),
      ])
      summary.value = s
      expenseByCategory.value = exp
      incomeByCategory.value = inc
      envelopes.value = e
    } finally {
      loading.value = false
    }
  }

  return { summary, expenseByCategory, incomeByCategory, envelopes, loading, load }
})