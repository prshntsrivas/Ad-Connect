import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { get, post } from '../api'
import { useAuth } from '../auth'

type Deal = {
  id: number
  campaignId: number
  campaignTitle: string
  brandName: string
  brandUserId: number
  influencerUserId: number
  influencerName?: string
  amount: number
  status: string
  paymentId?: number
  deliverableUrl?: string
  history: { fromStatus?: string; toStatus: string; note?: string; createdAt: string }[]
}

export function Deals() {
  const nav = useNavigate()
  const [list, setList] = useState<Deal[]>([])
  useEffect(() => { get<{ content: Deal[] }>('/api/deals/mine').then(r => setList(r.content)) }, [])
  return (
    <div>
      <h2>My deals</h2>
      <table className="table">
        <thead><tr><th>Campaign</th><th>Amount</th><th>Status</th></tr></thead>
        <tbody>
          {list.map(d => (
            <tr key={d.id} data-testid="deal-row" style={{ cursor: 'pointer' }} onClick={() => nav(`/deals/${d.id}`)}>
              <td><Link to={`/deals/${d.id}`}>{d.campaignTitle}</Link></td>
              <td>₹{d.amount}</td>
              <td><span className="badge">{d.status}</span></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export function DealDetail() {
  const { id } = useParams()
  const { user } = useAuth()
  const nav = useNavigate()
  const [deal, setDeal] = useState<Deal | null>(null)
  const [url, setUrl] = useState('https://instagram.com/p/demo')
  const [rating, setRating] = useState(5)
  const [comment, setComment] = useState('Great collaboration')
  const [err, setErr] = useState('')
  const reload = () => get<Deal>(`/api/deals/${id}`).then(setDeal)
  useEffect(() => { reload() }, [id])

  const act = async (path: string, body?: unknown) => {
    setErr('')
    try { await post(path, body); await reload() } catch (e) { setErr((e as Error).message) }
  }

  const pay = async () => {
    setErr('')
    try {
      const intent = await post<{ id: number; status: string; provider: string }>(
        '/api/payments/intents',
        { dealId: Number(id), idempotencyKey: `ui-${id}-${Date.now()}` },
      )
      if (intent.provider === 'MOCK') {
        await post('/api/payments/mock/confirm', { paymentId: intent.id })
      }
      await reload()
    } catch (e) { setErr((e as Error).message) }
  }

  if (!deal) return <p>Loading…</p>
  const isBrand = user?.id === deal.brandUserId
  const isInf = user?.id === deal.influencerUserId
  return (
    <div>
      <h2>{deal.campaignTitle}</h2>
      <p>₹{deal.amount} · {deal.brandName} ↔ {deal.influencerName || deal.influencerUserId}</p>
      <span className="badge" data-testid="deal-status">{deal.status}</span>
      {err && <p className="err">{err}</p>}
      <div className="row" style={{ margin: '1rem 0' }}>
        {(deal.status === 'APPLIED' || deal.status === 'OFFERED') && (
          <>
            <button onClick={() => act(`/api/deals/${id}/accept`)}>Accept</button>
            <button className="secondary" onClick={() => act(`/api/deals/${id}/reject`, { note: 'Not a fit' })}>Reject</button>
          </>
        )}
        {isBrand && deal.status === 'ACCEPTED' && <button onClick={pay} data-testid="pay-btn">Fund deal (mock Stripe)</button>}
        {isInf && deal.status === 'FUNDED' && (
          <form onSubmit={(e: FormEvent) => { e.preventDefault(); act(`/api/deals/${id}/deliver`, { deliverableUrl: url, note: 'Posted' }) }}>
            <input value={url} onChange={e => setUrl(e.target.value)} />
            <button>Submit deliverable</button>
          </form>
        )}
        {isBrand && deal.status === 'DELIVERED' && (
          <>
            <button onClick={() => act(`/api/deals/${id}/approve`)}>Approve</button>
            <button className="secondary" onClick={() => act(`/api/deals/${id}/request-revision`, { note: 'Please reshoot in daylight' })}>Request revision</button>
          </>
        )}
        {deal.status === 'COMPLETED' && (
          <form onSubmit={(e: FormEvent) => { e.preventDefault(); act(`/api/deals/${id}/review`, { rating, comment }) }}>
            <label>Rating <input type="number" min={1} max={5} value={rating} onChange={e => setRating(Number(e.target.value))} /></label>
            <label>Comment <input value={comment} onChange={e => setComment(e.target.value)} /></label>
            <button>Leave review</button>
          </form>
        )}
        <button className="ghost" onClick={() => nav(`/chat?with=${isBrand ? deal.influencerUserId : deal.brandUserId}&deal=${deal.id}`)}>Open chat</button>
      </div>
      <div className="card">
        <h3>History</h3>
        {(deal.history || []).map((h, i) => (
          <div key={i}>{h.fromStatus || '—'} → {h.toStatus} {h.note && `· ${h.note}`}</div>
        ))}
      </div>
    </div>
  )
}
