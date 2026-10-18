const ACCESS = 'adconnect.access'
const REFRESH = 'adconnect.refresh'

export type Role = 'BRAND' | 'INFLUENCER' | 'MANAGER' | 'ADMIN'

export type User = {
  id: number
  email: string
  fullName: string
  phone?: string
  role: Role
  status: string
  statusReason?: string
}

export type AuthResponse = { accessToken: string; refreshToken: string; expiresIn: number; user: User }

export class ApiError extends Error {
  status: number
  body: unknown
  constructor(status: number, body: unknown) {
    super(typeof body === 'object' && body && 'message' in body ? String((body as { message: string }).message) : `HTTP ${status}`)
    this.status = status
    this.body = body
  }
}

export function getAccess() { return localStorage.getItem(ACCESS) }
export function setTokens(a: string, r: string) {
  localStorage.setItem(ACCESS, a)
  localStorage.setItem(REFRESH, r)
}
export function clearTokens() {
  localStorage.removeItem(ACCESS)
  localStorage.removeItem(REFRESH)
}

let refreshing: Promise<boolean> | null = null

async function refreshTokens(): Promise<boolean> {
  const refresh = localStorage.getItem(REFRESH)
  if (!refresh) return false
  const res = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: refresh }),
  })
  if (!res.ok) {
    clearTokens()
    return false
  }
  const data: AuthResponse = await res.json()
  setTokens(data.accessToken, data.refreshToken)
  return true
}

export async function api<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const token = getAccess()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  const res = await fetch(path, { ...init, headers })
  if (res.status === 401 && retry && localStorage.getItem(REFRESH)) {
    refreshing ??= refreshTokens().finally(() => { refreshing = null })
    if (await refreshing) return api<T>(path, init, false)
  }
  if (res.status === 204) return undefined as T
  const text = await res.text()
  const body = text ? JSON.parse(text) : null
  if (!res.ok) throw new ApiError(res.status, body)
  return body as T
}

export const get = <T>(path: string) => api<T>(path)
export const post = <T>(path: string, body?: unknown) =>
  api<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) })
export const put = <T>(path: string, body: unknown) =>
  api<T>(path, { method: 'PUT', body: JSON.stringify(body) })
export const patch = <T>(path: string, body: unknown) =>
  api<T>(path, { method: 'PATCH', body: JSON.stringify(body) })
