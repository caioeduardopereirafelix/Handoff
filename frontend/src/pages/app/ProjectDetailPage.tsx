import { Link, useParams } from 'react-router-dom'
import { app } from '../../api/api'
import type { ServiceRequest } from '../../api/types'
import { BackIcon, PlusIcon } from '../../components/Icons'
import { PortalLinks } from '../../components/PortalLinks'
import { linkStatus, linkToneColor, loadActiveRequests, summarize, turnSummary } from '../../lib/projects'
import { byNewest, FINISHED_STATUSES, relativeDay, shortDate, statusLabel, turnOf } from '../../lib/requests'
import { useLoad } from '../../lib/useLoad'
import { usePagedList } from '../../lib/usePagedList'

const FINISHED_PAGE_SIZE = 20

export function ProjectDetailPage() {
  const { id = '' } = useParams()
  const data = useLoad(async () => {
    const [project, active, links] = await Promise.all([
      app.project(id),
      loadActiveRequests(id),
      app.portalLinks(id),
    ])
    const client = await app.client(project.clientId).catch(() => null)
    return { project, active, links, client }
  }, [id])
  const finished = usePagedList(
    (page) => app.projectRequests(id, { status: FINISHED_STATUSES, page, size: FINISHED_PAGE_SIZE }),
    [id],
  )

  if (data.loading && !data.data) return <p style={{ color: 'var(--muted)' }}>Carregando projeto…</p>
  if (data.error) return <p className="error-box" role="alert">{data.error.message}</p>
  const { project, active, links, client } = data.data!
  const summary = summarize(project, active, finished.total, links)
  const sorted = [...active].sort(byNewest)
  const mine = sorted.filter((r) => turnOf(r.status) === 'team')
  const theirs = sorted.filter((r) => turnOf(r.status) === 'client')
  const status = linkStatus(summary.links)

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 32 }}>
      <Link to="/app/projetos" style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontWeight: 600, textDecoration: 'none', minHeight: 44 }}>
        <BackIcon />Projetos
      </Link>

      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'flex-end', justifyContent: 'space-between', gap: 20 }}>
        <div>
          <p style={{ fontSize: 16, color: 'var(--muted)' }}>{project.clientName}</p>
          <h1 style={{ marginTop: 6, fontSize: 'clamp(34px, 4.4vw, 48px)', lineHeight: 1, fontWeight: 800, letterSpacing: '-0.035em' }}>{project.name}</h1>
          <p style={{ marginTop: 12, fontSize: 17, color: 'var(--ink-soft)' }}>
            {active.length + finished.total === 0 ? 'Nenhuma solicitação ainda.' : turnSummary(summary)}
          </p>
        </div>
        <Link to={`/app/solicitacoes/nova?projeto=${project.id}`} className="btn btn-primary"><PlusIcon />Nova solicitação</Link>
      </div>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 32, alignItems: 'flex-start' }}>
        <div style={{ flex: '1 1 480px', minWidth: 0, display: 'flex', flexDirection: 'column', gap: 32 }}>
          <RequestGroup title="Sua vez" requests={mine} yours empty="Nada esperando a equipe neste projeto." />
          <RequestGroup title="Com o cliente" requests={theirs} yours={false} empty="Nenhuma entrega aguardando o cliente." />
          {finished.items.length > 0 && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12, alignItems: 'flex-start' }}>
              <RequestGroup title={`Concluídas (${finished.total})`} requests={finished.items} yours={false} empty="" />
              {finished.hasMore && (
                <button type="button" className="btn btn-quiet" onClick={finished.loadMore} disabled={finished.loading}>
                  {finished.loading ? 'Carregando…' : `Carregar mais ${Math.min(FINISHED_PAGE_SIZE, finished.total - finished.items.length)}`}
                </button>
              )}
            </div>
          )}
          {finished.error && <p className="error-box" role="alert">{finished.error.message}</p>}
        </div>

        <aside style={{ flex: '1 1 320px', minWidth: 0, display: 'flex', flexDirection: 'column', gap: 20 }}>
          <section className="panel" aria-labelledby="cliente-titulo" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <h2 id="cliente-titulo" style={{ fontSize: 20, fontWeight: 800 }}>Cliente</h2>
            <p style={{ fontSize: 17, fontWeight: 700 }}>{project.clientName}</p>
            <p style={{ fontSize: 15, color: 'var(--ink-soft)' }}>{client?.contactName ?? 'Sem pessoa de contato'}</p>
            <p style={{ fontSize: 15, color: client?.email ? 'var(--ink-soft)' : 'var(--danger)' }}>{client?.email ?? 'Sem e-mail: o cliente não recebe avisos'}</p>
          </section>

          <section className="panel" aria-labelledby="links-titulo" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <h2 id="links-titulo" style={{ fontSize: 20, fontWeight: 800 }}>Link do portal</h2>
            <p style={{ fontSize: 15, color: linkToneColor[status.tone], fontWeight: 600 }}>{status.text}</p>
            <PortalLinks projectId={project.id} clientHasEmail={Boolean(client?.email)} onChanged={data.reload} />
          </section>
        </aside>
      </div>
    </div>
  )
}

function RequestGroup({ title, requests, yours, empty }: { title: string; requests: ServiceRequest[]; yours: boolean; empty: string }) {
  return (
    <section style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <h2 className="section-title">{title}</h2>
      {requests.length === 0 ? (
        <p className="empty">{empty}</p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {requests.map((r) => {
            const finished = turnOf(r.status) === 'done'
            return (
              <Link key={r.id} to={`/app/solicitacoes/${r.id}`} className={`row-link${yours ? ' is-yours' : ''}`}>
                <span className="code-xl" style={{ color: yours ? undefined : 'var(--muted-code)' }}>{r.code}</span>
                <span className="row-main">
                  <span className="row-title">{r.title}</span>
                  <span className="row-meta">{finished ? `${statusLabel[r.status]} em ${shortDate(r.updatedAt)}` : `Atualizada ${relativeDay(r.updatedAt)}`}</span>
                </span>
                {!finished && (
                  <span className={`pill ${yours ? 'pill-signal' : 'pill-team'}`}>
                    {!yours && <span className="pill-dot" />}{statusLabel[r.status]}
                  </span>
                )}
              </Link>
            )
          })}
        </div>
      )}
    </section>
  )
}
