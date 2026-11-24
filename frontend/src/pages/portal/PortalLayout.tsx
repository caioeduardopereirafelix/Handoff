import { createContext, useContext } from 'react'
import { Link, Outlet, useParams } from 'react-router-dom'
import { portal } from '../../api/api'
import type { PortalProject } from '../../api/types'
import { useLoad } from '../../lib/useLoad'
import { ThemeToggle } from '../../components/ThemeToggle'

interface PortalContextValue {
  token: string
  api: ReturnType<typeof portal>
  project: PortalProject
}

const PortalContext = createContext<PortalContextValue | null>(null)

export function usePortal(): PortalContextValue {
  const value = useContext(PortalContext)
  if (!value) throw new Error('usePortal fora do portal')
  return value
}

export function PortalLayout() {
  const { token = '' } = useParams()
  const api = portal(token)
  const project = useLoad(() => api.project(), [token])

  if (project.error) {
    const gone = project.error.status === 404
    return (
      <div className="auth-wrap">
        <div className="auth-card">
          <span className="wordmark">handoff</span>
          <h1 style={{ fontSize: 30, fontWeight: 800, letterSpacing: '-0.03em' }}>{gone ? 'Este link não abre mais' : 'Não foi possível abrir o portal'}</h1>
          <p style={{ fontSize: 17, lineHeight: 1.5, color: 'var(--ink-soft)' }}>
            {gone
              ? 'Ele pode ter expirado ou sido trocado por um novo. Peça à equipe que enviou o link um link atualizado.'
              : project.error.message}
          </p>
        </div>
      </div>
    )
  }
  if (!project.data) return <p className="container" style={{ paddingTop: 40, color: 'var(--muted)' }}>Abrindo o portal…</p>

  return (
    <PortalContext.Provider value={{ token, api, project: project.data }}>
      <header className="portal-header">
        <div className="container" style={{ paddingTop: 18, paddingBottom: 18, display: 'flex', flexWrap: 'wrap', gap: 16, alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 14 }}>
            <Link to={`/portal/${token}`} className="wordmark" aria-label="Início do portal">handoff</Link>
            <span aria-hidden="true" style={{ width: 1, height: 22, background: 'var(--line-strong)' }} />
            <span style={{ fontSize: 15, fontWeight: 600 }}>{project.data.clientName}</span>
            <span style={{ fontSize: 15, color: 'var(--muted)' }}>{project.data.projectName}</span>
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 16 }}>
            <p style={{ fontSize: 14, color: 'var(--muted)' }}>Você entrou pelo link do portal. Não compartilhe este link.</p>
            <ThemeToggle />
          </div>
        </div>
      </header>
      <Outlet />
    </PortalContext.Provider>
  )
}
