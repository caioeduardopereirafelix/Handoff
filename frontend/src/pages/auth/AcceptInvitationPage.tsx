import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { invitations } from '../../api/api'
import { ApiError } from '../../api/http'
import { ThemeToggle } from '../../components/ThemeToggle'
import { shortDate } from '../../lib/requests'
import { messageOf, useLoad } from '../../lib/useLoad'

export function AcceptInvitationPage() {
  const { token = '' } = useParams()
  const navigate = useNavigate()
  const preview = useLoad(() => invitations.preview(token), [token])
  const [name, setName] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [sending, setSending] = useState(false)

  const passwordTooLong = new TextEncoder().encode(password).length > 72

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (passwordTooLong) return
    setSending(true)
    setError(null)
    setFieldErrors({})
    try {
      await invitations.accept(token, { name: name.trim(), password })
      navigate('/app', { replace: true })
    } catch (reason) {
      setError(messageOf(reason))
      if (reason instanceof ApiError) setFieldErrors(reason.fieldErrors)
      setSending(false)
    }
  }

  if (preview.loading && !preview.data) {
    return <p className="container" style={{ paddingTop: 40, color: 'var(--muted)' }}>Abrindo o convite…</p>
  }

  if (preview.error) {
    const gone = preview.error.status === 404
    return (
      <div className="auth-wrap">
        <ThemeToggle className="auth-theme" />
        <div className="auth-card">
          <span className="wordmark">handoff</span>
          <h1 style={{ fontSize: 30, fontWeight: 800, letterSpacing: '-0.03em' }}>{gone ? 'Este convite não vale mais' : 'Não foi possível abrir o convite'}</h1>
          <p style={{ fontSize: 17, lineHeight: 1.5, color: 'var(--ink-soft)' }}>
            {gone
              ? 'Ele pode ter vencido, sido cancelado ou já ter sido usado. Peça um convite novo a quem convidou você.'
              : preview.error.message}
          </p>
          <p style={{ fontSize: 15, color: 'var(--muted)' }}>Já tem conta? <Link to="/entrar">Entrar</Link></p>
        </div>
      </div>
    )
  }

  const invitation = preview.data!

  return (
    <div className="auth-wrap">
      <ThemeToggle className="auth-theme" />
      <form className="auth-card" onSubmit={submit}>
        <span className="wordmark">handoff</span>
        <div>
          <h1 style={{ fontSize: 32, fontWeight: 800, letterSpacing: '-0.03em' }}>Entrar na equipe {invitation.organizationName}</h1>
          <p style={{ marginTop: 8, color: 'var(--muted)', fontSize: 16 }}>
            {invitation.invitedByName} convidou você. Crie a sua senha para acessar os projetos. O convite vale até {shortDate(invitation.expiresAt)}.
          </p>
        </div>
        <div className="field">
          <label htmlFor="convite-email">E-mail</label>
          <input id="convite-email" className="input" type="email" value={invitation.email} readOnly autoComplete="username" />
        </div>
        <div className="field">
          <label htmlFor="convite-nome">Seu nome</label>
          <input id="convite-nome" className="input" required maxLength={120} autoComplete="name" value={name} onChange={(e) => setName(e.target.value)} />
          {fieldErrors.name && <span className="hint" style={{ color: 'var(--danger)' }}>{fieldErrors.name}</span>}
        </div>
        <div className="field">
          <label htmlFor="convite-senha">Senha</label>
          <input id="convite-senha" className="input" type="password" required minLength={8} autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} aria-describedby="convite-senha-dica" />
          <span id="convite-senha-dica" className="hint" style={{ color: passwordTooLong || fieldErrors.password ? 'var(--danger)' : undefined }}>
            {passwordTooLong ? 'Senha longa demais. Use uma mais curta.' : fieldErrors.password ?? 'Pelo menos 8 caracteres.'}
          </span>
        </div>
        {error && <p className="error-box" role="alert">{error}</p>}
        <button type="submit" className="btn btn-primary btn-large" disabled={sending || passwordTooLong}>{sending ? 'Entrando…' : 'Entrar na equipe'}</button>
      </form>
    </div>
  )
}
