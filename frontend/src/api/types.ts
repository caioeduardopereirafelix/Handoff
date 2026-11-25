export type Role = 'OWNER' | 'MEMBER'
export type ActorType = 'USER' | 'CLIENT'
export type RequestStatus = 'OPEN' | 'IN_PROGRESS' | 'AWAITING_APPROVAL' | 'CHANGES_REQUESTED' | 'DONE' | 'CANCELED'
export type RequestPriority = 'LOW' | 'MEDIUM' | 'HIGH'
export type ProjectStatus = 'ACTIVE' | 'ARCHIVED'
export type ActivityAction = 'REQUEST_CREATED' | 'STATUS_CHANGED' | 'COMMENT_ADDED' | 'ASSIGNMENT_CHANGED' | 'ATTACHMENT_ADDED'

export interface TokenResponse {
  token: string
  type: string
  expiration: number
  refreshToken: string
}

export interface Me {
  id: string
  name: string
  email: string
  role: Role
  organizationId: string
  organizationName: string
  organizationSlug: string
}

export interface Client {
  id: string
  name: string
  contactName: string | null
  email: string | null
  createdAt: string
}

export interface Project {
  id: string
  clientId: string
  clientName: string
  name: string
  status: ProjectStatus
  createdAt: string
}

export interface ServiceRequest {
  id: string
  projectId: string
  number: number
  code: string
  title: string
  description: string | null
  priority: RequestPriority
  status: RequestStatus
  createdByType: ActorType
  assigneeId: string | null
  assigneeName: string | null
  dueDate: string | null
  version: number
  createdAt: string
  updatedAt: string
}

export interface Comment {
  id: string
  requestId: string
  authorType: ActorType
  authorName: string
  body: string
  createdAt: string
}

export interface Attachment {
  id: string
  requestId: string
  commentId: string | null
  originalName: string
  contentType: string
  sizeBytes: number
  uploadedByType: ActorType
  uploadedByName: string
  createdAt: string
  completedAt: string | null
}

export interface Activity {
  id: string
  action: ActivityAction
  actorType: ActorType
  actorName: string
  fromStatus: RequestStatus | null
  toStatus: RequestStatus | null
  createdAt: string
}

export interface PortalAccess {
  id: string
  createdAt: string
  expiresAt: string
  revokedAt: string | null
  lastUsedAt: string | null
  active: boolean
}

export interface PortalAccessCreated {
  id: string
  token: string
  portalPath: string
  expiresAt: string
  emailSent: boolean
}

export interface PortalProject {
  projectId: string
  projectName: string
  clientName: string
  status: ProjectStatus
}

export interface UploadUrl {
  attachmentId: string
  uploadUrl: string
  method: string
  headers: Record<string, string>
  expiresAt: string
}

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}
