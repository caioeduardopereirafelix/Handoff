import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { app } from '../../api/api'
import type { Project, ServiceRequest } from '../../api/types'
import { PlusIcon } from '../../components/Icons'
import { byNewest, relativeDay, turnOf } from '../../lib/requests'
import { useLoad } from '../../lib/useLoad'
import { useApp } from './AppLayout'

interface BoardItem {
  request: ServiceRequest
  project: Project
}

function plural(count: number, one: string, many: string) {
  return `${count} ${count === 1 ? one : many}`
}

function why(request: ServiceRequest): string {
  switch (request.status) {
    case 'OPEN':
      return request.createdByType === 'CLIENT' ? 'Nova, aberta pelo cliente' : 'Aberta pela equipe'
    case 'IN_PROGRESS':
      return 'Em andamento'
    case 'CHANGES_REQUESTED':
      return 'O cliente pediu ajuste'
    case 'AWAITING_APPROVAL':
      return 'Entregue, esperando aprovação'
    default:
      return ''
  }
}

export function BoardPage() {
  const { setYourTurnCount } = useApp()
  const [projectFilter, setProjectFilter] = useState<string | null>(null)
  const board = useLoad(async () => {
    const projects = (await app.projects()).filter((p) => p.status === 'ACTIVE')
    const lists = await Promise.all(projects.map((p) => app.projectRequests(p.id)))
    const items: BoardItem[] = lists.flatMap((requests, i) => requests.map((request) => ({ request, project: projects[i] })))
    return { projects, items }
  }, [])

  const items = board.data?.items ?? []
  const visible = items.filter((i) => !projectFilter || i.project.id === projectFilter)
  const ours = visible.filter((i) => turnOf(i.request.status) === 'team').sort((a, b) => byNewest(a.request, b.request))
  const theirs = visible.filter((i) => turnOf(i.request.status) === 'client').sort((a, b) => byNewest(a.request, b.request))
  const allOurs = items.filter((i) => turnOf(i.request.status) === 'team').length

  useEffect(() => {
    setYourTurnCount(allOurs)
  }, [allOurs, setYourTurnCount])

  if (board.loading && !board.data) return <p style={{ color: 'var(--muted)' }}>Carregando solicitações…</p>
  if (board.error) return <p className="error-box" role="alert">{board.error.message}</p>

  const projects = board.data?.projects ?? []
  if (projects.length === 0) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 20, alignItems: 'flex-start' }}>
        <h1 style={{ fontSize: 48, lineHeight: 1, fontWeight: 800, letterSpacing: '-0.035em' }}>Comece pelo primeiro cliente</h1>
        <p style={{ fontSize: 17, color: 'var(--ink-soft)', maxWidth: '56ch' }}>Cadastre um cliente e um projeto. Depois gere o link do portal para ele abrir as solicitações.</p>
        <Link to="/app/projetos" className="btn btn-primary">Cadastrar cliente e projeto</Link>
      </div>
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 28 }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'flex-end', justifyContent: 'space-between', gap: 20 }}>
        <div>
          <h1 style={{ fontSize: 'clamp(34px, 4.4vw, 48px)', lineHeight: 1, fontWeight: 800, letterSpacing: '-0.035em' }}>
            {allOurs === 0 ? 'Nada esperando por você' : `A vez é sua em ${plural(allOurs, 'solicitação', 'solicitações')}`}
          </h1>
          <p style={{ marginTop: 12, fontSize: 17, color: 'var(--ink-soft)' }}>
            {theirs.length === 0 ? 'Nenhuma entrega aguardando clientes.' : `${plural(theirs.length, 'entrega aguarda', 'entregas aguardam')} aprovação de clientes.`}
          </p>
        </div>
        <Link to="/app/solicitacoes/nova" className="btn btn-primary"><PlusIcon />Nova solicitação</Link>
      </div>

      {projects.length > 1 && (
        <div className="chips" role="group" aria-label="Filtrar por projeto">
          <button type="button" className="chip" aria-pressed={projectFilter === null} onClick={() => setProjectFilter(null)}>Todos os projetos</button>
          {projects.map((p) => (
            <button key={p.id} type="button" className="chip" aria-pressed={projectFilter === p.id} onClick={() => setProjectFilter(p.id)}>
              {p.clientName}, {p.name}
            </button>
          ))}
        </div>
      )}

      <div className="board">
        <Column title="Sua vez" items={ours} yours empty="Nenhuma solicitação esperando a equipe." />
        <Column title="Com o cliente" items={theirs} yours={false} empty="Nenhuma entrega aguardando o cliente." />
      </div>
    </div>
  )
}

function Column({ title, items, yours, empty }: { title: string; items: BoardItem[]; yours: boolean; empty: string }) {
  return (
    <section className={`board-col${yours ? ' is-yours' : ''}`}>
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
        <h2 style={{ fontSize: 22, fontWeight: 800 }}>{title}</h2>
        <span style={{ fontSize: 15, color: 'var(--muted)' }}>{plural(items.length, 'solicitação', 'solicitações')}</span>
      </div>
      {items.length === 0 ? (
        <p style={{ marginTop: 16, color: 'var(--muted)' }}>{empty}</p>
      ) : (
        <div style={{ marginTop: 12, display: 'flex', flexDirection: 'column' }}>
          {items.map(({ request, project }) => (
            <Link key={request.id} to={`/app/solicitacoes/${request.id}`} className="board-item">
              <span className="code-sm" style={{ fontSize: 22, fontWeight: 800, letterSpacing: '-0.03em', minWidth: 56, color: yours ? undefined : 'var(--muted-code)' }}>{request.code}</span>
              <span style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: 4 }}>
                <span style={{ fontSize: 16, fontWeight: 700 }}>{request.title}</span>
                <span style={{ fontSize: 14, color: 'var(--muted)' }}>{project.clientName}, {project.name}</span>
                <span style={{ fontSize: 14, fontWeight: yours ? 600 : 400, color: yours ? 'var(--ink)' : 'var(--ink-soft)' }}>{why(request)}</span>
              </span>
              <span style={{ fontSize: 14, color: 'var(--muted)', whiteSpace: 'nowrap' }}>{relativeDay(request.updatedAt)}</span>
            </Link>
          ))}
        </div>
      )}
    </section>
  )
}
