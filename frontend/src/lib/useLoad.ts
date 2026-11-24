import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '../api/http'

export interface Loaded<T> {
  data: T | null
  error: ApiError | null
  loading: boolean
  reload: () => void
}

export function useLoad<T>(load: () => Promise<T>, deps: unknown[]): Loaded<T> {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<ApiError | null>(null)
  const [loading, setLoading] = useState(true)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    load()
      .then((value) => {
        if (!active) return
        setData(value)
        setError(null)
      })
      .catch((reason) => {
        if (!active) return
        setError(reason instanceof ApiError ? reason : new ApiError(0, 'Não foi possível conectar ao servidor.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, version])

  const reload = useCallback(() => setVersion((v) => v + 1), [])
  return { data, error, loading, reload }
}

export function messageOf(error: unknown): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return 'Algo deu errado. Tente de novo.'
}
