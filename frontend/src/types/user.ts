export interface User {
  id: number
  email: string
  displayName: string
  currencyPref: string
}

export interface AuthResponse {
  token: string
  userId: number
  email: string
  displayName: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  displayName: string
}