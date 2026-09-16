import { useCallback } from 'react'

import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { ErrorText, LoadingText } from '@/shared/ui/feedback/state'
import { formatKm, formatNumber, formatWon } from '@/shared/lib/format/format'
import { useAsyncData } from '@/shared/lib/hooks/useAsyncData'
import { fetchFuelSummary } from '@/features/fuel/api/endpoints/endpoints'

/**
 * 평균 연비를 화면의 주인공으로 둔다. 이 앱이 주유 기록을 받는 이유가 이 숫자 하나다.
 * 재조회는 부모가 key 를 바꿔 컴포넌트를 재생성한다 (NextServiceCard 와 같은 방식).
 */
export function FuelSummaryCard({ vehicleId }: { vehicleId: number }) {
  const load = useCallback(() => fetchFuelSummary(vehicleId), [vehicleId])
  const { data, loading, error } = useAsyncData(load, '주유 요약을 불러오지 못했습니다.')

  if (loading) {
    return <LoadingText />
  }

  if (error !== null || data === null) {
    return <ErrorText message={error ?? '주유 요약을 불러오지 못했습니다.'} />
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>연비</CardTitle>
      </CardHeader>

      <CardContent className="flex flex-col gap-6">
        {data.averageEfficiency === null ? (
          <p className="text-[0.8125rem] leading-relaxed text-muted-foreground">
            {data.recordCount < 2
              ? '주유 기록이 2건 이상 쌓이면 평균 연비를 계산합니다. 첫 기록은 기준점이 됩니다.'
              : '주행거리가 늘어난 기록이 없어 연비를 계산할 수 없습니다.'}
          </p>
        ) : (
          <div className="flex items-baseline gap-3">
            {/* 히어로 숫자에는 tabular-nums 를 붙이지 않는다 — 세로로 줄 맞출 상대가 없다. */}
            <span className="text-display text-strong">{data.averageEfficiency.toFixed(2)}</span>
            <span className="text-muted-foreground">km/L</span>
          </div>
        )}

        {/* 격자 사이로 부모의 선 색이 비치게 한다. 칸마다 border 를 주면 맞닿는 자리가 2px 이 된다. */}
        <dl className="grid grid-cols-2 gap-px overflow-hidden border border-border bg-border sm:grid-cols-4">
          <Stat label="기록" value={`${formatNumber(data.recordCount)}건`} />
          <Stat
            label="주행"
            value={data.totalDistance === null ? '—' : formatKm(data.totalDistance)}
          />
          <Stat label="주유량" value={`${data.totalLiters.toFixed(2)} L`} />
          <Stat label="총 유류비" value={formatWon(data.totalCost)} />
        </dl>
      </CardContent>
    </Card>
  )
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex min-w-0 flex-col gap-1.5 bg-card px-4 py-3.5">
      <dt className="text-eyebrow text-faint">{label}</dt>
      <dd className="truncate text-figure tabular-nums text-strong">{value}</dd>
    </div>
  )
}
