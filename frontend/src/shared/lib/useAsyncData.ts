import { useCallback, useEffect, useState } from 'react'
import type { Dispatch, SetStateAction } from 'react'

import { ApiError } from '@/shared/api/client'

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
 * "요청해서 화면에 뿌린다"의 공통 부분.
 *
 * 화면 4곳(차량 목록·차량 상세·정비 이력·다음 정비 시점)이 같은 코드를 각자 들고 있었다.
 * 특히 cancelled 플래그는 빠뜨려도 평소엔 티가 안 나다가 가끔 경고가 뜨는 종류라,
 * 손으로 매번 쓰면 언젠가 한 곳이 빠진다.
 *
 * load 는 반드시 useCallback 으로 감싸서 넘긴다. 그래야 렌더마다 새 함수가 만들어져
 * 요청이 무한히 반복되는 일이 없다. 무엇이 바뀌면 다시 부를지는 그 useCallback 의
 * 의존성 배열이 정한다.
 *
 * 조건은 그대로인데 다시 불러야 할 때(저장·삭제 직후)는 useCallback 에 가짜 의존성을
 * 넣지 말고 reload() 를 부른다. 안 쓰는 값을 의존성에 넣는 건 린터도 잡아낸다.
 */
export function useAsyncData<T>(load: () => Promise<T>, fallbackMessage: string): AsyncData<T> {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  // reload() 가 이 값을 올리면 아래 effect 가 다시 돈다.
  const [reloadCount, setReloadCount] = useState(0)
  const reload = useCallback(() => setReloadCount((current) => current + 1), [])

  useEffect(() => {
    // 응답이 늦게 도착했을 때 이미 사라진 컴포넌트에 setState 하는 것을 막는다.
    // StrictMode 는 개발 중에 effect 를 일부러 두 번 실행하므로 반드시 필요하다.
    let cancelled = false

    async function run() {
      try {
        const result = await load()
        if (cancelled) return
        setData(result)
        setError(null)
      } catch (caught) {
        if (cancelled) return
        // 백엔드가 준 메시지가 있으면 그걸 쓴다. 네트워크 자체가 끊긴 경우엔 없다.
        setError(caught instanceof ApiError ? caught.message : fallbackMessage)
      } finally {
        // 재조회일 때 setLoading(true) 로 되돌리지 않는다. 이미 보이던 내용이 깜빡인다.
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
