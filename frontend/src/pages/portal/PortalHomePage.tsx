import { Link } from 'react-router-dom'
import { allPages } from '../../api/api'
import type { ServiceRequest } from '../../api/types'
import { CheckIcon, PlusIcon } from '../../components/Icons'
import { ACTIVE_STATUSES, byNewest, FINISHED_STATUSES, relativeDay, shortDate, statusLabel, turnOf } from '../../lib/requests'
import { useLoad } from '../../lib/useLoad'
import { usePagedList } from '../../lib/usePagedList'

const FINISHED_PAGE_SIZE = 5
import { usePortal } from './PortalLayout'

const words = ['Nenhuma', 'Uma', 'Duas', 'Três', 'Quatro', 'Cinco', 'Seis', 'Sete', 'Oito', 'Nove']

function headline(waiting: number, withTeam: number): string {
  if (waiting === 1) return 'Uma entrega espera a sua aprovação'
  if (waiting > 1) return `${words[waiting] ?? waiting} entregas esperam a sua aprovação`
  if (withTeam > 0) return 'Tudo com a equipe por enquanto'
  return 'Nada pendente no momento'
}

export function PortalHomePage() {
  const { token, api } = usePortal()
  const requests = useLoad(() => allPages(api.requests, { status: ACTIVE_STATUSES }), [token])
  const finished = usePagedList((page) => api.requests({ status: FINISHED_STATUSES, page, size: FINISHED_PAGE_SIZE }), [token])

  if (requests.loading && !requests.data) return <p className="container" style={{ paddingTop: 40, color: 'var(--muted)' }}>Carregando…</p>
  if (requests.error) return <p className="container error-box" style={{ marginTop: 40 }}>{requests.error.message}</p>

  const all = [...(requests.data ?? [])].sort(byNewest)
  const mine = all.filter((r) => turnOf(r.status) === 'client')
  const team = all.filter((r) => turnOf(r.status) === 'team')
  const doneCount = finished.total
  const total = mine.length + team.length + doneCount

  return (
    <main className="container" style={{ paddingTop: 56, paddingBottom: 80 }}>
      <section style={{ display: 'flex', flexWrap: 'wrap', gap: 40, alignItems: 'flex-end', justifyContent: 'space-between' }}>
        <div style={{ flex: '1 1 560px', minWidth: 0 }}>
          <h1 style={{ fontSize: 'clamp(40px, 5.6vw, 76px)', lineHeight: 0.98, fontWeight: 800, letterSpacing: '-0.035em', maxWidth: '13ch' }}>
            {headline(mine.length, team.length)}
          </h1>
          <p style={{ marginTop: 20, fontSize: 19, lineHeight: 1.5, color: 'var(--ink-soft)', maxWidth: '52ch' }}>
            {mine.length > 0
              ? 'Abra cada uma, confira o que a equipe fez e aprove. Se algo não estiver certo, peça um ajuste e diga o que mudar.'
              : 'Precisa de algo no projeto? Abra uma solicitação e acompanhe por aqui cada passo até a entrega.'}
          </p>
        </div>
        <Link to={`/portal/${token}/solicitacoes/nova`} className="btn btn-primary btn-large"><PlusIcon />Abrir solicitação</Link>
      </section>

      {total > 0 && (
        <section aria-label="Onde estão as solicitações" style={{ marginTop: 48 }}>
          <div className="split-bar">
            {mine.length > 0 && <div className="split-yours" style={{ flex: mine.length }}><strong>{mine.length}</strong><span>com você</span></div>}
            {team.length > 0 && <div className="split-team" style={{ flex: team.length }}><strong>{team.length}</strong><span>com a equipe</span></div>}
            {doneCount > 0 && <div className="split-done" style={{ flex: doneCount }}><strong>{doneCount}</strong><span>{doneCount === 1 ? 'concluída' : 'concluídas'}</span></div>}
          </div>
        </section>
      )}

      <section style={{ marginTop: 56 }}>
        <h2 className="section-title" style={{ marginBottom: 16 }}>Com você</h2>
        {mine.length === 0 ? (
          <p className="empty">Nenhuma entrega esperando você. Quando a equipe entregar algo, aparece aqui.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {mine.map((r) => <Row key={r.id} token={token} request={r} yours />)}
          </div>
        )}
      </section>

      <section style={{ marginTop: 48 }}>
        <h2 className="section-title" style={{ marginBottom: 16 }}>Com a equipe</h2>
        {team.length === 0 ? (
          <p className="empty">Nada com a equipe agora.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {team.map((r) => <Row key={r.id} token={token} request={r} yours={false} />)}
          </div>
        )}
      </section>

      {finished.items.length > 0 && (
        <section style={{ marginTop: 48 }}>
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'baseline', justifyContent: 'space-between', gap: 12, marginBottom: 16 }}>
            <h2 className="section-title">Concluídas</h2>
            {finished.hasMore && (
              <button type="button" onClick={finished.loadMore} disabled={finished.loading} style={{ background: 'none', border: 0, fontSize: 15, fontWeight: 600, textDecoration: 'underline', cursor: 'pointer', minHeight: 44 }}>
                {finished.loading ? 'Carregando…' : `Ver mais concluídas (${finished.items.length} de ${doneCount})`}
              </button>
            )}
          </div>
          <div className="panel" style={{ padding: '6px 24px' }}>
            {finished.items.map((r) => (
              <Link key={r.id} to={`/portal/${token}/solicitacoes/${r.id}`} style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 20, padding: '16px 0', borderBottom: '1px solid var(--line-soft)', textDecoration: 'none' }}>
                <span style={{ fontSize: 17, fontWeight: 700, minWidth: 72, color: 'var(--muted-code)' }}>{r.code}</span>
                <span style={{ flex: '1 1 320px', minWidth: 0, fontSize: 17 }}>{r.title}</span>
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8, fontSize: 15, fontWeight: 600, color: r.status === 'DONE' ? 'var(--pine)' : 'var(--muted)' }}>
                  {r.status === 'DONE' && <CheckIcon />}
                  {r.status === 'DONE' ? `Aprovada em ${shortDate(r.updatedAt)}` : `Cancelada em ${shortDate(r.updatedAt)}`}
                </span>
              </Link>
            ))}
          </div>
        </section>
      )}
    </main>
  )
}

function Row({ token, request, yours }: { token: string; request: ServiceRequest; yours: boolean }) {
  const meta = yours
    ? `A equipe entregou ${relativeDay(request.updatedAt)}`
    : request.status === 'CHANGES_REQUESTED'
      ? `Você pediu um ajuste ${relativeDay(request.updatedAt)}`
      : `${request.createdByType === 'CLIENT' ? 'Você pediu' : 'A equipe abriu'} ${relativeDay(request.createdAt)}`
  return (
    <Link to={`/portal/${token}/solicitacoes/${request.id}`} className={`row-link${yours ? ' is-yours' : ''}`}>
      <span className="code-xl" style={{ color: yours ? undefined : 'var(--muted-code)' }}>{request.code}</span>
      <span className="row-main">
        <span className="row-title">{request.title}</span>
        <span className="row-meta">{meta}</span>
      </span>
      {yours ? (
        <span className="pill pill-signal">Revisar entrega</span>
      ) : (
        <span className="pill pill-team"><span className="pill-dot" />{statusLabel[request.status]}</span>
      )}
    </Link>
  )
}
