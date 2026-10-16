import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth'
import { LoginPage, OAuthCallback, RegisterPage } from './pages/AuthPages'
import { CampaignDetail, Campaigns } from './pages/Campaigns'
import { Chat } from './pages/Chat'
import { Dashboard } from './pages/Dashboard'
import { DealDetail, Deals } from './pages/Deals'
import { Discover } from './pages/Discover'
import { Inbox } from './pages/Inbox'
import { Layout } from './pages/Layout'
import { Manager } from './pages/Manager'
import { Profile } from './pages/Profile'
import type { ReactNode } from 'react'

function Guard({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <p>Loading…</p>
  if (!user) return <Navigate to="/login" replace />
  return children
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/oauth2/callback" element={<OAuthCallback />} />
          <Route element={<Guard><Layout /></Guard>}>
            <Route path="/" element={<Dashboard />} />
            <Route path="/profile" element={<Profile />} />
            <Route path="/discover" element={<Discover />} />
            <Route path="/campaigns" element={<Campaigns />} />
            <Route path="/campaigns/:id" element={<CampaignDetail />} />
            <Route path="/deals" element={<Deals />} />
            <Route path="/deals/:id" element={<DealDetail />} />
            <Route path="/chat" element={<Chat />} />
            <Route path="/notifications" element={<Inbox />} />
            <Route path="/manager" element={<Manager />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
