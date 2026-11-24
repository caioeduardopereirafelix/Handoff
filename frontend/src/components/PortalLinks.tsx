import { useState } from 'react'
import { app } from '../api/api'
import type { PortalAccessCreated } from '../api/types'
import { shortDate } from '../lib/requests'
import { messageOf, useLoad } from '../lib/useLoad'
import { CopyIcon } from './Icons'

export function portalUrl(token: string): string {
  return `${window.location.origin}/portal/${token}`
}

export function CreatedLink({ created }: { created: PortalAccessCreated }) {
  const [copied, setCopied] = useState(false)
  const url = portalUrl(created.token)

  async function copy() {
    try {
      await navigator.clipboard.writeText(url)
      setCopied(true)
    } catch {
      setCopied(false)
    }
  }

  return (
    <div className="notice-box" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      <p>
        Link criado. Ele aparece só agora: copie e guarde, ou envie ao cliente.
        {created.emailSent ? ' Também foi enviado por e-mail ao cliente.' : ''}
      </p>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'center' }}>
        <code style={{ flex: '1 1 320px', minWidth: 0, overflowWrap: 'anywhere', background: '#fff', color: 'var(--ink)', padding: '10px 12px', borderRadius: 10, fontSize: 14 }}>{url}</code>
        <button type="button" className="btn btn-primary" onClick={copy}><CopyIcon />{copied ? 'Copiado' : 'Copiar link'}</button>
      </div>
    </div>
  )
}

export function PortalLinks({ projectId, clientHasEmail, onChanged }: { projectId: string; clientHasEmail: boolean; onChanged?: () => void }) {
  const links = useLoad(() => app.portalLinks(projectId), [projectId])
  const [days, setDays] = useState(90)
  const [sendToClient, setSendToClient] = useState(clientHasEmail)
  const [created, setCreated] = useState<PortalAccessCreated | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function create(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      setCreated(await app.createPortalLink(projectId, { expiresInDays: days, sendToClient: clientHasEmail && sendToClient }))
      links.reload()
      onChanged?.()
    } catch (reason) {
      setError(messageOf(reason))
    } finally {
      setBusy(false)
    }
  }

  async function revoke(accessId: string) {
    if (!window.confirm('Revogar este link? O cliente perde o acesso na hora.')) return
    setError(null)
    try {
      await app.revokePortalLink(projectId, accessId)
      links.reload()
      onChanged?.()
    } catch (reason) {
      setError(messageOf(reason))
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {created && <CreatedLink created={created} />}

      <form onSubmit={create} style={{ display: 'flex', flexWrap: 'wrap', gap: 16, alignItems: 'flex-end' }}>
        <div className="field" style={{ flex: '0 1 180px' }}>
          <label htmlFor={`dias-${projectId}`}>Validade em dias</label>
          <input id={`dias-${projectId}`} className="input" type="number" min={1} max={365} value={days} onChange={(e) => setDays(Number(e.target.value))} />
        </div>
        {clientHasEmail ? (
          <label style={{ display: 'flex', alignItems: 'center', gap: 10, minHeight: 48, fontSize: 15, fontWeight: 600 }}>
            <input type="checkbox" checked={sendToClient} onChange={(e) => setSendToClient(e.target.checked)} style={{ width: 20, height: 20 }} />
            Enviar o link por e-mail ao cliente
          </label>
        ) : (
          <p style={{ minHeight: 48, display: 'flex', alignItems: 'center', fontSize: 15, color: 'var(--muted)' }}>O cliente não tem e-mail: copie o link e envie você mesmo.</p>
        )}
        <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Gerando…' : 'Gerar link'}</button>
      </form>
      {error && <p className="error-box" role="alert">{error}</p>}

      {links.data && links.data.length > 0 && (
        <ul className="list-plain" style={{ display: 'flex', flexDirection: 'column' }}>
          {links.data.map((link) => (
            <li key={link.id} style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 16, padding: '12px 0', borderBottom: '1px solid var(--line-soft)' }}>
              <span className={`pill ${link.active ? 'pill-done' : 'pill-team'}`}>{link.active ? 'Ativo' : link.revokedAt ? 'Revogado' : 'Expirado'}</span>
              <span style={{ flex: '1 1 260px', fontSize: 15, color: 'var(--ink-soft)' }}>
                Criado em {shortDate(link.createdAt)}, vale até {shortDate(link.expiresAt)}.{' '}
                {link.lastUsedAt ? `Último acesso em ${shortDate(link.lastUsedAt)}.` : 'Ainda não foi usado.'}
              </span>
              {link.active && <button type="button" className="btn btn-danger" onClick={() => revoke(link.id)}>Revogar</button>}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
