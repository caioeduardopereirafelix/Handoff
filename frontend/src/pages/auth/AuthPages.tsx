import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { auth } from '../../api/api'
import { ApiError } from '../../api/http'
import { messageOf } from '../../lib/useLoad'

export function LoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [sending, setSending] = useState(false)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setSending(true)
    setError(null)
    try {
      await auth.login(email, password)
      navigate('/app')
    } catch (reason) {
      setError(messageOf(reason))
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="auth-wrap">
      <form className="auth-card" onSubmit={submit}>
        <span className="wordmark">handoff</span>
        <div>
          <h1 style={{ fontSize: 32, fontWeight: 800, letterSpacing: '-0.03em' }}>Entrar no painel</h1>
          <p style={{ marginTop: 8, color: 'var(--muted)', fontSize: 16 }}>Para a equipe que atende os clientes. Clientes entram pelo link que receberam.</p>
        </div>
        <div className="field">
          <label htmlFor="email">E-mail</label>
          <input id="email" className="input" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="senha">Senha</label>
          <input id="senha" className="input" type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
        {error && <p className="error-box" role="alert">{error}</p>}
        <button type="submit" className="btn btn-primary btn-large" disabled={sending}>{sending ? 'Entrando…' : 'Entrar'}</button>
        <p style={{ fontSize: 15, color: 'var(--muted)' }}>Ainda não tem conta? <Link to="/criar-conta">Criar conta</Link></p>
      </form>
    </div>
  )
}

export function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ organizationName: '', name: '', email: '', password: '' })
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [sending, setSending] = useState(false)

  const passwordTooLong = new TextEncoder().encode(form.password).length > 72

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (passwordTooLong) return
    setSending(true)
    setError(null)
    setFieldErrors({})
    try {
      await auth.register(form)
      navigate('/app/projetos')
    } catch (reason) {
      setError(messageOf(reason))
      if (reason instanceof ApiError) setFieldErrors(reason.fieldErrors)
    } finally {
      setSending(false)
    }
  }

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [key]: e.target.value })

  return (
    <div className="auth-wrap">
      <form className="auth-card" onSubmit={submit}>
        <span className="wordmark">handoff</span>
        <div>
          <h1 style={{ fontSize: 32, fontWeight: 800, letterSpacing: '-0.03em' }}>Criar conta</h1>
          <p style={{ marginTop: 8, color: 'var(--muted)', fontSize: 16 }}>Você cria o espaço do seu estúdio e depois convida cada cliente por um link.</p>
        </div>
        <div className="field">
          <label htmlFor="org">Nome do estúdio ou empresa</label>
          <input id="org" className="input" required maxLength={120} value={form.organizationName} onChange={set('organizationName')} />
          {fieldErrors.organizationName && <span className="hint" style={{ color: 'var(--danger)' }}>{fieldErrors.organizationName}</span>}
        </div>
        <div className="field">
          <label htmlFor="nome">Seu nome</label>
          <input id="nome" className="input" required maxLength={120} autoComplete="name" value={form.name} onChange={set('name')} />
        </div>
        <div className="field">
          <label htmlFor="email">E-mail</label>
          <input id="email" className="input" type="email" required maxLength={254} autoComplete="email" value={form.email} onChange={set('email')} />
          {fieldErrors.email && <span className="hint" style={{ color: 'var(--danger)' }}>{fieldErrors.email}</span>}
        </div>
        <div className="field">
          <label htmlFor="senha">Senha</label>
          <input id="senha" className="input" type="password" required minLength={8} autoComplete="new-password" value={form.password} onChange={set('password')} aria-describedby="senha-dica" />
          <span id="senha-dica" className="hint" style={{ color: passwordTooLong ? 'var(--danger)' : undefined }}>
            {passwordTooLong ? 'Senha longa demais. Use uma mais curta.' : 'Pelo menos 8 caracteres.'}
          </span>
        </div>
        {error && <p className="error-box" role="alert">{error}</p>}
        <button type="submit" className="btn btn-primary btn-large" disabled={sending || passwordTooLong}>{sending ? 'Criando…' : 'Criar conta'}</button>
        <p style={{ fontSize: 15, color: 'var(--muted)' }}>Já tem conta? <Link to="/entrar">Entrar</Link></p>
      </form>
    </div>
  )
}
