import { useEffect, useState, type FormEvent } from 'react'
import { get, put } from '../api'
import { useAuth } from '../auth'

export function Profile() {
  const { user } = useAuth()
  const [msg, setMsg] = useState('')
  const [brand, setBrand] = useState({ companyName: '', industry: '', website: '', city: '', state: '', description: '' })
  const [inf, setInf] = useState({
    displayName: '', bio: '', city: '', state: '', country: 'India', niches: 'fashion',
    instagramHandle: '', instagramFollowers: 10000, youtubeChannel: '', youtubeSubscribers: 0,
    twitterHandle: '', twitterFollowers: 0, engagementRate: 3.5, pricePerPost: 5000,
  })

  useEffect(() => {
    get<{ brand?: typeof brand; influencer?: Record<string, unknown> }>('/api/profiles/me').then(p => {
      if (p.brand) setBrand({ ...brand, ...p.brand })
      if (p.influencer) {
        const i = p.influencer as typeof inf & { niches?: string[] }
        setInf({ ...inf, ...i, niches: (i.niches || []).join(',') })
      }
    }).catch(() => {})
  }, [])

  const saveBrand = async (e: FormEvent) => {
    e.preventDefault()
    await put('/api/profiles/brand', brand)
    setMsg('Brand profile saved')
  }
  const saveInf = async (e: FormEvent) => {
    e.preventDefault()
    await put('/api/profiles/influencer', {
      ...inf,
      niches: inf.niches.split(',').map(s => s.trim()).filter(Boolean),
      instagramFollowers: Number(inf.instagramFollowers),
      youtubeSubscribers: Number(inf.youtubeSubscribers),
      twitterFollowers: Number(inf.twitterFollowers),
      engagementRate: Number(inf.engagementRate),
      pricePerPost: Number(inf.pricePerPost),
    })
    setMsg('Influencer profile saved')
  }

  return (
    <div>
      <h2>Profile</h2>
      {msg && <p className="badge ok">{msg}</p>}
      {user?.role === 'BRAND' && (
        <form className="card" onSubmit={saveBrand} data-testid="brand-profile">
          <label>Company <input value={brand.companyName} onChange={e => setBrand({ ...brand, companyName: e.target.value })} required /></label>
          <label>Industry <input value={brand.industry} onChange={e => setBrand({ ...brand, industry: e.target.value })} /></label>
          <label>City <input value={brand.city} onChange={e => setBrand({ ...brand, city: e.target.value })} /></label>
          <label>Website <input value={brand.website} onChange={e => setBrand({ ...brand, website: e.target.value })} /></label>
          <label>About <textarea value={brand.description} onChange={e => setBrand({ ...brand, description: e.target.value })} /></label>
          <button>Save brand profile</button>
        </form>
      )}
      {user?.role === 'INFLUENCER' && (
        <form className="card" onSubmit={saveInf} data-testid="influencer-profile">
          <label>Display name <input value={inf.displayName} onChange={e => setInf({ ...inf, displayName: e.target.value })} required /></label>
          <label>City <input value={inf.city} onChange={e => setInf({ ...inf, city: e.target.value })} required /></label>
          <label>Niches (comma separated) <input value={inf.niches} onChange={e => setInf({ ...inf, niches: e.target.value })} required /></label>
          <label>Instagram followers <input type="number" value={inf.instagramFollowers} onChange={e => setInf({ ...inf, instagramFollowers: Number(e.target.value) })} /></label>
          <label>Engagement % <input type="number" step="0.1" value={inf.engagementRate} onChange={e => setInf({ ...inf, engagementRate: Number(e.target.value) })} /></label>
          <label>Price per post (₹) <input type="number" value={inf.pricePerPost} onChange={e => setInf({ ...inf, pricePerPost: Number(e.target.value) })} /></label>
          <label>Bio <textarea value={inf.bio} onChange={e => setInf({ ...inf, bio: e.target.value })} /></label>
          <button>Save influencer profile</button>
        </form>
      )}
      {user?.role !== 'BRAND' && user?.role !== 'INFLUENCER' && <p className="muted">Staff accounts do not have a public marketplace profile.</p>}
    </div>
  )
}
