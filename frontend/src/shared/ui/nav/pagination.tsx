import { ChevronLeft, ChevronRight } from 'lucide-react'

import { Button } from '@/shared/ui/base/button'

/** 목록 페이지 이동. 숫자는 tabular-nums */
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
  // 한 장뿐이면 숨김
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

      <span className="min-w-16 text-center text-caption tabular-nums text-muted-foreground">
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
