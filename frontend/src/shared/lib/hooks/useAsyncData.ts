import { useCallback, useEffect, useState } from 'react'
import type { Dispatch, SetStateAction } from 'react'

import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'

export interface AsyncData<T> {
  /** 성공 전이면 null. 재조회 중에는 직전 값 유지 */
  data: T | null
  loading: boolean
  error: string | null
  /** 같은 조건으로 재조회 */
  reload: () => void
  /** 갱신 응답으로 직접 교체 */
  setData: Dispatch<SetStateAction<T | null>>
}

/**
 * 조회 화면 공통 훅
 * load 는 useCallback 필수. 조건 변화 없는 재조회는 reload()
 */
export function useAsyncData<T>(load: () => Promise<T>, fallbackMessage: string): AsyncData<T> {
  const { t } = useI18n()
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  // reload() 트리거
  const [reloadCount, setReloadCount] = useState(0)
  const reload = useCallback(() => setReloadCount((current) => current + 1), [])

  useEffect(() => {
    // 언마운트 후 setState 방지
    let cancelled = false

    async function run() {
      try {
        const result = await load()
        if (cancelled) return
        setData(result)
        setError(null)
      } catch (caught) {
        if (cancelled) return
        // 서버 코드의 문구 우선, 모르면 호출부의 문구
        setError(errorMessage(caught, t, fallbackMessage))
      } finally {
        // 재조회 때 loading 미설정. 깜빡임 방지
        if (!cancelled) setLoading(false)
      }
    }

    run()

    return () => {
      cancelled = true
    }
  }, [load, fallbackMessage, reloadCount, t])

  // 갱신 응답으로 교체하면 직전 재조회 실패 문구는 낡음
  const replaceData = useCallback<Dispatch<SetStateAction<T | null>>>((next) => {
    setData(next)
    setError(null)
  }, [])

  return { data, loading, error, reload, setData: replaceData }
}
