import { app } from '../api/api'
import type { Client, PortalAccess, Project, ServiceRequest } from '../api/types'
import { relativeDay, turnOf } from './requests'

export interface ProjectSummary {
  project: Project
  requests: ServiceRequest[]
  links: PortalAccess[]
  mine: number
  theirs: number
  done: number
}

export interface ClientGroup {
  client: Client
  projects: ProjectSummary[]
}

export async function loadProjectSummaries(): Promise<ClientGroup[]> {
  const [clients, projects] = await Promise.all([app.clients(), app.projects()])
  const summaries = await Promise.all(projects.map(async (project) => {
    const [requests, links] = await Promise.all([app.projectRequests(project.id), app.portalLinks(project.id)])
    return summarize(project, requests, links)
  }))
  return clients.map((client) => ({
    client,
    projects: summaries.filter((s) => s.project.clientId === client.id),
  }))
}

export function summarize(project: Project, requests: ServiceRequest[], links: PortalAccess[]): ProjectSummary {
  return {
    project,
    requests,
    links,
    mine: requests.filter((r) => turnOf(r.status) === 'team').length,
    theirs: requests.filter((r) => turnOf(r.status) === 'client').length,
    done: requests.filter((r) => turnOf(r.status) === 'done').length,
  }
}

export function turnSummary(summary: Pick<ProjectSummary, 'mine' | 'theirs'>): string {
  const parts: string[] = []
  if (summary.mine > 0) parts.push(`${summary.mine} com você`)
  if (summary.theirs > 0) parts.push(`${summary.theirs} com o cliente`)
  return parts.length > 0 ? parts.join(', ') : 'Nada pendente'
}

export interface LinkStatus {
  text: string
  tone: 'ok' | 'warning' | 'missing'
}

export function linkStatus(links: PortalAccess[]): LinkStatus {
  const active = links.filter((l) => l.active).sort((a, b) => b.createdAt.localeCompare(a.createdAt))
  if (active.length === 0) return { text: 'Sem link do portal ativo', tone: 'missing' }
  const link = active[0]
  const daysLeft = Math.ceil((new Date(link.expiresAt).getTime() - Date.now()) / 86_400_000)
  if (daysLeft <= 7) return { text: `Link expira em ${daysLeft} ${daysLeft === 1 ? 'dia' : 'dias'}`, tone: 'warning' }
  if (link.lastUsedAt) return { text: `Link ativo, usado ${relativeDay(link.lastUsedAt)}`, tone: 'ok' }
  return { text: 'Link ativo, ainda não usado', tone: 'ok' }
}

export const linkToneColor: Record<LinkStatus['tone'], string> = {
  ok: 'var(--ink-soft)',
  warning: '#8a5a00',
  missing: 'var(--danger)',
}
