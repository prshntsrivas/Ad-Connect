import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { setTokens } from '../api'
import { useAuth } from '../auth'

export function LoginPage() {
  const { login } = useAuth()
  const nav = useNavigate()
  const [email, setEmail] = useState('manager@adconnect.local')
  const [password, setPassword] = useState('Manager@123')
  const [err, setErr] = useState('')
  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setErr('')
    try { await login(email, password); nav('/') } catch (ex) { setErr((ex as Error).message) }
  }
  return (
    <div className="auth-wrap">
      <form className="card auth-card" onSubmit={submit} data-testid="login-form">
        <h2>Sign in to Ad-Connect</h2>
        <p className="muted">Brand ↔ influencer marketplace</p>
        {err && <p className="err">{err}</p>}
        <label>Email <input value={email} onChange={e => setEmail(e.target.value)} required /></label>
        <label>Password <input type="password" value={password} onChange={e => setPassword(e.target.value)} required /></label>
        <button type="submit">Sign in</button>
        <p><Link to="/register">Create an account</Link></p>
      </form>
    </div>
  )
}

export function RegisterPage() {
  const { register } = useAuth()
  const nav = useNavigate()
  const [form, setForm] = useState({ email: '', password: 'Passw0rd!', fullName: '', role: 'BRAND' })
  const [err, setErr] = useState('')
  const set = (k: string, v: string) => setForm(f => ({ ...f, [k]: v }))
  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setErr('')
    try { await register(form); nav('/') } catch (ex) { setErr((ex as Error).message) }
  }
  return (
    <div className="auth-wrap">
      <form className="card auth-card" onSubmit={submit} data-testid="register-form">
        <h2>Create account</h2>
        {err && <p className="err">{err}</p>}
        <label>Full name <input value={form.fullName} onChange={e => set('fullName', e.target.value)} required /></label>
        <label>Email <input type="email" value={form.email} onChange={e => set('email', e.target.value)} required /></label>
        <label>Password <input type="password" value={form.password} onChange={e => set('password', e.target.value)} required /></label>
        <label>I am a
          <select value={form.role} onChange={e => set('role', e.target.value)}>
            <option value="BRAND">Brand</option>
            <option value="INFLUENCER">Influencer</option>
          </select>
        </label>
        <button type="submit">Register</button>
        <p className="muted">New accounts start as PENDING until a site manager verifies them.</p>
        <p><Link to="/login">Already have an account</Link></p>
      </form>
    </div>
  )
}

export function OAuthCallback() {
  const nav = useNavigate()
  const [params] = useSearchParams()
  useEffect(() => {
    const hash = new URLSearchParams(window.location.hash.replace('#', ''))
    const access = hash.get('accessToken')
    const refresh = hash.get('refreshToken')
    if (access && refresh) {
      setTokens(access, refresh)
      nav('/')
      window.location.reload()
    } else if (params.get('oauthError')) {
      nav('/login')
    }
  }, [nav, params])
  return <p>Finishing sign-in…</p>
}
