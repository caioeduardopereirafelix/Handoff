import { useState } from 'react'
import { app } from '../../api/api'
import type { Client, PortalAccessCreated, Project } from '../../api/types'
import { CopyIcon } from '../../components/Icons'
import { shortDate } from '../../lib/requests'
import { messageOf, useLoad } from '../../lib/useLoad'

export function ProjectsPage() {
  const data = useLoad(async () => {
    const [clients, projects] = await Promise.all([app.clients(), app.projects()])
    return { clients, projects }
  }, [])

  if (data.loading && !data.data) return <p style={{ color: 'var(--muted)' }}>Carregando…</p>
  if (data.error) return <p className="error-box" role="alert">{data.error.message}</p>
  const { clients, projects } = data.data!

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 36 }}>
      <div>
        <h1 style={{ fontSize: 'clamp(34px, 4.4vw, 48px)', lineHeight: 1, fontWeight: 800, letterSpacing: '-0.035em' }}>Projetos e clientes</h1>
        <p style={{ marginTop: 12, fontSize: 17, color: 'var(--ink-soft)', maxWidth: '60ch' }}>
          Cada projeto tem o próprio link do portal. O cliente abre solicitações e aprova entregas por ele, sem criar conta.
        </p>
      </div>

      <section style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        <h2 className="section-title">Projetos</h2>
        {projects.length === 0 && <p className="empty">Nenhum projeto ainda. Cadastre um cliente e crie o primeiro projeto abaixo.</p>}
        {projects.map((project) => <ProjectCard key={project.id} project={project} />)}
      </section>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 24 }}>
        <NewProjectForm clients={clients} onCreated={data.reload} />
        <NewClientForm onCreated={data.reload} />
      </div>

      {clients.length > 0 && (
        <section style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <h2 className="section-title">Clientes</h2>
          <div className="panel" style={{ padding: '6px 24px' }}>
            {clients.map((client) => (
              <div key={client.id} style={{ display: 'flex', flexWrap: 'wrap', gap: 16, padding: '14px 0', borderBottom: '1px solid var(--line-soft)' }}>
                <span style={{ flex: '1 1 200px', fontSize: 17, fontWeight: 700 }}>{client.name}</span>
                <span style={{ flex: '1 1 200px', fontSize: 15, color: 'var(--muted)' }}>{client.contactName ?? 'Sem contato'}</span>
                <span style={{ flex: '1 1 240px', fontSize: 15, color: client.email ? 'var(--ink-soft)' : 'var(--muted)' }}>
                  {client.email ?? 'Sem e-mail: não recebe avisos'}
                </span>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}

function ProjectCard({ project }: { project: Project }) {
  const [open, setOpen] = useState(false)
  return (
    <div className="panel" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 16, justifyContent: 'space-between' }}>
        <div>
          <p style={{ fontSize: 19, fontWeight: 700 }}>{project.name}</p>
          <p style={{ fontSize: 15, color: 'var(--muted)', marginTop: 2 }}>
            {project.clientName}{project.status === 'ARCHIVED' ? ', arquivado' : ''}
          </p>
        </div>
        <button type="button" className="btn btn-quiet" aria-expanded={open} onClick={() => setOpen(!open)}>
          {open ? 'Fechar links do portal' : 'Links do portal'}
        </button>
      </div>
      {open && <PortalLinks project={project} />}
    </div>
  )
}

function PortalLinks({ project }: { project: Project }) {
  const links = useLoad(() => app.portalLinks(project.id), [project.id])
  const [days, setDays] = useState(90)
  const [sendToClient, setSendToClient] = useState(true)
  const [created, setCreated] = useState<PortalAccessCreated | null>(null)
  const [copied, setCopied] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const linkUrl = created ? `${window.location.origin}/portal/${created.token}` : ''

  async function create(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    setCopied(false)
    try {
      setCreated(await app.createPortalLink(project.id, { expiresInDays: days, sendToClient }))
      links.reload()
    } catch (reason) {
      setError(messageOf(reason))
    } finally {
      setBusy(false)
    }
  }

  async function revoke(accessId: string) {
    setError(null)
    try {
      await app.revokePortalLink(project.id, accessId)
      links.reload()
    } catch (reason) {
      setError(messageOf(reason))
    }
  }

  async function copy() {
    try {
      await navigator.clipboard.writeText(linkUrl)
      setCopied(true)
    } catch {
      setCopied(false)
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16, borderTop: '1px solid var(--line-soft)', paddingTop: 16 }}>
      {created && (
        <div className="notice-box" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <p>Link criado. Ele aparece só agora: copie e guarde, ou envie ao cliente.{created.emailSent ? ' Também foi enviado por e-mail ao cliente.' : ''}</p>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'center' }}>
            <code style={{ flex: '1 1 320px', minWidth: 0, overflowWrap: 'anywhere', background: '#fff', color: 'var(--ink)', padding: '10px 12px', borderRadius: 10, fontSize: 14 }}>{linkUrl}</code>
            <button type="button" className="btn btn-primary" onClick={copy}><CopyIcon />{copied ? 'Copiado' : 'Copiar link'}</button>
          </div>
        </div>
      )}

      <form onSubmit={create} style={{ display: 'flex', flexWrap: 'wrap', gap: 16, alignItems: 'flex-end' }}>
        <div className="field" style={{ flex: '0 1 180px' }}>
          <label htmlFor={`dias-${project.id}`}>Validade em dias</label>
          <input id={`dias-${project.id}`} className="input" type="number" min={1} max={365} value={days} onChange={(e) => setDays(Number(e.target.value))} />
        </div>
        <label style={{ display: 'flex', alignItems: 'center', gap: 10, minHeight: 48, fontSize: 15, fontWeight: 600 }}>
          <input type="checkbox" checked={sendToClient} onChange={(e) => setSendToClient(e.target.checked)} style={{ width: 20, height: 20 }} />
          Enviar o link por e-mail ao cliente
        </label>
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

function NewClientForm({ onCreated }: { onCreated: () => void }) {
  const [form, setForm] = useState({ name: '', contactName: '', email: '' })
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await app.createClient({ name: form.name, contactName: form.contactName || undefined, email: form.email || undefined })
      setForm({ name: '', contactName: '', email: '' })
      onCreated()
    } catch (reason) {
      setError(messageOf(reason))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="panel" onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <h2 style={{ fontSize: 22, fontWeight: 800 }}>Novo cliente</h2>
      <div className="field">
        <label htmlFor="cliente-nome">Nome do cliente</label>
        <input id="cliente-nome" className="input" required maxLength={120} placeholder="Ex.: Padaria Central" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
      </div>
      <div className="field">
        <label htmlFor="cliente-contato">Pessoa de contato</label>
        <input id="cliente-contato" className="input" maxLength={120} value={form.contactName} onChange={(e) => setForm({ ...form, contactName: e.target.value })} />
      </div>
      <div className="field">
        <label htmlFor="cliente-email">E-mail do cliente</label>
        <input id="cliente-email" className="input" type="email" maxLength={254} value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} aria-describedby="cliente-email-dica" />
        <span id="cliente-email-dica" className="hint">Recebe o link do portal e os avisos de entrega.</span>
      </div>
      {error && <p className="error-box" role="alert">{error}</p>}
      <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Salvando…' : 'Cadastrar cliente'}</button>
    </form>
  )
}

function NewProjectForm({ clients, onCreated }: { clients: Client[]; onCreated: () => void }) {
  const [clientId, setClientId] = useState('')
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await app.createProject({ clientId, name })
      setName('')
      onCreated()
    } catch (reason) {
      setError(messageOf(reason))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="panel" onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <h2 style={{ fontSize: 22, fontWeight: 800 }}>Novo projeto</h2>
      {clients.length === 0 ? (
        <p style={{ color: 'var(--muted)' }}>Cadastre um cliente primeiro. Todo projeto pertence a um cliente.</p>
      ) : (
        <>
          <div className="field">
            <label htmlFor="projeto-cliente">Cliente</label>
            <select id="projeto-cliente" className="input" required value={clientId} onChange={(e) => setClientId(e.target.value)}>
              <option value="" disabled>Escolha o cliente</option>
              {clients.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div className="field">
            <label htmlFor="projeto-nome">Nome do projeto</label>
            <input id="projeto-nome" className="input" required maxLength={120} placeholder="Ex.: Site institucional" value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          {error && <p className="error-box" role="alert">{error}</p>}
          <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Criando…' : 'Criar projeto'}</button>
        </>
      )}
    </form>
  )
}
