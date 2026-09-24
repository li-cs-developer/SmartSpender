<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PeriodSwitcher from '@/components/common/PeriodSwitcher.vue'
import { useTransactionsStore } from '@/stores/transactions'
import { usePeriodStore } from '@/stores/period'
import { formatCurrency, formatDate } from '@/utils/formatters'
import type { Transaction } from '@/types/transaction'

const store = useTransactionsStore()
const period = usePeriodStore()
const rows = ref(20)

function refresh() { store.load() }
onMounted(refresh)
watch(() => [period.mode, period.year, period.month], refresh)

const netTotal = computed(() =>
  store.items.reduce((s, t) => s + (t.type === 'EXPENSE' ? -t.amount : t.amount), 0)
)
</script>

<template>
  <div class="sp-stack-lg">
    <header class="page-header">
      <div>
        <h1>Transactions</h1>
        <p class="sp-text-muted sub">
          {{ store.items.length }} transactions · net {{ formatCurrency(netTotal) }}
        </p>
      </div>
      <div class="controls">
        <PeriodSwitcher />
        <label class="rows-control sp-text-muted">
          Show
          <select v-model.number="rows" class="rows-select">
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
        </label>
      </div>
    </header>

    <div class="sp-card">
      <div class="table-scroll">
        <DataTable
          :value="store.items"
          :loading="store.loading"
          :rows="rows"
          paginator
          :rows-per-page-options="[10, 20, 50, 100]"
          striped-rows
        >
          <template #empty>No transactions in this period.</template>

          <Column field="date" header="Date" sortable style="width: 140px;">
            <template #body="{ data }: { data: Transaction }">{{ formatDate(data.date) }}</template>
          </Column>

          <Column field="name" header="Name" sortable style="min-width: 200px;">
            <template #body="{ data }: { data: Transaction }">
              <span class="text-truncate" :title="data.name">{{ data.name }}</span>
            </template>
          </Column>

          <Column field="categoryName" header="Category" sortable style="min-width: 180px;">
            <template #body="{ data }: { data: Transaction }">
              <span v-if="data.categoryName" class="tag">
                <span class="dot" :style="{ background: data.categoryColor }" />
                <span class="text-truncate" :title="data.categoryName">{{ data.categoryName }}</span>
              </span>
            </template>
          </Column>

          <Column field="amount" header="Amount" sortable style="width: 140px;">
            <template #body="{ data }: { data: Transaction }">
              <span :class="data.type === 'INCOME' ? 'sp-amount-income' : 'sp-amount-expense'">
                {{ data.type === 'INCOME' ? '+' : '−' }}{{ formatCurrency(data.amount) }}
              </span>
            </template>
          </Column>
        </DataTable>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  flex-wrap: wrap;
}
.page-header h1 { font-size: var(--text-xl); margin-bottom: var(--space-1); }
.sub { margin: 0; font-size: var(--text-sm); }
.controls {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex-wrap: wrap;
}
.rows-control {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
}
.rows-select {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: var(--space-1) var(--space-2);
  background: var(--color-surface);
  color: var(--color-text);
  font-size: var(--text-sm);
}
.tag {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: 2px 8px;
  background: var(--color-surface-2);
  border-radius: 999px;
  font-size: var(--text-xs);
  max-width: 100%;
  min-width: 0;
}
.dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
</style>