<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { usePeriodStore } from '@/stores/period'
import MetricCard from '@/components/common/MetricCard.vue'
import CategoryDonut from '@/components/charts/CategoryDonut.vue'
import BudgetListCard from '@/components/budgets/BudgetListCard.vue'
import PeriodSwitcher from '@/components/common/PeriodSwitcher.vue'
import { formatCurrency } from '@/utils/formatters'

const store = useDashboardStore()
const period = usePeriodStore()

function refresh() { store.load() }
onMounted(refresh)
watch(() => [period.mode, period.year, period.month], refresh)

const averageLabel = computed(() =>
  period.mode === 'YEAR' ? 'Avg / Month' : 'Avg / Day'
)
const averageValue = computed(() =>
  period.mode === 'YEAR' ? store.summary?.averagePerMonth : store.summary?.averagePerDay
)
</script>

<template>
  <div class="sp-stack-lg">
    <header class="page-header">
      <div>
        <h1>Dashboard</h1>
        <span class="sp-text-muted">{{ period.label }}</span>
      </div>
      <PeriodSwitcher />
    </header>

    <!-- ROW 1: 4 metrics, exact order from spec -->
    <section class="sp-grid-4">
      <MetricCard
        label="Total Income"
        :value="formatCurrency(store.summary?.totalIncome)"
        :hint="`${store.summary?.incomeCount ?? 0} deposits`"
        tone="income"
      />
      <MetricCard
        label="Total Expense"
        :value="formatCurrency(store.summary?.totalExpenses)"
        :hint="`${store.summary?.expenseCount ?? 0} spend events`"
        tone="expense"
      />
      <MetricCard
        label="Net Flow"
        :value="formatCurrency(store.summary?.netFlow)"
        hint="income − expenses"
        tone="neutral"
      />
      <MetricCard
        :label="averageLabel"
        :value="formatCurrency(averageValue)"
        :hint="`over ${store.summary?.periodDays ?? 0} days`"
        tone="neutral"
      />
    </section>

    <!-- ROW 2: 3 cards -->
    <section class="sp-grid-3">
      <CategoryDonut title="Expense by Category" :items="store.expenseByCategory" />
      <BudgetListCard :envelopes="store.envelopes" :limit="4" />
      <CategoryDonut title="Income by Category" :items="store.incomeByCategory" />
    </section>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  flex-wrap: wrap;
}
.page-header h1 {
  font-size: var(--text-xl);
  margin-bottom: var(--space-1);
}
</style>