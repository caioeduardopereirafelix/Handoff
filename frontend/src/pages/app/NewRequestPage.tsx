import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { app } from '../../api/api'
import { BackIcon } from '../../components/Icons'
import { RequestForm } from '../../components/RequestForm'
import { useLoad } from '../../lib/useLoad'

export function NewRequestPage() {
  const navigate = useNavigate()
  const projects = useLoad(() => app.projects(), [])
  const [projectId, setProjectId] = useState('')
  const active = (projects.data ?? []).filter((p) => p.status === 'ACTIVE')

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <Link to="/app" style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontWeight: 600, textDecoration: 'none', minHeight: 44 }}>
        <BackIcon />Solicitações
      </Link>
      <h1 style={{ fontSize: 'clamp(34px, 4.4vw, 48px)', lineHeight: 1, fontWeight: 800, letterSpacing: '-0.035em' }}>Nova solicitação</h1>
      {projects.error && <p className="error-box" role="alert">{projects.error.message}</p>}
      {projects.data && active.length === 0 && (
        <p className="empty">Nenhum projeto ativo. <Link to="/app/projetos">Crie um projeto</Link> antes de abrir solicitações.</p>
      )}
      {active.length > 0 && (
        <RequestForm
          submitLabel="Abrir solicitação"
          onSubmit={async (data) => {
            const created = await app.createRequest(projectId, data)
            navigate(`/app/solicitacoes/${created.id}`)
          }}
        >
          <div className="field">
            <label htmlFor="projeto">Projeto</label>
            <select id="projeto" className="input" required value={projectId} onChange={(e) => setProjectId(e.target.value)}>
              <option value="" disabled>Escolha o projeto</option>
              {active.map((p) => <option key={p.id} value={p.id}>{p.clientName}, {p.name}</option>)}
            </select>
          </div>
        </RequestForm>
      )}
    </div>
  )
}
