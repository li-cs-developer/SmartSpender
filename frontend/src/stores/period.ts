import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { PeriodMode } from '@/types/period'

const STORAGE_KEY = 'sp_period'
const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
]

function readStored() {
  const now = new Date()
  const fallback = { mode: 'MONTH' as PeriodMode, year: now.getFullYear(), month: now.getMonth() + 1 }
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return fallback
    const parsed = JSON.parse(raw)
    if ((parsed.mode === 'MONTH' || parsed.mode === 'YEAR') && typeof parsed.year === 'number') {
      return {
        mode: parsed.mode as PeriodMode,
        year: parsed.year,
        month: typeof parsed.month === 'number' ? parsed.month : fallback.month,
      }
    }
  } catch { /* ignore */ }
  return fallback
}

export const usePeriodStore = defineStore('period', () => {
  const initial = readStored()
  const mode = ref<PeriodMode>(initial.mode)
  const year = ref(initial.year)
  const month = ref(initial.month)

  const monthForApi = computed(() => mode.value === 'MONTH' ? month.value : null)
  const label = computed(() =>
    mode.value === 'MONTH' ? `${MONTHS[month.value - 1]} ${year.value}` : String(year.value)
  )
  const yearOptions = computed(() => {
    const now = new Date().getFullYear()
    return Array.from({ length: 5 }, (_, i) => now - i)
  })
  const monthOptions = computed(() => MONTHS.map((label, i) => ({ label, value: i + 1 })))

  function persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ mode: mode.value, year: year.value, month: month.value }))
  }
  function setMode(m: PeriodMode) { mode.value = m; persist() }
  function setYear(y: number) { year.value = y; persist() }
  function setMonth(m: number) { month.value = m; persist() }

  function previous() {
    if (mode.value === 'MONTH') {
      if (month.value === 1) { month.value = 12; year.value -= 1 } else { month.value -= 1 }
    } else { year.value -= 1 }
    persist()
  }
  function next() {
    if (mode.value === 'MONTH') {
      if (month.value === 12) { month.value = 1; year.value += 1 } else { month.value += 1 }
    } else { year.value += 1 }
    persist()
  }
  function today() {
    const now = new Date()
    year.value = now.getFullYear()
    month.value = now.getMonth() + 1
    persist()
  }

  return {
    mode, year, month,
    monthForApi, label, yearOptions, monthOptions,
    setMode, setYear, setMonth, previous, next, today,
  }
})