<script setup lang="ts">
import { computed } from 'vue'
import Chart from 'primevue/chart'
import type { CategorySpending } from '@/types/dashboard'

const props = defineProps<{
  items: CategorySpending[]
  title: string
}>()

const chartData = computed(() => ({
  labels: props.items.map((i) => i.categoryName),
  datasets: [{
    data: props.items.map((i) => i.total),
    backgroundColor: props.items.map((i) => i.categoryColor),
    borderWidth: 0,
  }],
}))

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        label: (ctx: any) => `${ctx.label}: $${(ctx.parsed as number).toFixed(2)}`,
      },
    },
  },
  cutout: '65%',
}))
</script>

<template>
  <div class="sp-card">
    <h3 class="card-title text-truncate" :title="title">{{ title }}</h3>
    <div v-if="items.length === 0" class="empty sp-text-muted">
      No data in this period.
    </div>
    <div v-else class="chart-wrap">
      <div class="chart">
        <Chart type="doughnut" :data="chartData" :options="chartOptions" />
      </div>
      <ul class="legend">
        <li v-for="item in items" :key="item.categoryId">
          <span class="dot" :style="{ background: item.categoryColor }" />
          <span class="legend-name text-truncate" :title="item.categoryName">
            {{ item.categoryName }}
          </span>
          <span class="legend-value">${{ item.total.toFixed(2) }}</span>
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.card-title {
  margin: 0 0 var(--space-4);
  font-size: var(--text-base);
  font-weight: 600;
}

.chart-wrap {
  display: flex;
  gap: var(--space-5);
  align-items: center;
}

.chart {
  position: relative;
  width: 150px;
  height: 150px;
  flex-shrink: 0;
}

.legend {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  flex: 1;
  min-width: 0;
}

.legend li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  min-width: 0;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-name { flex: 1; min-width: 0; }
.legend-value {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
  white-space: nowrap;
}

.empty {
  font-size: var(--text-sm);
  padding: var(--space-4) 0;
}
</style>