import { useState } from 'react'
import { app } from '../api/api'
import type { Project, ProjectStatus } from '../api/types'
import { messageOf } from '../lib/useLoad'

function useStatusChange(project: Project, onChanged: () => void) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function change(status: ProjectStatus) {
    setBusy(true)
    setError(null)
    try {
      await app.updateProject(project.id, { name: project.name, status })
      onChanged()
    } catch (reason) {
      setError(messageOf(reason))
    } finally {
      setBusy(false)
    }
  }

  return { busy, error, change }
}

export function ArchiveProject({ project, onChanged }: { project: Project; onChanged: () => void }) {
  const { busy, error, change } = useStatusChange(project, onChanged)

  return (
    <section className="panel" aria-labelledby="arquivar-titulo" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <h2 id="arquivar-titulo" style={{ fontSize: 20, fontWeight: 800 }}>Arquivar projeto</h2>
      <p style={{ fontSize: 15, color: 'var(--ink-soft)' }}>
        Tira o projeto do quadro de solicitações e da lista de projetos em andamento. Nada é apagado, e você pode desarquivar quando quiser.
      </p>
      {error && <p className="error-box" role="alert">{error}</p>}
      <button type="button" className="btn btn-quiet" style={{ alignSelf: 'flex-start' }} onClick={() => change('ARCHIVED')} disabled={busy}>
        {busy ? 'Arquivando…' : 'Arquivar projeto'}
      </button>
    </section>
  )
}

export function ArchivedBanner({ project, onChanged }: { project: Project; onChanged: () => void }) {
  const { busy, error, change } = useStatusChange(project, onChanged)

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div className="warning-box" role="status" style={{ alignItems: 'center', flexWrap: 'wrap', justifyContent: 'space-between' }}>
        <span>Este projeto está arquivado. Ele não aparece no quadro de solicitações nem entre os projetos em andamento.</span>
        <button type="button" className="btn btn-quiet" onClick={() => change('ACTIVE')} disabled={busy}>
          {busy ? 'Desarquivando…' : 'Desarquivar'}
        </button>
      </div>
      {error && <p className="error-box" role="alert">{error}</p>}
    </div>
  )
}
