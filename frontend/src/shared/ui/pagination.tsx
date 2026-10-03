import { ChevronLeft, ChevronRight } from 'lucide-react'

import { Button } from '@/shared/ui/base/button'
import { useI18n } from '@/shared/i18n/I18nContext'

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
  const { t } = useI18n()

  // 한 장뿐이면 숨김
  if (totalPages <= 1) {
    return null
  }

  return (
    <div className="flex items-center justify-center gap-2 pt-2">
      <Button
        variant="ghost"
        size="icon-sm"
        aria-label={t.common.previousPage}
        disabled={page === 0}
        // 연타해도 범위 안. 다음 장 응답 전엔 page 가 그대로라 버튼이 안 잠김
        onClick={() => onChange((current) => Math.max(0, current - 1))}
      >
        <ChevronLeft />
      </Button>

      <span className="min-w-16 text-center text-caption tabular-nums text-muted-foreground">
        {page + 1} / {totalPages}
      </span>

      <Button
        variant="ghost"
        size="icon-sm"
        aria-label={t.common.nextPage}
        disabled={!hasNext}
        onClick={() => onChange((current) => Math.min(totalPages - 1, current + 1))}
      >
        <ChevronRight />
      </Button>
    </div>
  )
}
