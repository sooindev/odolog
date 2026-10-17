import { useState } from 'react'
import type { SetStateAction } from 'react'

import type { PageResponse } from '@/shared/api/types'

/**
 * 쪽 이동 조회가 실패해도 받아 둔 쪽과 페이지 이동을 남김. keepPreviousData 는 받는 동안만 앞 쪽을 줌
 * scope 가 바뀌면(정비 종류 필터) 버림. 다른 조건의 목록을 오류 아래 보이지 않게
 */
export function useShownPage<T>({
  page,
  setPage,
  data,
  isPlaceholderData,
  isError,
  refetch,
  scope = null,
}: {
  page: number
  setPage: (next: SetStateAction<number>) => void
  data: PageResponse<T> | undefined
  isPlaceholderData: boolean
  isError: boolean
  refetch: () => unknown
  scope?: unknown
}) {
  const [kept, setKept] = useState<{ data: PageResponse<T>; scope: unknown } | null>(null)

  // 실제로 받은 쪽만 기억. 렌더 중 갱신이라 한 프레임 늦지 않음
  if (data !== undefined && !isPlaceholderData && (kept === null || kept.data !== data || kept.scope !== scope)) {
    setKept({ data, scope })
  }

  const shown = data ?? (kept !== null && kept.scope === scope ? kept.data : undefined)

  /** 보이는 쪽 기준으로 옮김. 실패한 쪽을 다시 고르면 page 가 그대로라 상태가 안 바뀌어 직접 다시 조회 */
  function changePage(updater: (current: number) => number) {
    const target = updater(shown?.page ?? page)
    if (target !== page) {
      setPage(target)
    } else if (isError) {
      void refetch()
    }
  }

  return { shown, changePage }
}
