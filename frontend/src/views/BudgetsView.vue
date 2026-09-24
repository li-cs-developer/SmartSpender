<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Button from 'primevue/button'
import Select from 'primevue/select'
import ConfirmDialog from 'primevue/confirmdialog'
import { useConfirm } from 'primevue/useconfirm'
import EnvelopeCard from '@/components/budgets/EnvelopeCard.vue'
import BudgetFormDialog from '@/components/budgets/BudgetFormDialog.vue'
import { useBudgetsStore } from '@/stores/budgets'
import { useCategoriesStore } from '@/stores/categories'
import { formatPeriod, monthName } from '@/utils/periodMath'
import type { EnvelopeStatus } from '@/types/dashboard'

const budgets = useBudgetsStore()
const categories = useCategoriesStore()
const confirm = useConfirm()

const formVisible = ref(false)
const editing = ref<EnvelopeStatus | null>(null)
const errorMsg = ref<string | null>(null)

const monthOptions = Array.from({ length: 12 }, (_, i) => ({
  label: monthName(i + 1),
  value: i + 1,
}))
const yearOptions = computed(() => {
  const now = new Date().getFullYear()
  return Array.from({ length: 5 }, (_, i) => now - i + 1)
})

const monthValue = computed({
  get: () => budgets.month,
  set: (m: number) => budgets.setPeriod(budgets.year, m),
})
const yearValue = computed({
  get: () => budgets.year,
  set: (y: number) => budgets.setPeriod(y, budgets.month),
})

const periodLabel = computed(() => formatPeriod(budgets.year, budgets.month))

// Total of all envelopes for the summary line
const totals = computed(() => {
  const spent = budgets.items.reduce((s, e) => s + e.spent, 0)
  const limit = budgets.items.reduce((s, e) => s + e.limitAmount, 0)
  return { spent, limit }
})

onMounted(() => {
  budgets.load(true)
  categories.load()
})

function openCreate() {
  editing.value = null
  formVisible.value = true
}

function openEdit(env: EnvelopeStatus) {
  editing.value = env
  formVisible.value = true
}

function onDelete(env: EnvelopeStatus) {
  confirm.require({
    message: `Delete the "${env.categoryName}" budget for ${periodLabel.value}?`,
    header: 'Delete Budget',
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: 'Cancel', severity: 'secondary', outlined: true },
    acceptProps: { label: 'Delete', severity: 'danger' },
    accept: async () => {
      try {
        await budgets.remove(env.budgetId)
      } catch (e) {
        errorMsg.value = (e as Error).message
      }
    },
  })
}

async function copyFromPrevious() {
  errorMsg.value = null
  try {
    await budgets.copyFromPrevious()
  } catch (e) {
    errorMsg.value = (e as Error).message
  }
}
</script>

<template>
  <div class="sp-stack-lg">
    <header class="page-header">
      <div>
        <h1>Budgets</h1>
        <p class="sp-text-muted sub">
          Soft limits per category for
          <strong>{{ periodLabel }}</strong>.
        </p>
      </div>

      <div class="header-controls">
        <Select v-model="monthValue" :options="monthOptions" option-label="label" option-value="value" style="min-width: 130px;" />
        <Select v-model="yearValue" :options="yearOptions" style="min-width: 100px;" />
        <Button label="New Budget" icon="pi pi-plus" @click="openCreate" />
      </div>
    </header>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

    <!-- Summary line -->
    <div v-if="budgets.items.length > 0" class="summary sp-text-muted">
      <i class="pi pi-wallet" />
      <span>
        {{ budgets.items.length }} {{ budgets.items.length === 1 ? 'envelope' : 'envelopes' }} ·
        ${{ totals.spent.toFixed(2) }} spent of ${{ totals.limit.toFixed(2) }} budgeted
      </span>
    </div>

    <!-- Empty state with carry-over prompt -->
    <template v-if="!budgets.loading && budgets.items.length === 0">
      <div class="sp-card empty-state">
        <i class="pi pi-wallet empty-icon" />
        <h3>No budgets for {{ periodLabel }}</h3>
        <p class="sp-text-muted">
          Create budgets to track your spending against soft limits.
        </p>
        <div class="empty-actions">
          <Button label="New budget" icon="pi pi-plus" @click="openCreate" />
          <Button
            label="Copy from previous period"
            icon="pi pi-copy"
            severity="secondary"
            outlined
            @click="copyFromPrevious"
          />
        </div>
        <p class="sp-text-muted copy-hint">
          Copying looks for budgets in <strong>{{ formatPeriod(budgets.previousPeriod.year, budgets.previousPeriod.month) }}</strong>.
        </p>
      </div>
    </template>

    <!-- Grid of envelopes -->
    <div v-else class="envelope-grid">
      <EnvelopeCard
        v-for="env in budgets.items"
        :key="env.budgetId"
        :envelope="env"
        :year="budgets.year"
        :month="budgets.month"
        @edit="openEdit"
        @delete="onDelete"
      />
    </div>

    <BudgetFormDialog v-model:visible="formVisible" :existing="editing" />

    <ConfirmDialog />
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

.page-header h1 {
  font-size: var(--text-xl);
  margin-bottom: var(--space-1);
}

.sub {
  margin: 0;
  font-size: var(--text-sm);
}

.header-controls {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.summary {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
}

.envelope-grid {
  display: grid;
  gap: var(--space-5);
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
}

@media (max-width: 640px) {
  .envelope-grid {
    grid-template-columns: 1fr;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: var(--space-10) var(--space-6);
  gap: var(--space-3);
}

.empty-icon {
  font-size: 2.5rem;
  color: var(--color-text-muted);
}

.empty-state h3 {
  font-size: var(--text-lg);
}

.empty-state p {
  margin: 0;
  max-width: 420px;
}

.empty-actions {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
  justify-content: center;
  margin-top: var(--space-2);
}

.copy-hint {
  font-size: var(--text-xs);
  margin-top: var(--space-2);
}

.error {
  color: var(--color-danger);
  font-size: var(--text-sm);
}
</style>