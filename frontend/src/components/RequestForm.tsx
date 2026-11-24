import { useState } from 'react'
import type { NewRequest } from '../api/api'
import type { RequestPriority } from '../api/types'
import { messageOf } from '../lib/useLoad'

interface Props {
  submitLabel: string
  onSubmit: (data: NewRequest) => Promise<void>
  children?: React.ReactNode
}

const priorities: { value: RequestPriority; label: string }[] = [
  { value: 'LOW', label: 'Sem pressa' },
  { value: 'MEDIUM', label: 'Normal' },
  { value: 'HIGH', label: 'Urgente' },
]

export function RequestForm({ submitLabel, onSubmit, children }: Props) {
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [priority, setPriority] = useState<RequestPriority>('MEDIUM')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await onSubmit({ title, description: description || undefined, priority })
    } catch (reason) {
      setError(messageOf(reason))
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="panel panel-lg" style={{ display: 'flex', flexDirection: 'column', gap: 20, maxWidth: 680 }}>
      {children}
      <div className="field">
        <label htmlFor="titulo">O que precisa ser feito</label>
        <input id="titulo" className="input" required maxLength={160} placeholder="Ex.: Trocar a foto da fachada na home" value={title} onChange={(e) => setTitle(e.target.value)} />
      </div>
      <div className="field">
        <label htmlFor="detalhes">Detalhes</label>
        <textarea id="detalhes" className="input" rows={5} maxLength={5000} placeholder="Onde fica, como deve ficar e qualquer referência que ajude." value={description} onChange={(e) => setDescription(e.target.value)} />
        <span className="hint">Arquivos podem ser enviados na conversa, depois de abrir a solicitação.</span>
      </div>
      <fieldset style={{ border: 0, padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 8 }}>
        <legend style={{ fontSize: 15, fontWeight: 700, marginBottom: 8 }}>Prioridade</legend>
        <div className="chips">
          {priorities.map((p) => (
            <button key={p.value} type="button" className="chip" aria-pressed={priority === p.value} onClick={() => setPriority(p.value)}>{p.label}</button>
          ))}
        </div>
      </fieldset>
      {error && <p className="error-box" role="alert">{error}</p>}
      <div>
        <button type="submit" className="btn btn-primary btn-large" disabled={busy}>{busy ? 'Abrindo…' : submitLabel}</button>
      </div>
    </form>
  )
}
