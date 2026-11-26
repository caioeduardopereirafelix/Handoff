import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { app, thread } from '../../api/api'
import type { RequestStatus } from '../../api/types'
import { AssigneePicker } from '../../components/AssigneePicker'
import { Conversation } from '../../components/Conversation'
import { BackIcon } from '../../components/Icons'
import { Timeline } from '../../components/Timeline'
import { relativeDay, statusLabel, teamActions, timeline, turnOf } from '../../lib/requests'
import { messageOf, useLoad } from '../../lib/useLoad'
import { useApp } from './AppLayout'

export function RequestDetailPage() {
  const { id = '' } = useParams()
  const { me } = useApp()
  const scope = app.scope(id)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const data = useLoad(async () => {
    const request = await app.request(id)
    const [project, comments, attachments, activity, team] = await Promise.all([
      app.project(request.projectId),
      thread.comments(scope),
      thread.attachments(scope),
      thread.activity(scope),
      app.team(),
    ])
    return { request, project, comments, attachments, activity, members: team.members }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  if (data.loading && !data.data) return <p style={{ color: 'var(--muted)' }}>Carregando solicitação…</p>
  if (data.error) return <p className="error-box" role="alert">{data.error.message}</p>
  const { request, project, comments, attachments, activity, members } = data.data!
  const turn = turnOf(request.status)

  async function change(status: RequestStatus) {
    if (status === 'CANCELED' && !window.confirm('Cancelar esta solicitação? Ela não poderá ser reaberta.')) return
    setBusy(true)
    setError(null)
    try {
      await app.changeStatus(id, status)
      data.reload()
    } catch (reason) {
      setError(messageOf(reason))
    } finally {
      setBusy(false)
    }
  }

  const banner = {
    team: { title: 'A vez é sua', text: bannerText(request.status) },
    client: { title: 'Com o cliente', text: 'Entregue. Assim que o cliente aprovar ou pedir ajuste, ela volta para cá.' },
    done: { title: statusLabel[request.status], text: request.status === 'DONE' ? 'O cliente aprovou a entrega.' : 'Esta solicitação foi cancelada.' },
  }[turn]

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 28 }}>
      <Link to="/app" style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontWeight: 600, textDecoration: 'none', minHeight: 44 }}>
        <BackIcon />Solicitações
      </Link>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 40 }}>
        <div style={{ flex: '1 1 420px', minWidth: 0, display: 'flex', flexDirection: 'column', gap: 24 }}>
          <div>
            <p style={{ fontSize: 15, color: 'var(--muted)' }}>{project.clientName}, {project.name}</p>
            <div className="code-hero" style={{ marginTop: 6 }}>{request.code}</div>
            <h1 style={{ marginTop: 14, fontSize: 30, lineHeight: 1.15, fontWeight: 800, letterSpacing: '-0.02em' }}>{request.title}</h1>
            <p style={{ marginTop: 8, fontSize: 15, color: 'var(--muted)' }}>
              Aberta {request.createdByType === 'CLIENT' ? 'pelo cliente' : 'pela equipe'} {relativeDay(request.createdAt)}
              {request.priority === 'HIGH' ? '. Urgente.' : '.'}
            </p>
          </div>

          {request.description && (
            <p style={{ fontSize: 17, lineHeight: 1.55, whiteSpace: 'pre-wrap', maxWidth: '68ch' }}>{request.description}</p>
          )}

          <div className={`turn-banner${turn === 'client' ? ' is-team' : turn === 'done' ? ' is-done' : ''}`}>
            <p style={{ fontSize: 18, fontWeight: 800 }}>{banner.title}</p>
            <p style={{ marginTop: 6, fontSize: 15, lineHeight: 1.45 }}>{banner.text}</p>
          </div>

          {teamActions(request.status).length > 0 && (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
              {teamActions(request.status).map((action) => (
                <button
                  key={action.status}
                  type="button"
                  disabled={busy}
                  className={`btn ${action.kind === 'primary' ? 'btn-primary btn-large' : 'btn-danger'}`}
                  onClick={() => change(action.status)}
                >
                  {action.label}
                </button>
              ))}
            </div>
          )}
          {error && <p className="error-box" role="alert">{error}</p>}

          <Conversation
            scope={scope}
            viewer="USER"
            comments={comments}
            attachments={attachments}
            composerLabel="Escreva para o cliente"
            disabled={turn === 'done'}
            onChanged={data.reload}
          />
        </div>

        <aside style={{ flex: '0 1 300px', minWidth: 260 }} aria-label="Andamento">
          <AssigneePicker request={request} members={members} meId={me.id} onChanged={data.reload} />
          <h2 style={{ fontSize: 20, fontWeight: 800, marginBottom: 16 }}>Andamento</h2>
          <Timeline steps={timeline(request, activity, 'team')} />
        </aside>
      </div>
    </div>
  )
}

function bannerText(status: RequestStatus): string {
  switch (status) {
    case 'OPEN':
      return 'Ninguém começou ainda. Comece quando for trabalhar nela.'
    case 'CHANGES_REQUESTED':
      return 'O cliente pediu ajuste. Leia a conversa e retome o trabalho.'
    default:
      return 'Quando terminar, entregue para o cliente aprovar. Envie os arquivos na conversa antes.'
  }
}
