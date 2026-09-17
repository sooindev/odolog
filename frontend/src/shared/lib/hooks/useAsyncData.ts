import { useCallback, useEffect, useState } from 'react'
import type { Dispatch, SetStateAction } from 'react'

import { ApiError } from '@/shared/api/client/client'

export interface AsyncData<T> {
  /** 한 번도 성공 못 했으면 null. 재조회 중에는 직전 값 유지 */
  data: T | null
  loading: boolean
  error: string | null
  /** 같은 조건으로 재조회. 저장·삭제 뒤에 호출 */
  reload: () => void
  /** 이미 아는 값으로 교체. 갱신 응답을 받았을 때 재조회를 피하기 위함 */
  setData: Dispatch<SetStateAction<T | null>>
}

/**
 * 조회 화면 4곳의 공통부
 * load 는 useCallback 필수 — 렌더마다 새 함수가 생기면 요청이 무한 반복
 * 조건은 그대로인데 다시 불러야 하면 가짜 의존성 대신 reload()
 */
export function useAsyncData<T>(load: () => Promise<T>, fallbackMessage: string): AsyncData<T> {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  // reload() 가 올리면 아래 effect 재실행
  const [reloadCount, setReloadCount] = useState(0)
  const reload = useCallback(() => setReloadCount((current) => current + 1), [])

  useEffect(() => {
    // 사라진 컴포넌트에 setState 방지. StrictMode 가 effect 를 두 번 돌려 없으면 바로 드러남
    let cancelled = false

    async function run() {
      try {
        const result = await load()
        if (cancelled) return
        setData(result)
        setError(null)
      } catch (caught) {
        if (cancelled) return
        // 백엔드 메시지 우선. 네트워크가 끊기면 없음
        setError(caught instanceof ApiError ? caught.message : fallbackMessage)
      } finally {
        // 재조회 때 setLoading(true) 금지 — 보이던 내용이 깜빡임
        if (!cancelled) setLoading(false)
      }
    }

    run()

    return () => {
      cancelled = true
    }
  }, [load, fallbackMessage, reloadCount])

  return { data, loading, error, reload, setData }
}
