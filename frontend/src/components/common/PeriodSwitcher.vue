<script setup lang="ts">
import { computed } from 'vue'
import Button from 'primevue/button'
import Select from 'primevue/select'
import SelectButton from 'primevue/selectbutton'
import { usePeriodStore } from '@/stores/period'
import type { PeriodMode } from '@/types/period'

const period = usePeriodStore()

const modeOptions: { label: string; value: PeriodMode }[] = [
  { label: 'Month', value: 'MONTH' },
  { label: 'Year', value: 'YEAR' },
]

const monthValue = computed({
  get: () => period.month,
  set: (v: number) => period.setMonth(v),
})
const yearValue = computed({
  get: () => period.year,
  set: (v: number) => period.setYear(v),
})
</script>

<template>
  <div class="switcher">
    <Button icon="pi pi-chevron-left" text rounded size="small" @click="period.previous()" />

    <SelectButton
      :model-value="period.mode"
      :options="modeOptions"
      option-label="label"
      option-value="value"
      :allow-empty="false"
      @update:model-value="(v) => period.setMode(v as PeriodMode)"
    />

    <Select
      v-if="period.mode === 'MONTH'"
      v-model="monthValue"
      :options="period.monthOptions"
      option-label="label"
      option-value="value"
      style="min-width: 130px;"
    />

    <Select
      v-model="yearValue"
      :options="period.yearOptions"
      style="min-width: 100px;"
    />

    <Button icon="pi pi-chevron-right" text rounded size="small" @click="period.next()" />
    <Button label="Today" text size="small" @click="period.today()" />
  </div>
</template>

<style scoped>
.switcher {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}
</style>