import { appRequest, json, publicRequest, session } from './http'
import type {
  Activity,
  Attachment,
  Client,
  Comment,
  InvitationCreated,
  InvitationPreview,
  Me,
  Page,
  PortalAccess,
  PortalAccessCreated,
  PortalProject,
  Project,
  ProjectStatus,
  RequestPriority,
  RequestStatus,
  Role,
  ServiceRequest,
  Team,
  TeamMember,
  TokenResponse,
  UploadUrl,
} from './types'

export interface NewRequest {
  title: string
  description?: string
  priority?: RequestPriority
}

export interface PageQuery {
  page?: number
  size?: number
  status?: RequestStatus[]
}

const MAX_PAGE_SIZE = 100

function withQuery(path: string, query: PageQuery & { clientId?: string } = {}): string {
  const params = new URLSearchParams()
  if (query.clientId) params.set('clientId', query.clientId)
  if (query.page !== undefined) params.set('page', String(query.page))
  if (query.size !== undefined) params.set('size', String(query.size))
  query.status?.forEach((status) => params.append('status', status))
  const text = params.toString()
  return text ? `${path}?${text}` : path
}

export async function allPages<T>(load: (query: PageQuery) => Promise<Page<T>>, query: PageQuery = {}): Promise<T[]> {
  const items: T[] = []
  for (let page = 0; ; page += 1) {
    const result = await load({ ...query, page, size: MAX_PAGE_SIZE })
    items.push(...result.items)
    if (page + 1 >= result.totalPages) return items
  }
}

export interface RequestScope {
  base: string
  request: <T>(path: string, init?: RequestInit) => Promise<T>
}

export const auth = {
  async login(email: string, password: string) {
    session.store(await publicRequest<TokenResponse>('/api/auth/login', { method: 'POST', ...json({ email, password }) }))
  },
  async register(data: { organizationName: string; name: string; email: string; password: string }) {
    session.store(await publicRequest<TokenResponse>('/api/auth/register', { method: 'POST', ...json(data) }))
  },
  async logout() {
    const refreshToken = session.refreshToken()
    session.clear()
    if (refreshToken) {
      await publicRequest('/api/auth/logout', { method: 'POST', ...json({ refreshToken }) }).catch(() => undefined)
    }
  },
}

export const app = {
  me: () => appRequest<Me>('/api/app/me'),
  clients: (query?: PageQuery) => appRequest<Page<Client>>(withQuery('/api/app/clients', query)),
  client: (id: string) => appRequest<Client>(`/api/app/clients/${id}`),
  createClient: (data: { name: string; contactName?: string; email?: string }) =>
    appRequest<Client>('/api/app/clients', { method: 'POST', ...json(data) }),
  projects: (query?: PageQuery) => appRequest<Page<Project>>(withQuery('/api/app/projects', query)),
  project: (id: string) => appRequest<Project>(`/api/app/projects/${id}`),
  createProject: (data: { clientId: string; name: string }) =>
    appRequest<Project>('/api/app/projects', { method: 'POST', ...json(data) }),
  deleteProject: (id: string) => appRequest<void>(`/api/app/projects/${id}`, { method: 'DELETE' }),
  updateProject: (id: string, data: { name: string; status: ProjectStatus }) =>
    appRequest<Project>(`/api/app/projects/${id}`, { method: 'PUT', ...json(data) }),
  projectRequests: (projectId: string, query?: PageQuery) =>
    appRequest<Page<ServiceRequest>>(withQuery(`/api/app/projects/${projectId}/requests`, query)),
  createRequest: (projectId: string, data: NewRequest) =>
    appRequest<ServiceRequest>(`/api/app/projects/${projectId}/requests`, { method: 'POST', ...json(data) }),
  request: (id: string) => appRequest<ServiceRequest>(`/api/app/requests/${id}`),
  assign: (id: string, data: { assigneeId: string | null; dueDate: string | null }) =>
    appRequest<ServiceRequest>(`/api/app/requests/${id}/assignment`, { method: 'PUT', ...json(data) }),
  changeStatus: (id: string, status: RequestStatus) =>
    appRequest<ServiceRequest>(`/api/app/requests/${id}/status`, { method: 'PATCH', ...json({ status }) }),
  portalLinks: (projectId: string) => appRequest<PortalAccess[]>(`/api/app/projects/${projectId}/portal-access`),
  createPortalLink: (projectId: string, data: { expiresInDays?: number; sendToClient?: boolean }) =>
    appRequest<PortalAccessCreated>(`/api/app/projects/${projectId}/portal-access`, { method: 'POST', ...json(data) }),
  revokePortalLink: (projectId: string, accessId: string) =>
    appRequest<void>(`/api/app/projects/${projectId}/portal-access/${accessId}`, { method: 'DELETE' }),
  scope: (requestId: string): RequestScope => ({
    base: `/api/app/requests/${requestId}`,
    request: appRequest,
  }),
  team: () => appRequest<Team>('/api/app/team'),
  invite: (data: { email: string; role: Role }) =>
    appRequest<InvitationCreated>('/api/app/team/invitations', { method: 'POST', ...json(data) }),
  revokeInvitation: (id: string) => appRequest<void>(`/api/app/team/invitations/${id}`, { method: 'DELETE' }),
  updateMember: (id: string, data: { role?: Role; active?: boolean }) =>
    appRequest<TeamMember>(`/api/app/team/members/${id}`, { method: 'PATCH', ...json(data) }),
}

export const invitations = {
  preview: (token: string) => publicRequest<InvitationPreview>(`/api/invitations/${encodeURIComponent(token)}`),
  async accept(token: string, data: { name: string; password: string }) {
    session.store(await publicRequest<TokenResponse>(`/api/invitations/${encodeURIComponent(token)}/accept`, { method: 'POST', ...json(data) }))
  },
}

export const portal = (token: string) => {
  const base = `/api/portal/${encodeURIComponent(token)}`
  return {
    project: () => publicRequest<PortalProject>(`${base}/project`),
    requests: (query?: PageQuery) => publicRequest<Page<ServiceRequest>>(withQuery(`${base}/requests`, query)),
    request: (id: string) => publicRequest<ServiceRequest>(`${base}/requests/${id}`),
    createRequest: (data: NewRequest) =>
      publicRequest<ServiceRequest>(`${base}/requests`, { method: 'POST', ...json(data) }),
    approve: (id: string) => publicRequest<ServiceRequest>(`${base}/requests/${id}/approve`, { method: 'POST' }),
    requestChanges: (id: string) =>
      publicRequest<ServiceRequest>(`${base}/requests/${id}/request-changes`, { method: 'POST' }),
    scope: (requestId: string): RequestScope => ({
      base: `${base}/requests/${requestId}`,
      request: publicRequest,
    }),
  }
}

export const thread = {
  comments: (scope: RequestScope) => scope.request<Comment[]>(`${scope.base}/comments`),
  addComment: (scope: RequestScope, body: string) =>
    scope.request<Comment>(`${scope.base}/comments`, { method: 'POST', ...json({ body }) }),
  attachments: (scope: RequestScope) => scope.request<Attachment[]>(`${scope.base}/attachments`),
  activity: (scope: RequestScope) => scope.request<Activity[]>(`${scope.base}/activity`),
  downloadUrl: (scope: RequestScope, attachmentId: string) =>
    scope.request<{ downloadUrl: string }>(`${scope.base}/attachments/${attachmentId}/download-url`),
  async upload(scope: RequestScope, file: File, commentId?: string): Promise<Attachment> {
    const contentType = file.type || 'application/octet-stream'
    const upload = await scope.request<UploadUrl>(`${scope.base}/attachments/upload-url`, {
      method: 'POST',
      ...json({ fileName: file.name, contentType, sizeBytes: file.size, commentId }),
    })
    const headers = new Headers()
    Object.entries(upload.headers).forEach(([name, value]) => {
      const lower = name.toLowerCase()
      if (lower !== 'host' && lower !== 'content-length') headers.set(name, value)
    })
    headers.set('Content-Type', contentType)
    const response = await fetch(upload.uploadUrl, { method: 'PUT', headers, body: file })
    if (!response.ok) {
      throw new Error(`Não foi possível enviar ${file.name}. Tente de novo.`)
    }
    return scope.request<Attachment>(`${scope.base}/attachments/${upload.attachmentId}/complete`, { method: 'POST' })
  },
}
