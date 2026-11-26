import { useState } from 'react'
import { app } from '../../api/api'
import type { InvitationCreated, PendingInvitation, Role, TeamMember } from '../../api/types'
import { OneTimeLink } from '../../components/OneTimeLink'
import { shortDate } from '../../lib/requests'
import { messageOf, useLoad } from '../../lib/useLoad'
import { useApp } from './AppLayout'

const roleLabel: Record<Role, string> = {
  OWNER: 'Dono',
  MEMBER: 'Membro',
}

function invitationUrl(token: string): string {
  return `${window.location.origin}/convite/${token}`
}

export function TeamPage() {
  const { me } = useApp()
  const team = useLoad(() => app.team(), [])
  const isOwner = me.role === 'OWNER'
  const [error, setError] = useState<string | null>(null)

  async function run(action: () => Promise<unknown>) {
    setError(null)
    try {
      await action()
      team.reload()
    } catch (reason) {
      setError(messageOf(reason))
    }
  }

  if (team.loading && !team.data) return <p style={{ color: 'var(--muted)' }}>Carregando equipe…</p>
  if (team.error) return <p className="error-box" role="alert">{team.error.message}</p>
  const { members, invitations } = team.data!
  const active = members.filter((m) => m.active)
  const inactive = members.filter((m) => !m.active)

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 32, maxWidth: 820 }}>
      <div>
        <h1 style={{ fontSize: 'clamp(34px, 4.4vw, 48px)', lineHeight: 1, fontWeight: 800, letterSpacing: '-0.035em' }}>Equipe</h1>
        <p style={{ marginTop: 12, fontSize: 17, color: 'var(--ink-soft)' }}>
          {isOwner
            ? 'Convide quem trabalha com você. Cada pessoa entra com o próprio login e vê todos os projetos da empresa.'
            : 'Quem trabalha com você nesta empresa. Só os donos convidam e mudam acessos.'}
        </p>
      </div>

      {isOwner && <InviteForm onInvited={team.reload} />}
      {error && <p className="error-box" role="alert">{error}</p>}

      <section aria-labelledby="membros-titulo" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <h2 id="membros-titulo" className="section-title">Pessoas ({active.length})</h2>
        <ul className="list-plain panel" style={{ padding: '4px 24px' }}>
          {active.map((member) => (
            <MemberRow key={member.id} member={member} isMe={member.id === me.id} canManage={isOwner}
                       onChange={(data) => run(() => app.updateMember(member.id, data))} />
          ))}
        </ul>
      </section>

      {invitations.length > 0 && (
        <section aria-labelledby="convites-titulo" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <h2 id="convites-titulo" className="section-title">Convites pendentes ({invitations.length})</h2>
          <ul className="list-plain panel" style={{ padding: '4px 24px' }}>
            {invitations.map((invitation) => (
              <InvitationRow key={invitation.id} invitation={invitation} canManage={isOwner}
                             onRevoke={() => run(() => app.revokeInvitation(invitation.id))} />
            ))}
          </ul>
        </section>
      )}

      {inactive.length > 0 && (
        <section aria-labelledby="desativados-titulo" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <h2 id="desativados-titulo" className="section-title">Desativados ({inactive.length})</h2>
          <ul className="list-plain panel" style={{ padding: '4px 24px' }}>
            {inactive.map((member) => (
              <MemberRow key={member.id} member={member} isMe={false} canManage={isOwner}
                         onChange={(data) => run(() => app.updateMember(member.id, data))} />
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}

function InviteForm({ onInvited }: { onInvited: () => void }) {
  const { me } = useApp()
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<Role>('MEMBER')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [created, setCreated] = useState<InvitationCreated | null>(null)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      setCreated(await app.invite({ email: email.trim(), role }))
      setEmail('')
      setRole('MEMBER')
      onInvited()
    } catch (reason) {
      setError(messageOf(reason))
    } finally {
      setBusy(false)
    }
  }

  const url = created ? invitationUrl(created.token) : ''

  return (
    <section className="panel" aria-labelledby="convidar-titulo" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <h2 id="convidar-titulo" style={{ fontSize: 20, fontWeight: 800 }}>Convidar alguém</h2>
      {created && (
        <OneTimeLink
          url={url}
          intro={`Convite criado para ${created.email}, válido até ${shortDate(created.expiresAt)}.${created.emailSent ? ' Também foi enviado por e-mail.' : ''}`}
          warning="Este link é único e não poderá ser visto de novo. Copie ou compartilhe agora. Se ele se perder, convide o mesmo e-mail outra vez."
          subject={`Convite para a equipe ${me.organizationName} no Handoff`}
          message={`${me.name} convidou você para a equipe ${me.organizationName} no Handoff. Crie a sua senha por este link: ${url}`}
        />
      )}
      <form onSubmit={submit} style={{ display: 'flex', flexWrap: 'wrap', gap: 12, alignItems: 'flex-end' }}>
        <div className="field" style={{ flex: '1 1 260px' }}>
          <label htmlFor="convite-email">E-mail</label>
          <input id="convite-email" className="input" type="email" required maxLength={254} value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div className="field" style={{ flex: '0 1 160px' }}>
          <label htmlFor="convite-papel">Papel</label>
          <select id="convite-papel" className="input" value={role} onChange={(e) => setRole(e.target.value as Role)}>
            <option value="MEMBER">Membro</option>
            <option value="OWNER">Dono</option>
          </select>
        </div>
        <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Convidando…' : 'Convidar'}</button>
      </form>
      <p style={{ fontSize: 14, color: 'var(--muted)' }}>
        Membros veem e trabalham em todos os projetos. Donos também convidam pessoas, mudam acessos e apagam projetos.
      </p>
      {error && <p className="error-box" role="alert">{error}</p>}
    </section>
  )
}

function MemberRow({ member, isMe, canManage, onChange }: {
  member: TeamMember
  isMe: boolean
  canManage: boolean
  onChange: (data: { role?: Role; active?: boolean }) => void
}) {
  function deactivate() {
    if (!window.confirm(`Desativar ${member.name}? A pessoa perde o acesso na hora. Nada do que ela fez é apagado.`)) return
    onChange({ active: false })
  }

  return (
    <li style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 12, padding: '14px 0', borderBottom: '1px solid var(--line-soft)' }}>
      <span style={{ flex: '1 1 240px', minWidth: 0, display: 'flex', flexDirection: 'column', gap: 2 }}>
        <span style={{ fontSize: 16, fontWeight: 700 }}>{member.name}{isMe && <span style={{ fontWeight: 400, color: 'var(--muted)' }}> (você)</span>}</span>
        <span style={{ fontSize: 14, color: 'var(--ink-soft)', overflowWrap: 'anywhere' }}>{member.email}</span>
      </span>
      {canManage && !isMe && member.active ? (
        <>
          <select className="input" aria-label={`Papel de ${member.name}`} value={member.role}
                  onChange={(e) => onChange({ role: e.target.value as Role })} style={{ width: 'auto', minWidth: 120 }}>
            <option value="MEMBER">Membro</option>
            <option value="OWNER">Dono</option>
          </select>
          <button type="button" className="btn btn-danger" onClick={deactivate}>Desativar</button>
        </>
      ) : (
        <span className={`pill ${member.role === 'OWNER' ? 'pill-done' : 'pill-team'}`}>{roleLabel[member.role]}</span>
      )}
      {canManage && !member.active && (
        <button type="button" className="btn btn-quiet" onClick={() => onChange({ active: true })}>Reativar</button>
      )}
    </li>
  )
}

function InvitationRow({ invitation, canManage, onRevoke }: { invitation: PendingInvitation; canManage: boolean; onRevoke: () => void }) {
  function revoke() {
    if (!window.confirm(`Cancelar o convite de ${invitation.email}? O link enviado para de funcionar.`)) return
    onRevoke()
  }

  return (
    <li style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 12, padding: '14px 0', borderBottom: '1px solid var(--line-soft)' }}>
      <span style={{ flex: '1 1 240px', minWidth: 0, display: 'flex', flexDirection: 'column', gap: 2 }}>
        <span style={{ fontSize: 16, fontWeight: 700, overflowWrap: 'anywhere' }}>{invitation.email}</span>
        <span style={{ fontSize: 14, color: 'var(--ink-soft)' }}>
          {roleLabel[invitation.role]}, convidado{invitation.invitedByName ? ` por ${invitation.invitedByName}` : ''}. Vale até {shortDate(invitation.expiresAt)}.
        </span>
      </span>
      {canManage && <button type="button" className="btn btn-danger" onClick={revoke}>Cancelar convite</button>}
    </li>
  )
}
