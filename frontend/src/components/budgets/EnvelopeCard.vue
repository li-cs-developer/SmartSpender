<script setup lang="ts">
import { computed } from 'vue'
import Button from 'primevue/button'
import type { EnvelopeStatus } from '@/types/dashboard'
import { formatCurrency, percentUsed } from '@/utils/formatters'
import { daysRemainingInMonth, isCurrentMonth } from '@/utils/periodMath'

const props = defineProps<{
  envelope: EnvelopeStatus
  year: number
  month: number
}>()

const emit = defineEmits<{
  edit: [envelope: EnvelopeStatus]
  delete: [envelope: EnvelopeStatus]
}>()

const utilizationClass = computed(() => {
  switch (props.envelope.utilization) {
    case 'EXCEEDED': return 'sp-progress-exceeded'
    case 'WARNING':  return 'sp-progress-warning'
    default:         return 'sp-progress-ok'
  }
})

const barWidth = computed(() => Math.min(props.envelope.percentUsed * 100, 100))

const daysLeft = computed(() => {
  if (!isCurrentMonth(props.year, props.month)) return null
  return daysRemainingInMonth(props.year, props.month)
})
</script>

<template>
  <div class="envelope sp-card" :class="utilizationClass">
    <div class="header">
      <span class="dot" :style="{ background: envelope.categoryColor }" />
      <span class="name text-truncate" :title="envelope.categoryName">
        {{ envelope.categoryName }}
      </span>
      <span class="pct">{{ percentUsed(envelope.percentUsed) }}</span>
    </div>

    <div class="bar-track">
      <div class="bar-fill" :style="{ width: barWidth + '%', background: 'var(--bar-color)' }" />
    </div>

    <div class="amounts">
      <span class="spent">
        <strong>{{ formatCurrency(envelope.spent) }}</strong>
        <span class="sp-text-muted"> spent of </span>
        <strong>{{ formatCurrency(envelope.limitAmount) }}</strong>
      </span>
      <span v-if="envelope.overage > 0" class="over">
        over by {{ formatCurrency(envelope.overage) }}
      </span>
      <span v-else class="sp-text-muted">
        {{ formatCurrency(envelope.remaining) }} left
      </span>
    </div>

    <div v-if="daysLeft !== null" class="days sp-text-muted">
      <i class="pi pi-calendar" />
      {{ daysLeft }} {{ daysLeft === 1 ? 'day' : 'days' }} left this month
    </div>

    <div class="actions">
      <Button
        icon="pi pi-pencil"
        label="Edit"
        text
        size="small"
        @click="emit('edit', envelope)"
      />
      <Button
        icon="pi pi-trash"
        label="Delete"
        text
        size="small"
        severity="danger"
        @click="emit('delete', envelope)"
      />
    </div>
  </div>
</template>

<style scoped>
.envelope {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-width: 0;
}

.header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  font-size: var(--text-base);
  min-width: 0;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.name {
  flex: 1;
  min-width: 0;
  font-weight: 600;
}

.pct {
  font-variant-numeric: tabular-nums;
  font-weight: 700;
  color: var(--bar-color);
  flex-shrink: 0;
  font-size: var(--text-lg);
}

.bar-track {
  height: 8px;
  background: var(--color-surface-2);
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 200ms ease;
}

.amounts {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-2);
  font-size: var(--text-sm);
  flex-wrap: wrap;
}

.spent {
  font-variant-numeric: tabular-nums;
}

.over {
  color: var(--color-danger);
  font-weight: 600;
}

.days {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
}

.days i {
  font-size: 0.875rem;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
  padding-top: var(--space-2);
  border-top: 1px solid var(--color-border);
}
</style>