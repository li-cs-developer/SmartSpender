<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import type { EnvelopeStatus } from '@/types/dashboard'
import { formatCurrency, percentUsed } from '@/utils/formatters'

const props = defineProps<{ envelopes: EnvelopeStatus[]; limit?: number }>()
const router = useRouter()

const topEnvelopes = computed(() => {
  const rank = (e: EnvelopeStatus) =>
    e.utilization === 'EXCEEDED' ? 0 : e.utilization === 'WARNING' ? 1 : 2
  return [...props.envelopes]
    .sort((a, b) => rank(a) - rank(b) || b.percentUsed - a.percentUsed)
    .slice(0, props.limit ?? 4)
})

function utilizationClass(env: EnvelopeStatus) {
  return `sp-progress-${env.utilization.toLowerCase()}`
}
function barWidth(env: EnvelopeStatus) {
  return Math.min(env.percentUsed * 100, 100)
}
</script>

<template>
  <div class="sp-card">
    <h3 class="card-title">Budget Envelopes</h3>
    <div v-if="envelopes.length === 0" class="empty sp-text-muted">
      No budgets set for this month yet.
    </div>
    <ul v-else class="list">
      <li
        v-for="env in topEnvelopes"
        :key="env.budgetId"
        :class="utilizationClass(env)"
        class="item"
      >
        <div class="row">
          <span class="dot" :style="{ background: env.categoryColor }" />
          <span class="name text-truncate" :title="env.categoryName">
            {{ env.categoryName }}
          </span>
          <span class="pct">{{ percentUsed(env.percentUsed) }}</span>
        </div>
        <div class="bar-track">
          <div class="bar-fill" :style="{ width: barWidth(env) + '%', background: 'var(--bar-color)' }" />
        </div>
        <div class="row details sp-text-muted">
          <span>{{ formatCurrency(env.spent) }} / {{ formatCurrency(env.limitAmount) }}</span>
          <span v-if="env.overage > 0" class="over">over {{ formatCurrency(env.overage) }}</span>
          <span v-else>{{ formatCurrency(env.remaining) }} left</span>
        </div>
      </li>
    </ul>
    <div v-if="envelopes.length > 0" class="footer">
      <Button label="View all" icon="pi pi-arrow-right" icon-pos="right" text size="small" @click="router.push('/budgets')" />
    </div>
  </div>
</template>

<style scoped>
.card-title {
  margin: 0 0 var(--space-4);
  font-size: var(--text-base);
  font-weight: 600;
}
.empty { font-size: var(--text-sm); padding: var(--space-4) 0; }
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.item { display: flex; flex-direction: column; gap: var(--space-2); min-width: 0; }
.row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  min-width: 0;
}
.dot { width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0; }
.name { flex: 1; min-width: 0; font-weight: 500; }
.pct {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  color: var(--bar-color);
  flex-shrink: 0;
}
.bar-track {
  height: 5px;
  background: var(--color-surface-2);
  border-radius: 3px;
  overflow: hidden;
}
.bar-fill { height: 100%; border-radius: 3px; transition: width 200ms ease; }
.details { justify-content: space-between; font-size: var(--text-xs); }
.over { color: var(--color-danger); font-weight: 500; }
.footer { margin-top: var(--space-4); display: flex; justify-content: flex-end; }
</style>