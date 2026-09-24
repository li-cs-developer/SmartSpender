const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
]

export function monthName(month: number): string {
  return MONTH_NAMES[month - 1] ?? '?'
}

export function formatPeriod(year: number, month: number): string {
  return `${monthName(month)} ${year}`
}

/**
 * How many days remain in the given month, INCLUDING today.
 * Returns 0 if the month has already passed, and the full length if it's in the future.
 */
export function daysRemainingInMonth(year: number, month: number): number {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())

  const target = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)  // day 0 of next month = last day of this month

  if (today > lastDay) return 0
  if (today < target) {
    return Math.round((lastDay.getTime() - target.getTime()) / 86400000) + 1
  }
  return Math.round((lastDay.getTime() - today.getTime()) / 86400000) + 1
}

export function isCurrentMonth(year: number, month: number): boolean {
  const now = new Date()
  return now.getFullYear() === year && now.getMonth() + 1 === month
}