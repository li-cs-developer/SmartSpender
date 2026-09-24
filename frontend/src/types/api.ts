export interface ApiError {
  code: string
  message: string
  fields?: { field: string; message: string }[]
}

export interface ApiResponse<T> {
  data: T | null
  error?: ApiError
  timestamp: string
}