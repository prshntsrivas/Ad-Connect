import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { get, post } from '../api'
import { useAuth } from '../auth'

type Campaign = {
  id: number
  title: string
  description: string
  niche: string
  platform: string
  budget: number
  minFollowers: number
  status: string
  brandName: string
  city?: string
  acceptingApplications: boolean
}

export function Campaigns() {
  const { user } = useAuth()
  const [list, setList] = useState<Campaign[]>([])
  const [form, setForm] = useState({ title: '', description: '', niche: 'fashion', platform: 'INSTAGRAM', budget: 20000, minFollowers: 5000, city: '' })
  const load = () => get<{ content: Campaign[] }>('/api/campaigns?status=OPEN').then(r => setList(r.content))
  useEffect(() => { load() }, [])
  const create = async (e: FormEvent) => {
    e.preventDefault()
    await post('/api/campaigns', form)
    setForm({ ...form, title: '', description: '' })
    load()
  }
  return (
    <div>
      <h2>Campaigns</h2>
      {user?.role === 'BRAND' && (
        <form className="card" onSubmit={create} data-testid="create-campaign">
          <h3>New campaign</h3>
          <label>Title <input value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} required /></label>
          <label>Description <textarea value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} required /></label>
          <div className="row">
            <label>Niche <input value={form.niche} onChange={e => setForm({ ...form, niche: e.target.value })} /></label>
            <label>Platform
              <select value={form.platform} onChange={e => setForm({ ...form, platform: e.target.value })}>
                <option>INSTAGRAM</option><option>YOUTUBE</option><option>TWITTER</option><option>MULTI</option>
              </select>
            </label>
            <label>Budget <input type="number" value={form.budget} onChange={e => setForm({ ...form, budget: Number(e.target.value) })} /></label>
          </div>
          <button>Publish</button>
        </form>
      )}
      <div className="grid">
        {list.map(c => (
          <Link key={c.id} to={`/campaigns/${c.id}`} className="card" data-testid="campaign-card">
            <strong>{c.title}</strong>
            <div className="muted">{c.brandName} · {c.niche} · {c.platform}</div>
            <div>₹{c.budget} · min {c.minFollowers} followers</div>
            <span className="badge">{c.status}</span>
          </Link>
        ))}
      </div>
    </div>
  )
}

export function CampaignDetail() {
  const { id } = useParams()
  const { user } = useAuth()
  const nav = useNavigate()
  const [c, setC] = useState<Campaign | null>(null)
  const [amount, setAmount] = useState(5000)
  const [message, setMessage] = useState('I would love to collaborate.')
  const [err, setErr] = useState('')
  useEffect(() => { get<Campaign>(`/api/campaigns/${id}`).then(setC) }, [id])
  const apply = async (e: FormEvent) => {
    e.preventDefault()
    setErr('')
    try {
      const deal = await post<{ id: number }>(`/api/campaigns/${id}/apply`, { amount, message })
      nav(`/deals/${deal.id}`)
    } catch (ex) { setErr((ex as Error).message) }
  }
  if (!c) return <p>Loading…</p>
  return (
    <div>
      <h2>{c.title}</h2>
      <div className="card">
        <p>{c.description}</p>
        <p className="muted">{c.brandName} · {c.niche} · {c.platform} · ₹{c.budget}</p>
        <span className="badge">{c.status}</span>
      </div>
      {user?.role === 'INFLUENCER' && c.acceptingApplications && (
        <form className="card" onSubmit={apply} data-testid="apply-form">
          <h3>Apply</h3>
          {err && <p className="err">{err}</p>}
          <label>Proposed amount <input type="number" value={amount} onChange={e => setAmount(Number(e.target.value))} /></label>
          <label>Pitch <textarea value={message} onChange={e => setMessage(e.target.value)} /></label>
          <button>Submit application</button>
        </form>
      )}
    </div>
  )
}

