import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { get } from '../api'
import { useAuth } from '../auth'

export function Layout() {
  const { user, logout } = useAuth()
  const nav = useNavigate()
  const [unread, setUnread] = useState(0)
  useEffect(() => {
    if (!user) return
    get<{ count: number }>('/api/notifications/unread-count').then(r => setUnread(r.count)).catch(() => {})
  }, [user])
  if (!user) return null
  const staff = user.role === 'MANAGER' || user.role === 'ADMIN'
  return (
    <div className="layout">
      <aside className="nav">
        <h1>Ad-Connect</h1>
        <NavLink to="/" end>Dashboard</NavLink>
        <NavLink to="/profile">Profile</NavLink>
        <NavLink to="/discover">Discover</NavLink>
        <NavLink to="/campaigns">Campaigns</NavLink>
        <NavLink to="/deals">Deals</NavLink>
        <NavLink to="/chat">Chat</NavLink>
        <NavLink to="/notifications">Inbox {unread > 0 && <span className="badge">{unread}</span>}</NavLink>
        {staff && <NavLink to="/manager">Manager</NavLink>}
        <div className="spacer" />
        <small>{user.fullName}<br />{user.role} · {user.status}</small>
        <button className="ghost" style={{ color: 'white', borderColor: '#444' }} onClick={() => { logout(); nav('/login') }}>Sign out</button>
      </aside>
      <main className="main"><Outlet /></main>
    </div>
  )
}
