import { useState } from 'react'
import { app } from '../api/api'
import type { ServiceRequest, TeamMember } from '../api/types'
import { messageOf } from '../lib/useLoad'

interface AssigneePickerProps {
  request: ServiceRequest
  members: TeamMember[]
  meId: string
  onChanged: () => void
}

export function AssigneePicker({ request, members, meId, onChanged }: AssigneePickerProps) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const options = members.filter((m) => m.active)
  const currentIsInactive = request.assigneeId !== null && !options.some((m) => m.id === request.assigneeId)

  async function assign(assigneeId: string | null) {
    setBusy(true)
    setError(null)
    try {
      await app.assign(request.id, { assigneeId, dueDate: request.dueDate })
      onChanged()
    } catch (reason) {
      setError(messageOf(reason))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="field" style={{ marginBottom: 24 }}>
      <label htmlFor="responsavel">Responsável</label>
      <select
        id="responsavel"
        className="input"
        value={request.assigneeId ?? ''}
        disabled={busy}
        onChange={(e) => assign(e.target.value || null)}
      >
        <option value="">Ninguém</option>
        {currentIsInactive && <option value={request.assigneeId!}>{request.assigneeName} (desativado)</option>}
        {options.map((member) => (
          <option key={member.id} value={member.id}>{member.id === meId ? `${member.name} (você)` : member.name}</option>
        ))}
      </select>
      {request.assigneeId !== meId && options.some((m) => m.id === meId) && (
        <button type="button" className="btn btn-quiet" style={{ alignSelf: 'flex-start', minHeight: 40 }} disabled={busy} onClick={() => assign(meId)}>
          Atribuir a mim
        </button>
      )}
      <span className="hint">Recebe os avisos quando o cliente comenta, aprova ou pede ajuste.</span>
      {error && <p className="error-box" role="alert">{error}</p>}
    </div>
  )
}
