import { useEffect } from 'react'

import type { PageResponse } from '@/shared/api/types'

/**
 * 범위를 넘은 페이지면 마지막 장으로. 다음 버튼 연타·연속 삭제가 빈 페이지에 멈추는 것 방지
 * 화면이 빈 목록 안내를 띄우면 페이지 이동 UI 까지 사라져 빠져나올 길이 없음
 */
export function usePageInRange<T>(
  data: PageResponse<T> | null,
  setPage: (page: number) => void,
) {
  useEffect(() => {
    if (data !== null && data.items.length === 0 && data.page > 0) {
      setPage(Math.max(0, data.totalPages - 1))
    }
  }, [data, setPage])
}
