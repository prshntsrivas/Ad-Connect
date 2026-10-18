import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Client } from '@stomp/stompjs'
import { get, getAccess, post } from '../api'
import { useAuth } from '../auth'

type Conversation = { id: string; otherUserId: number; otherUserName: string; lastMessagePreview?: string; unread: number }
type Message = { id: string; senderId: number; senderName: string; content: string; clientMessageId: string; createdAt: string }

export function Chat() {
  const { user } = useAuth()
  const [params] = useSearchParams()
  const withId = params.get('with')
  const [convos, setConvos] = useState<Conversation[]>([])
  const [active, setActive] = useState<string | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [text, setText] = useState('')
  const [typing, setTyping] = useState('')

  const loadConvos = () => get<Conversation[]>('/api/chat/conversations').then(setConvos)

  useEffect(() => { loadConvos() }, [])

  useEffect(() => {
    if (!withId) return
    post<Conversation>('/api/chat/conversations', { participantId: Number(withId), dealId: params.get('deal') ? Number(params.get('deal')) : null })
      .then(c => { setActive(c.id); loadConvos() })
  }, [withId])

  useEffect(() => {
    if (!active) return
    get<Message[]>(`/api/chat/conversations/${active}/messages`).then(setMessages)
    post(`/api/chat/conversations/${active}/read`).catch(() => {})
  }, [active])

  useEffect(() => {
    const token = getAccess()
    if (!token) return
    const client = new Client({
      brokerURL: `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws?access_token=${token}`,
      reconnectDelay: 4000,
      onConnect: () => {
        client.subscribe('/user/queue/messages', frame => {
          const msg: Message = JSON.parse(frame.body)
          setMessages(prev => prev.some(m => m.id === msg.id) ? prev : [...prev, msg])
        })
        client.subscribe('/user/queue/typing', frame => {
          const t = JSON.parse(frame.body)
          setTyping(`${t.name} is typing…`)
          setTimeout(() => setTyping(''), 1500)
        })
      },
    })
    client.activate()
    return () => { client.deactivate() }
  }, [])

  const current = useMemo(() => convos.find(c => c.id === active), [convos, active])

  const send = async (e: FormEvent) => {
    e.preventDefault()
    if (!active || !text.trim()) return
    const clientMessageId = crypto.randomUUID()
    const msg = await post<Message>(`/api/chat/conversations/${active}/messages`, { clientMessageId, content: text })
    setMessages(prev => prev.some(m => m.id === msg.id) ? prev : [...prev, msg])
    setText('')
  }

  return (
    <div className="row" style={{ alignItems: 'stretch' }}>
      <div className="card" style={{ width: 260 }}>
        <h3>Conversations</h3>
        {convos.map(c => (
          <div key={c.id} className="card" style={{ padding: '.5rem', cursor: 'pointer' }} onClick={() => setActive(c.id)}>
            <strong>{c.otherUserName}</strong>
            <div className="muted">{c.lastMessagePreview}</div>
          </div>
        ))}
      </div>
      <div className="card" style={{ flex: 1 }}>
        <h3 data-testid="chat-with">{current?.otherUserName || 'Chat'}</h3>
        <div className="chat-log" data-testid="chat-log">
          {messages.map(m => (
            <div key={m.id} className={`bubble ${m.senderId === user?.id ? 'mine' : ''}`}>{m.content}</div>
          ))}
        </div>
        {typing && <div className="muted">{typing}</div>}
        <form className="row" onSubmit={send}>
          <input value={text} onChange={e => setText(e.target.value)} placeholder="Message" data-testid="chat-input" disabled={!active} />
          <button disabled={!active}>Send</button>
        </form>
      </div>
    </div>
  )
}
