import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { thread } from '../../api/api'
import { Conversation } from '../../components/Conversation'
import { BackIcon, CheckIcon } from '../../components/Icons'
import { Timeline } from '../../components/Timeline'
import { relativeDay, statusLabel, timeline, turnOf } from '../../lib/requests'
import { messageOf, useLoad } from '../../lib/useLoad'
import { usePortal } from './PortalLayout'

export function PortalRequestPage() {
  const { id = '' } = useParams()
  const { token, api, project } = usePortal()
  const scope = api.scope(id)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [askingChanges, setAskingChanges] = useState(false)
  const [changes, setChanges] = useState('')

  const data = useLoad(async () => {
    const [request, comments, attachments, activity] = await Promise.all([
      api.request(id),
      thread.comments(scope),
      thread.attachments(scope),
      thread.activity(scope),
    ])
    return { request, comments, attachments, activity }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, id])

  if (data.loading && !data.data) return <p className="container" style={{ paddingTop: 40, color: 'var(--muted)' }}>Carregando…</p>
  if (data.error) {
    return (
      <main className="container" style={{ paddingTop: 40 }}>
        <p className="error-box" role="alert">{data.error.status === 404 ? 'Esta solicitação não existe neste projeto.' : data.error.message}</p>
      </main>
    )
  }
  const { request, comments, attachments, activity } = data.data!
  const turn = turnOf(request.status)
  const lastDelivery = [...activity].reverse().find((a) => a.toStatus === 'AWAITING_APPROVAL')

  async function approve() {
    setBusy(true)
    setError(null)
    try {
      await api.approve(id)
      data.reload()
    } catch (reason) {
      setError(messageOf(reason))
    } finally {
      setBusy(false)
    }
  }

  async function requestChanges(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      if (changes.trim()) await thread.addComment(scope, changes.trim())
      await api.requestChanges(id)
      setAskingChanges(false)
      setChanges('')
      data.reload()
    } catch (reason) {
      setError(messageOf(reason))
      data.reload()
    } finally {
      setBusy(false)
    }
  }

  return (
    <main style={{ maxWidth: 720, margin: '0 auto', padding: '24px 20px 80px', display: 'flex', flexDirection: 'column', gap: 28 }}>
      <Link to={`/portal/${token}`} style={{ display: 'inline-flex', alignItems: 'center', gap: 6, minHeight: 44, fontSize: 15, fontWeight: 600, textDecoration: 'none' }}>
        <BackIcon />Todas as solicitações
      </Link>

      <div>
        <p style={{ fontSize: 14, color: 'var(--muted)' }}>{project.projectName}</p>
        <div className="code-hero" style={{ marginTop: 6 }}>{request.code}</div>
        <h1 style={{ marginTop: 14, fontSize: 28, lineHeight: 1.15, fontWeight: 800, letterSpacing: '-0.02em' }}>{request.title}</h1>
        {request.description && <p style={{ marginTop: 12, fontSize: 16, lineHeight: 1.55, whiteSpace: 'pre-wrap', color: 'var(--ink-soft)' }}>{request.description}</p>}
      </div>

      {turn === 'client' && (
        <div className="turn-banner">
          <p style={{ fontSize: 18, fontWeight: 800 }}>A vez é sua</p>
          <p style={{ marginTop: 6, fontSize: 15, lineHeight: 1.45 }}>
            A equipe entregou{lastDelivery ? ` ${relativeDay(lastDelivery.createdAt)}` : ''}. Confira na conversa abaixo e aprove, ou peça um ajuste.
          </p>
        </div>
      )}
      {turn === 'team' && (
        <div className="turn-banner is-team">
          <p style={{ fontSize: 18, fontWeight: 800 }}>Com a equipe</p>
          <p style={{ marginTop: 6, fontSize: 15, lineHeight: 1.45 }}>
            {request.status === 'CHANGES_REQUESTED' ? 'Você pediu um ajuste. A equipe avisa quando entregar de novo.' : `${statusLabel[request.status]}. Você recebe um aviso quando houver entrega.`}
          </p>
        </div>
      )}
      {turn === 'done' && (
        <div className="turn-banner is-done">
          <p style={{ fontSize: 18, fontWeight: 800 }}>{request.status === 'DONE' ? 'Concluída' : 'Cancelada'}</p>
          <p style={{ marginTop: 6, fontSize: 15, lineHeight: 1.45 }}>
            {request.status === 'DONE' ? 'Você aprovou esta entrega.' : 'A equipe cancelou esta solicitação.'}
          </p>
        </div>
      )}

      <Timeline steps={timeline(request, activity, 'client')} />

      {turn === 'client' && !askingChanges && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <button type="button" className="btn btn-approve" disabled={busy} onClick={approve}><CheckIcon size={20} />Aprovar entrega</button>
          <button type="button" className="btn btn-outline btn-large" disabled={busy} onClick={() => setAskingChanges(true)}>Pedir ajuste</button>
        </div>
      )}
      {turn === 'client' && askingChanges && (
        <form onSubmit={requestChanges} className="panel" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <div className="field">
            <label htmlFor="ajuste">O que precisa mudar?</label>
            <textarea id="ajuste" className="input" rows={4} required maxLength={5000} value={changes} onChange={(e) => setChanges(e.target.value)} placeholder="Ex.: a foto ficou escura, dá para clarear?" />
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
            <button type="submit" className="btn btn-primary" disabled={busy || !changes.trim()}>Enviar pedido de ajuste</button>
            <button type="button" className="btn btn-quiet" onClick={() => setAskingChanges(false)}>Voltar</button>
          </div>
        </form>
      )}
      {error && <p className="error-box" role="alert">{error}</p>}

      <Conversation
        scope={scope}
        viewer="CLIENT"
        comments={comments}
        attachments={attachments}
        composerLabel="Escreva para a equipe"
        disabled={turn === 'done'}
        onChanged={data.reload}
      />
    </main>
  )
}
