import { useEffect, useState } from 'react'
import type { SetStateAction } from 'react'

import type { PageResponse } from '@/shared/api/types'

/**
 * 쪽 이동 조회가 실패해도 받아 둔 쪽과 페이지 이동을 남김. keepPreviousData 는 받는 동안만 앞 쪽을 줌
 * 실패하면 page 를 보이는 쪽으로 되돌림. 안 되돌리면 저장·삭제 뒤 재조회가 보이지 않는 쪽을 읽음
 * scope 가 바뀌면(정비 종류 필터) 버림. 다른 조건의 목록을 오류 아래 보이지 않게
 */
export function useShownPage<T>({
  page,
  setPage,
  data,
  isPlaceholderData,
  isError,
  error,
  refetch,
  scope = null,
}: {
  page: number
  setPage: (next: SetStateAction<number>) => void
  data: PageResponse<T> | undefined
  isPlaceholderData: boolean
  isError: boolean
  error: unknown
  refetch: () => unknown
  scope?: unknown
}) {
  const [kept, setKept] = useState<{ data: PageResponse<T>; scope: unknown } | null>(null)
  // 되돌린 쪽 이동의 실패. 되돌린 쪽의 재조회가 성공해도 다음 이동까지 남김
  const [moveFailure, setMoveFailure] = useState<{ error: unknown; scope: unknown } | null>(null)

  // 실제로 받은 쪽만 기억. 렌더 중 갱신이라 한 프레임 늦지 않음
  if (data !== undefined && !isPlaceholderData && (kept === null || kept.data !== data || kept.scope !== scope)) {
    setKept({ data, scope })
  }

  const keptPage = kept !== null && kept.scope === scope ? kept.data : undefined
  const shown = data ?? keptPage

  // 고른 쪽을 못 받았으면 보이는 쪽으로 복귀. 실패는 렌더 중에 기억, page 는 부모 상태일 수 있어 effect 에서
  const moveFailed = isError && data === undefined && keptPage !== undefined && keptPage.page !== page
  if (moveFailed && moveFailure?.error !== error) {
    setMoveFailure({ error, scope })
  }
  useEffect(() => {
    if (moveFailed && keptPage !== undefined) {
      setPage(keptPage.page)
    }
  }, [moveFailed, keptPage, setPage])

  /** 보이는 쪽 기준으로 옮김. 지금 쪽의 재조회가 실패했으면 page 가 그대로라 직접 다시 조회 */
  function changePage(updater: (current: number) => number) {
    setMoveFailure(null)
    const target = updater(shown?.page ?? page)
    if (target !== page) {
      setPage(target)
    } else if (isError) {
      void refetch()
    }
  }

  const failure = moveFailure !== null && moveFailure.scope === scope ? moveFailure.error : null
  return { shown, changePage, error: error ?? failure }
}
