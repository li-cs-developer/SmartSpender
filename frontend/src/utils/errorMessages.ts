import { ApiError } from '@/api/http'

const MESSAGES: Record<string, string> = {
  NOT_FOUND: 'Not found',
  INVALID_INPUT: 'Please check your input',
  VALIDATION_FAILED: 'Please fix the highlighted fields',
  DATA_INTEGRITY_VIOLATION: 'This change is not allowed',
  INTERNAL_ERROR: 'Something went wrong on our end',
  UNAUTHENTICATED: 'Please sign in to continue',
  EMAIL_ALREADY_REGISTERED: 'That email is already registered',
  INVALID_CREDENTIALS: 'Wrong email or password',
  CATEGORY_NAME_TAKEN: 'A category with that name already exists',
  CATEGORY_COLOR_TAKEN: 'That color is already used by another category',
  CATEGORY_NOT_OWNED: 'That category does not belong to you',
  CATEGORY_IN_USE: 'This category has transactions. Reassign them first.',
  AMOUNT_TOO_LARGE: 'Amount is too large',
}

export function toUserMessage(err: unknown): string {
  if (err instanceof ApiError) {
    return MESSAGES[err.code] ?? err.message ?? 'Something went wrong'
  }
  if (err instanceof Error) {
    return MESSAGES[err.message] ?? err.message
  }
  return 'Something went wrong'
}