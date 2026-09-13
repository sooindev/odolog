import { useCallback, useEffect, useState } from 'react'
import type { Dispatch, SetStateAction } from 'react'

import { ApiError } from '@/shared/api/client/client'

export interface AsyncData<T> {
  /** 아직 한 번도 성공하지 못했으면 null. 재조회 중에는 직전 값이 그대로 남는다. */
  data: T | null
  loading: boolean
  error: string | null
  /** 같은 조건으로 다시 불러온다. 저장·삭제 뒤에 부른다. */
  reload: () => void
  /** 이미 아는 새 값으로 갈아끼운다. 갱신 응답을 받았을 때 다시 조회하지 않으려고. */
  setData: Dispatch<SetStateAction<T | null>>
}

/**
 * "요청해서 화면에 뿌린다"의 공통 부분. 조회 화면 4곳이 쓴다.
 *
 * load 는 useCallback 으로 감싸서 넘긴다. 안 그러면 렌더마다 새 함수가 생겨 요청이
 * 무한히 반복된다. 무엇이 바뀌면 다시 부를지는 그 의존성 배열이 정한다.
 * 조건은 그대로인데 다시 불러야 할 때는 가짜 의존성을 넣지 말고 reload() 를 쓴다.
 */
export function useAsyncData<T>(load: () => Promise<T>, fallbackMessage: string): AsyncData<T> {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  // reload() 가 이 값을 올리면 아래 effect 가 다시 돈다.
  const [reloadCount, setReloadCount] = useState(0)
  const reload = useCallback(() => setReloadCount((current) => current + 1), [])

  useEffect(() => {
    // 이미 사라진 컴포넌트에 setState 하는 것을 막는다.
    // StrictMode 가 effect 를 두 번 실행하므로 없으면 바로 드러난다.
    let cancelled = false

    async function run() {
      try {
        const result = await load()
        if (cancelled) return
        setData(result)
        setError(null)
      } catch (caught) {
        if (cancelled) return
        // 백엔드 메시지가 있으면 그걸 쓴다. 네트워크가 끊기면 없다.
        setError(caught instanceof ApiError ? caught.message : fallbackMessage)
      } finally {
        // 재조회 때 setLoading(true) 로 되돌리지 않는다. 보이던 내용이 깜빡인다.
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
