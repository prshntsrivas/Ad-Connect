import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, clearTokens, getAccess, post, setTokens, type AuthResponse, type User } from './api'

type AuthCtx = {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<User>
  register: (payload: { email: string; password: string; fullName: string; role: string }) => Promise<User>
  logout: () => void
  refreshMe: () => Promise<void>
}

const Ctx = createContext<AuthCtx | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  const apply = (res: AuthResponse) => {
    setTokens(res.accessToken, res.refreshToken)
    setUser(res.user)
    return res.user
  }

  const refreshMe = async () => {
    if (!getAccess()) { setUser(null); return }
    try { setUser(await api<User>('/api/auth/me')) } catch { setUser(null); clearTokens() }
  }

  useEffect(() => { refreshMe().finally(() => setLoading(false)) }, [])

  const value = useMemo<AuthCtx>(() => ({
    user, loading, refreshMe,
    login: (email, password) => post<AuthResponse>('/api/auth/login', { email, password }).then(apply),
    register: (payload) => post<AuthResponse>('/api/auth/register', payload).then(apply),
    logout: () => { const r = localStorage.getItem('adconnect.refresh'); if (r) post('/api/auth/logout', { refreshToken: r }).catch(() => {}); clearTokens(); setUser(null) },
  }), [user, loading])

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>
}

export function useAuth() {
  const ctx = useContext(Ctx)
  if (!ctx) throw new Error('useAuth outside provider')
  return ctx
}
