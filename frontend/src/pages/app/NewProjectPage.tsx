import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { allPages, app } from '../../api/api'
import type { Client, PortalAccessCreated, Project } from '../../api/types'
import { BackIcon, PlusIcon } from '../../components/Icons'
import { CreatedLink } from '../../components/PortalLinks'
import { messageOf, useLoad } from '../../lib/useLoad'

type Step = 1 | 2 | 3 | 'done'
type LinkChoice = 'email' | 'copy' | 'none'

const stepNames = ['Cliente', 'Projeto', 'Link do portal']

export function NewProjectPage() {
  const [params] = useSearchParams()
  const clients = useLoad(() => allPages(app.clients), [])
  if (clients.loading && !clients.data) return <p style={{ color: 'var(--muted)' }}>Carregando…</p>
  if (clients.error) return <p className="error-box" role="alert">{clients.error.message}</p>
  return <Wizard clients={clients.data ?? []} preselectedId={params.get('cliente')} />
}

function Wizard({ clients, preselectedId }: { clients: Client[]; preselectedId: string | null }) {
  const navigate = useNavigate()
  const preselected = clients.find((c) => c.id === preselectedId) ?? null
  const [step, setStep] = useState<Step>(preselected ? 2 : 1)
  const [query, setQuery] = useState(preselected?.name ?? '')
  const [selectedId, setSelectedId] = useState<string | null>(preselected?.id ?? null)
  const [isNew, setIsNew] = useState(clients.length === 0)
  const [contactName, setContactName] = useState('')
  const [email, setEmail] = useState('')
  const [projectName, setProjectName] = useState('')
  const [linkChoice, setLinkChoice] = useState<LinkChoice | null>(null)
  const [days, setDays] = useState(90)
  const [createdClient, setCreatedClient] = useState<Client | null>(null)
  const [createdProject, setCreatedProject] = useState<Project | null>(null)
  const [createdLink, setCreatedLink] = useState<PortalAccessCreated | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const selected = clients.find((c) => c.id === selectedId) ?? null
  const clientName = selected?.name ?? query.trim()
  const clientEmail = selected ? selected.email : email.trim() || null
  const choice: LinkChoice = linkChoice ?? (clientEmail ? 'email' : 'copy')

  const term = query.trim().toLowerCase()
  const matches = clients.filter((c) => !term || c.name.toLowerCase().includes(term)).slice(0, 6)
  const exact = clients.some((c) => c.name.toLowerCase() === term)
  const clientReady = Boolean(selected) || (isNew && query.trim().length > 0)

  function pickClient(client: Client) {
    setSelectedId(client.id)
    setQuery(client.name)
    setIsNew(false)
  }

  function startNewClient() {
    setSelectedId(null)
    setIsNew(true)
  }

  async function finish(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      let clientId = selected?.id ?? createdClient?.id
      if (!clientId) {
        const client = await app.createClient({ name: query.trim(), contactName: contactName.trim() || undefined, email: email.trim() || undefined })
        setCreatedClient(client)
        clientId = client.id
      }
      let project = createdProject
      if (!project) {
        project = await app.createProject({ clientId, name: projectName.trim() })
        setCreatedProject(project)
      }
      if (choice !== 'none') {
        setCreatedLink(await app.createPortalLink(project.id, { expiresInDays: days, sendToClient: choice === 'email' }))
      }
      setStep('done')
    } catch (reason) {
      setError(messageOf(reason))
    } finally {
      setBusy(false)
    }
  }

  if (step === 'done' && createdProject) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 24, maxWidth: 720 }}>
        <h1 style={{ fontSize: 'clamp(34px, 4.4vw, 48px)', lineHeight: 1, fontWeight: 800, letterSpacing: '-0.035em' }}>Projeto criado</h1>
        <p style={{ fontSize: 17, color: 'var(--ink-soft)' }}>
          {createdProject.name}, para {createdProject.clientName}.
          {choice === 'none' ? ' Você pode gerar o link do portal depois, na página do projeto.' : ''}
        </p>
        {createdLink && <CreatedLink created={createdLink} clientName={createdProject.clientName} projectName={createdProject.name} />}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
          <Link to={`/app/projetos/${createdProject.id}`} className="btn btn-primary btn-large">Abrir o projeto</Link>
          <button type="button" className="btn btn-quiet btn-large" onClick={() => navigate(0)}>Criar outro projeto</button>
        </div>
      </div>
    )
  }

  const current = step as 1 | 2 | 3

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24, maxWidth: 720 }}>
      <Link to="/app/projetos" style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontWeight: 600, textDecoration: 'none', minHeight: 44 }}>
        <BackIcon />Projetos
      </Link>
      <h1 style={{ fontSize: 'clamp(34px, 4.4vw, 48px)', lineHeight: 1, fontWeight: 800, letterSpacing: '-0.035em' }}>Novo projeto</h1>

      <ol className="steps" aria-label="Passos">
        {stepNames.map((name, index) => {
          const number = index + 1
          const state = number < current ? 'done' : number === current ? 'current' : ''
          return (
            <li key={name} className={state} aria-current={state === 'current' ? 'step' : undefined}>
              <span className="bar" />
              <span>{number}. {name}</span>
            </li>
          )
        })}
      </ol>

      {current > 1 && (
        <div className="step-row">
          <span><span style={{ fontWeight: 700 }}>Cliente:</span> {clientName}{isNew && !selected ? ' (novo)' : ''}</span>
          <button type="button" className="btn btn-quiet" style={{ minHeight: 40 }} onClick={() => setStep(1)}>Alterar</button>
        </div>
      )}
      {current > 2 && (
        <div className="step-row">
          <span><span style={{ fontWeight: 700 }}>Projeto:</span> {projectName}</span>
          <button type="button" className="btn btn-quiet" style={{ minHeight: 40 }} onClick={() => setStep(2)}>Alterar</button>
        </div>
      )}

      {current === 1 && (
        <form
          className="panel panel-lg"
          style={{ display: 'flex', flexDirection: 'column', gap: 20, padding: 32 }}
          onSubmit={(e) => {
            e.preventDefault()
            if (clientReady) setStep(2)
          }}
        >
          <div>
            <h2 style={{ fontSize: 26, fontWeight: 800, letterSpacing: '-0.02em' }}>Para qual cliente é o projeto?</h2>
            <p style={{ marginTop: 8, fontSize: 16, color: 'var(--ink-soft)' }}>
              {clients.length === 0 ? 'Digite o nome do seu primeiro cliente.' : 'Busque um cliente que você já atende ou digite o nome de um novo.'}
            </p>
          </div>
          <div className="field">
            <label htmlFor="cliente-busca">{clients.length === 0 ? 'Nome do cliente' : 'Cliente'}</label>
            <input
              id="cliente-busca"
              className="input"
              autoComplete="off"
              maxLength={120}
              placeholder="Ex.: Padaria Central"
              value={query}
              onChange={(e) => {
                setQuery(e.target.value)
                setSelectedId(null)
              }}
            />
          </div>

          {clients.length > 0 && (
            <div className="result-list" role="listbox" aria-label="Clientes">
              {matches.map((client) => (
                <button key={client.id} type="button" role="option" aria-selected={selectedId === client.id} className="result-option" onClick={() => pickClient(client)}>
                  <span>{client.name}</span>
                  <span style={{ fontSize: 14, color: selectedId === client.id ? 'var(--ink)' : 'var(--muted)', fontWeight: 400 }}>{client.email ?? 'sem e-mail'}</span>
                </button>
              ))}
              {query.trim() && !exact && (
                <button type="button" role="option" aria-selected={isNew} className="result-option" onClick={startNewClient} style={{ justifyContent: 'flex-start' }}>
                  <PlusIcon />Cadastrar “{query.trim()}” como cliente novo
                </button>
              )}
              {matches.length === 0 && !query.trim() && <p style={{ padding: 16, color: 'var(--muted)' }}>Nenhum cliente encontrado.</p>}
            </div>
          )}

          {isNew && (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 14 }}>
              <div className="field">
                <label htmlFor="cliente-contato">Pessoa de contato</label>
                <input id="cliente-contato" className="input" maxLength={120} value={contactName} onChange={(e) => setContactName(e.target.value)} />
              </div>
              <div className="field">
                <label htmlFor="cliente-email">E-mail do cliente</label>
                <input id="cliente-email" className="input" type="email" maxLength={254} value={email} onChange={(e) => setEmail(e.target.value)} />
              </div>
              <p className="hint" style={{ gridColumn: '1 / -1', fontSize: 14, color: 'var(--muted)' }}>O e-mail recebe o link do portal e os avisos de entrega.</p>
            </div>
          )}

          <div>
            <button type="submit" className="btn btn-primary btn-large" disabled={!clientReady}>Continuar</button>
          </div>
        </form>
      )}

      {current === 2 && (
        <form
          className="panel panel-lg"
          style={{ display: 'flex', flexDirection: 'column', gap: 20, padding: 32 }}
          onSubmit={(e) => {
            e.preventDefault()
            if (projectName.trim()) setStep(3)
          }}
        >
          <h2 style={{ fontSize: 26, fontWeight: 800, letterSpacing: '-0.02em' }}>Qual é o projeto?</h2>
          <div className="field">
            <label htmlFor="projeto-nome">Nome do projeto</label>
            <input id="projeto-nome" className="input" required maxLength={120} placeholder="Ex.: Site institucional" value={projectName} onChange={(e) => setProjectName(e.target.value)} />
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
            <button type="submit" className="btn btn-primary btn-large" disabled={!projectName.trim()}>Continuar</button>
            <button type="button" className="btn btn-quiet btn-large" onClick={() => setStep(1)}>Voltar</button>
          </div>
        </form>
      )}

      {current === 3 && (
        <form className="panel panel-lg" style={{ display: 'flex', flexDirection: 'column', gap: 20, padding: 32 }} onSubmit={finish}>
          <div>
            <h2 style={{ fontSize: 26, fontWeight: 800, letterSpacing: '-0.02em' }}>Como o cliente vai entrar?</h2>
            <p style={{ marginTop: 8, fontSize: 16, color: 'var(--ink-soft)' }}>Pelo link do portal ele abre solicitações e aprova entregas, sem criar conta.</p>
          </div>
          <fieldset style={{ border: 0, margin: 0, padding: 0, display: 'flex', flexDirection: 'column', gap: 10 }}>
            <legend className="visually-hidden">Link do portal</legend>
            {clientEmail && (
              <Choice checked={choice === 'email'} onChange={() => setLinkChoice('email')} title={`Enviar o link por e-mail para ${clientEmail}`} text="Você também vê o link na próxima tela." />
            )}
            <Choice checked={choice === 'copy'} onChange={() => setLinkChoice('copy')} title="Só gerar o link para eu copiar" text="Bom para mandar pelo WhatsApp." />
            <Choice checked={choice === 'none'} onChange={() => setLinkChoice('none')} title="Agora não" text="Você gera o link depois, na página do projeto." />
          </fieldset>
          {choice !== 'none' && (
            <div className="field" style={{ maxWidth: 200 }}>
              <label htmlFor="validade">Validade em dias</label>
              <input id="validade" className="input" type="number" min={1} max={365} value={days} onChange={(e) => setDays(Number(e.target.value))} />
            </div>
          )}
          {error && <p className="error-box" role="alert">{error}</p>}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
            <button type="submit" className="btn btn-primary btn-large" disabled={busy}>
              {busy ? 'Criando…' : choice === 'email' ? 'Criar projeto e enviar o link' : 'Criar projeto'}
            </button>
            <button type="button" className="btn btn-quiet btn-large" onClick={() => setStep(2)} disabled={busy}>Voltar</button>
          </div>
        </form>
      )}

      {stepNames.slice(current).map((name, index) => (
        <div key={name} className="step-row is-future">
          <span style={{ fontWeight: 700 }}>{current + index + 1}. {name}</span>
        </div>
      ))}
    </div>
  )
}

function Choice({ checked, onChange, title, text }: { checked: boolean; onChange: () => void; title: string; text: string }) {
  return (
    <label className={`choice${checked ? ' is-checked' : ''}`}>
      <input type="radio" name="link" checked={checked} onChange={onChange} style={{ width: 20, height: 20, margin: '2px 0 0' }} />
      <span>
        <span style={{ display: 'block', fontSize: 16, fontWeight: 700 }}>{title}</span>
        <span style={{ display: 'block', marginTop: 4, fontSize: 14, color: 'var(--muted)' }}>{text}</span>
      </span>
    </label>
  )
}
