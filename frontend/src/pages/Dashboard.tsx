import { useEffect, useState } from 'react'
import { get } from '../api'
import { useAuth } from '../auth'

type Dash = {
  role: string
  campaignsByStatus: Record<string, number>
  dealsByStatus: Record<string, number>
  money: number
  pendingMoney: number
  rating?: number
  completedDeals?: number
  profileComplete: boolean
}

export function Dashboard() {
  const { user } = useAuth()
  const [data, setData] = useState<Dash | null>(null)
  const [err, setErr] = useState('')
  useEffect(() => {
    get<Dash>('/api/dashboard').then(setData).catch(e => setErr(e.message))
  }, [])
  if (err) return <p className="err">{err}</p>
  if (!data) return <p>Loading…</p>
  return (
    <div>
      <h2>Welcome, {user?.fullName}</h2>
      {!data.profileComplete && <div className="card">Complete your profile to start working on campaigns.</div>}
      {user?.status !== 'VERIFIED' && <div className="card">Account status: <span className="badge warn">{user?.status}</span> — a manager must verify you before you can transact.</div>}
      <div className="stats">
        <div className="stat"><small>Committed / earned</small><b>₹{data.money ?? 0}</b></div>
        <div className="stat"><small>In escrow</small><b>₹{data.pendingMoney ?? 0}</b></div>
        {data.rating != null && <div className="stat"><small>Rating</small><b>{data.rating.toFixed(1)}</b></div>}
        {data.completedDeals != null && <div className="stat"><small>Completed deals</small><b>{data.completedDeals}</b></div>}
      </div>
      <div className="row" style={{ marginTop: '1rem' }}>
        <div className="card" style={{ flex: 1 }}>
          <h3>Campaigns</h3>
          {Object.entries(data.campaignsByStatus || {}).map(([k, v]) => <div key={k}>{k}: {v}</div>)}
          {Object.keys(data.campaignsByStatus || {}).length === 0 && <p className="muted">None yet</p>}
        </div>
        <div className="card" style={{ flex: 1 }}>
          <h3>Deals</h3>
          {Object.entries(data.dealsByStatus || {}).map(([k, v]) => <div key={k}>{k}: {v}</div>)}
          {Object.keys(data.dealsByStatus || {}).length === 0 && <p className="muted">None yet</p>}
        </div>
      </div>
    </div>
  )
}
