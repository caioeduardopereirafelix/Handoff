import type { TokenResponse } from './types'

export class ApiError extends Error {
  status: number
  fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

const REFRESH_KEY = 'handoff.refreshToken'
let accessToken: string | null = null
let refreshing: Promise<boolean> | null = null
const listeners = new Set<() => void>()

function readRefreshToken(): string | null {
  try {
    return sessionStorage.getItem(REFRESH_KEY)
  } catch {
    return null
  }
}

function writeRefreshToken(value: string | null) {
  try {
    if (value) sessionStorage.setItem(REFRESH_KEY, value)
    else sessionStorage.removeItem(REFRESH_KEY)
  } catch {
    return
  }
}

export const session = {
  hasSession: () => accessToken !== null || readRefreshToken() !== null,
  store(tokens: TokenResponse) {
    accessToken = tokens.token
    writeRefreshToken(tokens.refreshToken)
    listeners.forEach((listener) => listener())
  },
  clear() {
    accessToken = null
    writeRefreshToken(null)
    listeners.forEach((listener) => listener())
  },
  refreshToken: readRefreshToken,
  subscribe(listener: () => void) {
    listeners.add(listener)
    return () => {
      listeners.delete(listener)
    }
  },
}

async function parseError(response: Response): Promise<ApiError> {
  let detail = 'Algo deu errado. Tente de novo em instantes.'
  let fieldErrors: Record<string, string> = {}
  try {
    const body = await response.json()
    if (body?.detail) detail = body.detail
    if (body?.errors) fieldErrors = body.errors
  } catch {
    if (response.status === 404) detail = 'Não encontrado.'
    if (response.status === 403) detail = 'Você não tem permissão para fazer isso.'
  }
  return new ApiError(response.status, detail, fieldErrors)
}

async function send<T>(path: string, init: RequestInit, auth: boolean): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  if (auth && accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  const response = await fetch(path, { ...init, headers })
  if (response.status === 401 && auth) {
    throw new ApiError(401, 'Sua sessão expirou. Entre de novo.')
  }
  if (!response.ok) throw await parseError(response)
  if (response.status === 204) return undefined as T
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}

async function refresh(): Promise<boolean> {
  const token = readRefreshToken()
  if (!token) return false
  if (!refreshing) {
    refreshing = send<TokenResponse>('/api/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken: token }),
    }, false)
      .then((tokens) => {
        session.store(tokens)
        return true
      })
      .catch(() => {
        session.clear()
        return false
      })
      .finally(() => {
        refreshing = null
      })
  }
  return refreshing
}

export async function appRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  if (!accessToken && !(await refresh())) {
    throw new ApiError(401, 'Sua sessão expirou. Entre de novo.')
  }
  try {
    return await send<T>(path, init, true)
  } catch (error) {
    if (error instanceof ApiError && error.status === 401 && (await refresh())) {
      return send<T>(path, init, true)
    }
    if (error instanceof ApiError && error.status === 401) session.clear()
    throw error
  }
}

export function publicRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  return send<T>(path, init, false)
}

export function json(body: unknown): RequestInit {
  return { body: JSON.stringify(body) }
}
