<script setup lang="ts">
type Tone = 'income' | 'expense' | 'neutral'

defineProps<{
  label: string
  value: string
  hint?: string
  tone?: Tone
}>()
</script>

<template>
  <div class="sp-card metric">
    <div class="metric-label">{{ label }}</div>
    <div class="metric-value" :class="`tone-${tone ?? 'neutral'}`">{{ value }}</div>
    <div v-if="hint" class="metric-hint sp-text-muted">{{ hint }}</div>
  </div>
</template>

<style scoped>
.metric {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.metric-label {
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.metric-value {
  font-size: var(--text-2xl);
  font-weight: 700;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
  overflow-wrap: anywhere;
}

.tone-income  { color: var(--color-income); }
.tone-expense { color: var(--color-expense); }
.tone-neutral { color: var(--color-text); }

.metric-hint { font-size: var(--text-sm); }
</style>