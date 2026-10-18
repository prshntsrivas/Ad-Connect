import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { get } from '../api'

type Inf = {
  userId: number
  displayName: string
  city: string
  niches: string[]
  platforms: string[]
  totalFollowers: number
  engagementRate: number
  pricePerPost: number
  rating: number
  verified: boolean
}

type Result = { content: Inf[]; totalElements: number; facets: Record<string, { value: string; count: number }[]> }

export function Discover() {
  const [q, setQ] = useState('')
  const [niche, setNiche] = useState('')
  const [city, setCity] = useState('')
  const [sort, setSort] = useState('relevance')
  const [data, setData] = useState<Result | null>(null)

  const search = (e?: FormEvent) => {
    e?.preventDefault()
    const p = new URLSearchParams({ sort, size: '12' })
    if (q) p.set('q', q)
    if (niche) p.set('niche', niche)
    if (city) p.set('city', city)
    get<Result>(`/api/search/influencers?${p}`).then(setData).catch(() => setData({ content: [], totalElements: 0, facets: {} }))
  }
  useEffect(() => { search() }, [])

  return (
    <div>
      <h2>Discover influencers</h2>
      <form className="filters" onSubmit={search} data-testid="search-form">
        <input placeholder="Search (fuzzy)" value={q} onChange={e => setQ(e.target.value)} />
        <input placeholder="Niche" value={niche} onChange={e => setNiche(e.target.value)} />
        <input placeholder="City" value={city} onChange={e => setCity(e.target.value)} />
        <select value={sort} onChange={e => setSort(e.target.value)}>
          <option value="relevance">Relevance</option>
          <option value="followers">Followers</option>
          <option value="rating">Rating</option>
          <option value="price">Price</option>
        </select>
        <button>Search</button>
      </form>
      {data && (
        <>
          <p className="muted">{data.totalElements} matches
            {data.facets?.niche && <> · niches: {data.facets.niche.map(b => `${b.value} (${b.count})`).join(', ')}</>}
          </p>
          <div className="grid">
            {data.content.map(i => (
              <div className="card" key={i.userId} data-testid="influencer-card">
                <strong>{i.displayName}</strong> {i.verified && <span className="badge ok">verified</span>}
                <div className="muted">{i.city} · {(i.niches || []).join(', ')}</div>
                <div>{i.totalFollowers.toLocaleString()} followers · {i.engagementRate}% ER</div>
                <div>₹{i.pricePerPost} / post · ★ {i.rating?.toFixed?.(1) ?? i.rating}</div>
                <Link to={`/chat?with=${i.userId}`}>Message</Link>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  )
}
