import { useEffect, useState } from 'react'
import { get, post } from '../api'

type Note = { id: string; title: string; body: string; type: string; read: boolean; createdAt: string; link?: string }

export function Inbox() {
  const [notes, setNotes] = useState<Note[]>([])
  const load = () => get<{ content: Note[] }>('/api/notifications').then(r => setNotes(r.content))
  useEffect(() => { load() }, [])
  return (
    <div>
      <div className="row">
        <h2>Inbox</h2>
        <button className="ghost" onClick={() => post('/api/notifications/read-all').then(load)}>Mark all read</button>
      </div>
      {notes.map(n => (
        <div className="card" key={n.id}>
          <strong>{n.title}</strong> {!n.read && <span className="badge">new</span>}
          <p className="muted">{n.body}</p>
        </div>
      ))}
      {notes.length === 0 && <p className="muted">No notifications yet.</p>}
    </div>
  )
}
