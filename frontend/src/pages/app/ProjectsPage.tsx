import { Link } from 'react-router-dom'
import { PlusIcon } from '../../components/Icons'
import { linkStatus, linkToneColor, loadProjectSummaries, turnSummary } from '../../lib/projects'
import type { ProjectSummary } from '../../lib/projects'
import { useLoad } from '../../lib/useLoad'

export function ProjectsPage() {
  const groups = useLoad(() => loadProjectSummaries(), [])

  if (groups.loading && !groups.data) return <p style={{ color: 'var(--muted)' }}>Carregando projetos…</p>
  if (groups.error) return <p className="error-box" role="alert">{groups.error.message}</p>

  const data = groups.data ?? []
  const projectCount = data.reduce((total, g) => total + g.projects.length, 0)

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 36 }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'flex-end', justifyContent: 'space-between', gap: 20 }}>
        <div>
          <h1 style={{ fontSize: 'clamp(34px, 4.4vw, 48px)', lineHeight: 1, fontWeight: 800, letterSpacing: '-0.035em' }}>Projetos</h1>
          <p style={{ marginTop: 12, fontSize: 17, color: 'var(--ink-soft)' }}>
            {projectCount === 0 ? 'Nenhum projeto ainda.' : 'Seus clientes e o que cada um tem em andamento.'}
          </p>
        </div>
        <Link to="/app/projetos/novo" className="btn btn-primary"><PlusIcon />Novo projeto</Link>
      </div>

      {data.length === 0 && (
        <div className="empty" style={{ display: 'flex', flexDirection: 'column', gap: 16, alignItems: 'flex-start' }}>
          <p style={{ fontSize: 17, color: 'var(--ink)' }}>Crie o primeiro projeto. Você cadastra o cliente no mesmo passo e já pode mandar o link do portal para ele.</p>
          <Link to="/app/projetos/novo" className="btn btn-primary"><PlusIcon />Criar o primeiro projeto</Link>
        </div>
      )}

      {data.map(({ client, projects }) => (
        <section key={client.id} className="client-group" aria-labelledby={`cliente-${client.id}`}>
          <div className="client-group-head">
            <h2 id={`cliente-${client.id}`} className="section-title">{client.name}</h2>
            <span style={{ fontSize: 15, color: 'var(--muted)' }}>
              {[client.contactName, client.email].filter(Boolean).join(', ') || 'Sem contato cadastrado'}
            </span>
          </div>
          <div className="project-grid">
            {projects.map((summary) => <ProjectCard key={summary.project.id} summary={summary} />)}
            <Link to={`/app/projetos/novo?cliente=${client.id}`} className="project-card-new">
              <PlusIcon />Projeto para {client.name}
            </Link>
          </div>
        </section>
      ))}
    </div>
  )
}

function ProjectCard({ summary }: { summary: ProjectSummary }) {
  const { project, mine, theirs, done } = summary
  const status = linkStatus(summary.links)
  const empty = mine + theirs + done === 0
  return (
    <Link to={`/app/projetos/${project.id}`} className={`project-card${mine > 0 ? ' is-yours' : ''}`}>
      <span style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'baseline', gap: 10 }}>
        <span style={{ fontSize: 20, fontWeight: 700 }}>{project.name}</span>
        {project.status === 'ARCHIVED' && <span style={{ fontSize: 14, color: 'var(--muted)' }}>arquivado</span>}
      </span>
      <span className="turn-strip" aria-hidden="true">
        {mine > 0 && <span style={{ flex: mine, background: 'var(--warning)' }} />}
        {theirs > 0 && <span style={{ flex: theirs, background: 'var(--info)' }} />}
        {done > 0 && <span style={{ flex: done, background: 'var(--success)' }} />}
      </span>
      <span style={{ fontSize: 15, fontWeight: 600 }}>
        {empty ? 'Nenhuma solicitação ainda' : turnSummary(summary)}
        {done > 0 && <span style={{ fontWeight: 400, color: 'var(--muted)' }}>{`, ${done} ${done === 1 ? 'concluída' : 'concluídas'}`}</span>}
      </span>
      <span style={{ fontSize: 14, color: linkToneColor[status.tone] }}>{status.text}</span>
    </Link>
  )
}
