import { Link, useNavigate } from 'react-router-dom'
import { BackIcon } from '../../components/Icons'
import { RequestForm } from '../../components/RequestForm'
import { usePortal } from './PortalLayout'

export function PortalNewRequestPage() {
  const navigate = useNavigate()
  const { token, api, project } = usePortal()

  return (
    <main className="container" style={{ paddingTop: 32, paddingBottom: 80, display: 'flex', flexDirection: 'column', gap: 24 }}>
      <Link to={`/portal/${token}`} style={{ display: 'inline-flex', alignItems: 'center', gap: 6, minHeight: 44, fontSize: 15, fontWeight: 600, textDecoration: 'none' }}>
        <BackIcon />Todas as solicitações
      </Link>
      <div>
        <h1 style={{ fontSize: 'clamp(34px, 4.4vw, 52px)', lineHeight: 1, fontWeight: 800, letterSpacing: '-0.035em' }}>Abrir solicitação</h1>
        <p style={{ marginTop: 12, fontSize: 17, color: 'var(--ink-soft)', maxWidth: '56ch' }}>
          Conte o que precisa no {project.projectName}. A equipe recebe um aviso e você acompanha cada passo por aqui.
        </p>
      </div>
      <RequestForm
        submitLabel="Abrir solicitação"
        onSubmit={async (data) => {
          const created = await api.createRequest(data)
          navigate(`/portal/${token}/solicitacoes/${created.id}`)
        }}
      />
    </main>
  )
}
