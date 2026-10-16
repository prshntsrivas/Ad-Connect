import { useEffect, useState } from 'react'
import { get, patch, post } from '../api'

type U = { id: number; email: string; fullName: string; role: string; status: string }

export function Manager() {
  const [users, setUsers] = useState<U[]>([])
  const [status, setStatus] = useState('PENDING')
  const [q, setQ] = useState('')
  const [applied, setApplied] = useState('')
  const [mgr, setMgr] = useState({ email: '', password: 'Manager@123', fullName: 'New Manager' })
  const load = () => get<{ content: U[] }>(`/api/users?status=${status}&q=${encodeURIComponent(applied)}&size=50`).then(r => setUsers(r.content))
  useEffect(() => { load() }, [status, applied])
  const setSt = (id: number, next: string) => patch(`/api/users/${id}/status`, { status: next }).then(load)
  return (
    <div>
      <h2>Site manager</h2>
      <div className="row">
        <label>Filter
          <select value={status} onChange={e => setStatus(e.target.value)}>
            <option>PENDING</option><option>VERIFIED</option><option>REJECTED</option><option>SUSPENDED</option>
          </select>
        </label>
        <label>Search
          <input placeholder="Search email" value={q} onChange={e => setQ(e.target.value)}
                 data-testid="user-search"
                 onKeyDown={e => { if (e.key === 'Enter') setApplied(q) }} />
        </label>
        <button type="button" className="ghost" onClick={() => setApplied(q)}>Find</button>
      </div>
      <table className="table" data-testid="user-table">
        <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th></th></tr></thead>
        <tbody>
          {users.map(u => (
            <tr key={u.id}>
              <td>{u.fullName}</td><td>{u.email}</td><td>{u.role}</td><td>{u.status}</td>
              <td className="row">
                {u.status !== 'VERIFIED' && <button onClick={() => setSt(u.id, 'VERIFIED')}>Verify</button>}
                {u.status === 'PENDING' && <button className="secondary" onClick={() => setSt(u.id, 'REJECTED')}>Reject</button>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <form className="card" onSubmit={e => { e.preventDefault(); post('/api/users/managers', mgr).then(() => alert('Manager created')) }}>
        <h3>Add manager (admin)</h3>
        <label>Email <input value={mgr.email} onChange={e => setMgr({ ...mgr, email: e.target.value })} /></label>
        <label>Name <input value={mgr.fullName} onChange={e => setMgr({ ...mgr, fullName: e.target.value })} /></label>
        <button>Create manager</button>
      </form>
    </div>
  )
}
