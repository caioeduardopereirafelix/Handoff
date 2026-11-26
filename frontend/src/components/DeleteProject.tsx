import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { app } from '../api/api'
import { messageOf } from '../lib/useLoad'

interface DeleteProjectProps {
  projectId: string
  projectName: string
  requestCount: number
}

export function DeleteProject({ projectId, projectName, requestCount }: DeleteProjectProps) {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [typed, setTyped] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const matches = typed.trim() === projectName.trim()

  async function remove(event: React.FormEvent) {
    event.preventDefault()
    if (!matches) return
    setBusy(true)
    setError(null)
    try {
      await app.deleteProject(projectId)
      navigate('/app/projetos', { replace: true })
    } catch (reason) {
      setError(messageOf(reason))
      setBusy(false)
    }
  }

  function cancel() {
    setOpen(false)
    setTyped('')
    setError(null)
  }

  const requests = requestCount === 1 ? '1 solicitação' : `${requestCount} solicitações`

  return (
    <section className="panel" aria-labelledby="apagar-titulo" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <h2 id="apagar-titulo" style={{ fontSize: 20, fontWeight: 800 }}>Apagar projeto</h2>
      {!open ? (
        <>
          <p style={{ fontSize: 15, color: 'var(--ink-soft)' }}>Remove o projeto e tudo o que há nele. Não dá para desfazer.</p>
          <button type="button" className="btn btn-danger" style={{ alignSelf: 'flex-start' }} onClick={() => setOpen(true)}>Apagar projeto</button>
        </>
      ) : (
        <form onSubmit={remove} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <p className="error-box" style={{ fontWeight: 400 }}>
            Isto apaga para sempre <strong>{requests}</strong>, com os comentários, anexos e histórico, e os links do portal deste projeto. O cliente perde o acesso na hora.
          </p>
          <div className="field">
            <label htmlFor={`confirmar-${projectId}`}>Digite <strong>{projectName}</strong> para confirmar</label>
            <input
              id={`confirmar-${projectId}`}
              className="input"
              value={typed}
              onChange={(e) => setTyped(e.target.value)}
              autoComplete="off"
              autoFocus
            />
          </div>
          {error && <p className="error-box" role="alert">{error}</p>}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            <button type="submit" className="btn btn-danger-solid" disabled={!matches || busy}>{busy ? 'Apagando…' : 'Apagar para sempre'}</button>
            <button type="button" className="btn btn-quiet" onClick={cancel} disabled={busy}>Cancelar</button>
          </div>
        </form>
      )}
    </section>
  )
}
