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
  const { t, f } = useI18n()

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
        // 범위 안으로 자름. 부르는 쪽이 보이는 장 기준으로 계산해 응답 전 연타는 한 장만 넘어감
        onClick={() => onChange((current) => Math.max(0, current - 1))}
      >
        <ChevronLeft />
      </Button>

      <span className="min-w-16 text-center text-caption tabular-nums text-muted-foreground">
        {f.number(page + 1)} / {f.number(totalPages)}
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
