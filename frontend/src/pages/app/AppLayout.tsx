import { createContext, useContext, useEffect, useState } from 'react'
import { NavLink, Navigate, Outlet, useNavigate } from 'react-router-dom'
import { app, auth } from '../../api/api'
import { session } from '../../api/http'
import type { Me } from '../../api/types'
import { useLoad } from '../../lib/useLoad'

interface AppContextValue {
  me: Me
  yourTurnCount: number
  setYourTurnCount: (count: number) => void
}

const AppContext = createContext<AppContextValue | null>(null)

export function useApp(): AppContextValue {
  const value = useContext(AppContext)
  if (!value) throw new Error('useApp fora do painel')
  return value
}

export function AppLayout() {
  const navigate = useNavigate()
  const [signedIn, setSignedIn] = useState(session.hasSession())
  const [yourTurnCount, setYourTurnCount] = useState(0)
  const me = useLoad(() => app.me(), [])

  useEffect(() => session.subscribe(() => setSignedIn(session.hasSession())), [])

  if (!signedIn || me.error?.status === 401) return <Navigate to="/entrar" replace />
  if (me.error) return <p className="container" style={{ paddingTop: 40 }}>{me.error.message}</p>
  if (!me.data) return <p className="container" style={{ paddingTop: 40, color: 'var(--muted)' }}>Carregando…</p>

  const initials = me.data.name.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]?.toUpperCase()).join('')

  async function logout() {
    await auth.logout()
    navigate('/entrar')
  }

  return (
    <AppContext.Provider value={{ me: me.data, yourTurnCount, setYourTurnCount }}>
      <div className="app-shell">
        <nav className="app-nav" aria-label="Menu principal">
          <div style={{ padding: '0 12px' }}>
            <span className="wordmark" style={{ color: '#fff' }}>handoff</span>
            <p style={{ marginTop: 6, fontSize: 14, color: 'var(--nav-muted)' }}>{me.data.organizationName}</p>
          </div>
          <div className="nav-links" style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <NavLink to="/app" end className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}>
              Solicitações {yourTurnCount > 0 && <span className="nav-count">{yourTurnCount}</span>}
            </NavLink>
            <NavLink to="/app/projetos" className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}>Projetos</NavLink>
          </div>
          <div className="nav-user" style={{ marginTop: 'auto', padding: '0 12px', display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
            <span style={{ width: 36, height: 36, borderRadius: '50%', background: 'var(--signal)', color: 'var(--ink)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 14, fontWeight: 800 }}>{initials}</span>
            <span style={{ fontSize: 14, color: 'var(--nav-text)' }}>{me.data.name}</span>
            <button type="button" onClick={logout} style={{ background: 'none', border: 0, color: 'var(--nav-muted)', fontSize: 14, textDecoration: 'underline', cursor: 'pointer', minHeight: 44 }}>Sair</button>
          </div>
        </nav>
        <main className="app-main">
          <div style={{ maxWidth: 1080 }}>
            <Outlet />
          </div>
        </main>
      </div>
    </AppContext.Provider>
  )
}
