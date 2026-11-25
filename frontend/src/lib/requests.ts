import type { Activity, RequestStatus, ServiceRequest } from '../api/types'

export type Turn = 'team' | 'client' | 'done'

export const ACTIVE_STATUSES: RequestStatus[] = ['OPEN', 'IN_PROGRESS', 'CHANGES_REQUESTED', 'AWAITING_APPROVAL']
export const FINISHED_STATUSES: RequestStatus[] = ['DONE', 'CANCELED']

export function turnOf(status: RequestStatus): Turn {
  if (status === 'AWAITING_APPROVAL') return 'client'
  if (status === 'DONE' || status === 'CANCELED') return 'done'
  return 'team'
}

export const statusLabel: Record<RequestStatus, string> = {
  OPEN: 'Aberta',
  IN_PROGRESS: 'Em andamento',
  AWAITING_APPROVAL: 'Aguardando aprovação',
  CHANGES_REQUESTED: 'Ajuste pedido',
  DONE: 'Concluída',
  CANCELED: 'Cancelada',
}

export interface TeamAction {
  status: RequestStatus
  label: string
  kind: 'primary' | 'danger'
}

export function teamActions(status: RequestStatus): TeamAction[] {
  const cancel: TeamAction = { status: 'CANCELED', label: 'Cancelar solicitação', kind: 'danger' }
  switch (status) {
    case 'OPEN':
      return [{ status: 'IN_PROGRESS', label: 'Começar', kind: 'primary' }, cancel]
    case 'IN_PROGRESS':
      return [{ status: 'AWAITING_APPROVAL', label: 'Entregar para aprovação', kind: 'primary' }, cancel]
    case 'CHANGES_REQUESTED':
      return [{ status: 'IN_PROGRESS', label: 'Retomar o trabalho', kind: 'primary' }, cancel]
    case 'AWAITING_APPROVAL':
      return [cancel]
    default:
      return []
  }
}

const dateFormat = new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'short' })
const timeFormat = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' })

export function shortDate(iso: string): string {
  return dateFormat.format(new Date(iso)).replace('.', '')
}

export function dateTime(iso: string): string {
  return `${shortDate(iso)}, ${timeFormat.format(new Date(iso)).replace(':', 'h')}`
}

export function relativeDay(iso: string): string {
  const date = new Date(iso)
  const today = new Date()
  const startOf = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  const days = Math.round((startOf(today) - startOf(date)) / 86_400_000)
  const time = timeFormat.format(date).replace(':', 'h')
  if (days <= 0) return `hoje às ${time}`
  if (days === 1) return `ontem às ${time}`
  if (days < 7) return `há ${days} dias`
  return `em ${shortDate(iso)}`
}

export function fileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1).replace('.', ',')} MB`
}

export interface TimelineStep {
  title: string
  date: string | null
  state: 'done' | 'current' | 'future'
}

export function timeline(request: ServiceRequest, activity: Activity[], viewer: 'team' | 'client'): TimelineStep[] {
  const steps: TimelineStep[] = activity
    .filter((a) => a.action === 'REQUEST_CREATED' || a.action === 'STATUS_CHANGED')
    .map((a) => ({ title: stepTitle(a, viewer), date: a.createdAt, state: 'done' as const }))
  if (steps.length > 0) {
    steps[steps.length - 1] = { ...steps[steps.length - 1], state: turnOf(request.status) === 'done' ? 'done' : 'current' }
  }
  if (turnOf(request.status) !== 'done') {
    steps.push({ title: 'Concluída', date: null, state: 'future' })
  }
  return steps
}

function stepTitle(activity: Activity, viewer: 'team' | 'client'): string {
  const who = activity.actorType === 'CLIENT' ? (viewer === 'client' ? 'você' : activity.actorName) : activity.actorName
  if (activity.action === 'REQUEST_CREATED') return `Aberta por ${who}`
  switch (activity.toStatus) {
    case 'IN_PROGRESS':
      return activity.fromStatus === 'CHANGES_REQUESTED' ? `${who} retomou o trabalho` : `${who} começou`
    case 'AWAITING_APPROVAL':
      return viewer === 'client' ? 'Entregue, esperando você' : `${who} entregou`
    case 'CHANGES_REQUESTED':
      return `${capitalize(who)} pediu ajuste`
    case 'DONE':
      return `${capitalize(who)} aprovou`
    case 'CANCELED':
      return `${capitalize(who)} cancelou`
    default:
      return statusLabel[activity.toStatus ?? 'OPEN']
  }
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1)
}

export function byNewest(a: ServiceRequest, b: ServiceRequest): number {
  return b.updatedAt.localeCompare(a.updatedAt)
}
