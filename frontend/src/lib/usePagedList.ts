import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '../api/http'
import type { Page } from '../api/types'

export interface PagedList<T> {
  items: T[]
  total: number
  loading: boolean
  error: ApiError | null
  hasMore: boolean
  loadMore: () => void
  reload: () => void
}

function toApiError(reason: unknown): ApiError {
  return reason instanceof ApiError ? reason : new ApiError(0, 'Não foi possível conectar ao servidor.')
}

export function usePagedList<T>(load: (page: number) => Promise<Page<T>>, deps: unknown[]): PagedList<T> {
  const [items, setItems] = useState<T[]>([])
  const [next, setNext] = useState(0)
  const [total, setTotal] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<ApiError | null>(null)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    load(0)
      .then((page) => {
        if (!active) return
        setItems(page.items)
        setNext(1)
        setTotal(page.totalItems)
        setTotalPages(page.totalPages)
        setError(null)
      })
      .catch((reason) => {
        if (active) setError(toApiError(reason))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, version])

  const loadMore = useCallback(() => {
    setLoading(true)
    load(next)
      .then((page) => {
        setItems((current) => [...current, ...page.items])
        setNext(next + 1)
        setTotal(page.totalItems)
        setTotalPages(page.totalPages)
        setError(null)
      })
      .catch((reason) => setError(toApiError(reason)))
      .finally(() => setLoading(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [next, ...deps])

  const reload = useCallback(() => setVersion((v) => v + 1), [])
  return { items, total, loading, error, hasMore: next < totalPages, loadMore, reload }
}
