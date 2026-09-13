import { ChevronLeft, ChevronRight } from 'lucide-react'

import { Button } from '@/shared/ui/base/button'

/**
 * 목록 페이지 이동. 차량 목록과 정비 이력이 함께 쓴다.
 * 숫자에 tabular-nums 를 준다. 페이지가 1 → 2 로 바뀔 때 "1 / 12" 전체가 흔들린다.
 */
export function Pagination({
  page,
  totalPages,
  hasNext,
  onChange,
}: {
  page: number
  totalPages: number
  hasNext: boolean
  onChange: (updater: (current: number) => number) => void
}) {
  // 페이지가 1장뿐이면 이동 UI 자체를 보여주지 않는다.
  if (totalPages <= 1) {
    return null
  }

  return (
    <div className="flex items-center justify-center gap-2 pt-2">
      <Button
        variant="ghost"
        size="icon-sm"
        aria-label="이전 페이지"
        disabled={page === 0}
        onClick={() => onChange((current) => current - 1)}
      >
        <ChevronLeft />
      </Button>

      <span className="min-w-16 text-center text-[0.8125rem] tabular-nums text-muted-foreground">
        {page + 1} / {totalPages}
      </span>

      <Button
        variant="ghost"
        size="icon-sm"
        aria-label="다음 페이지"
        disabled={!hasNext}
        onClick={() => onChange((current) => current + 1)}
      >
        <ChevronRight />
      </Button>
    </div>
  )
}
