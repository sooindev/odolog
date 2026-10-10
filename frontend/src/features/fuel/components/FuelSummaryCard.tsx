import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'

import { Button } from '@/shared/ui/base/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { ErrorText, Skeleton } from '@/shared/ui/state'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import { queryKeys } from '@/shared/api/queryKeys'
import { fromKmPerLiter, lowerIsBetter } from '@/shared/lib/units'
import { fetchFuelSummary, updateFuelRecord } from '@/features/fuel/api/endpoints'
import type { FuelSummaryResponse } from '@/features/fuel/api/types'

/** 평균 연비 카드. 주유 기록이 바뀌면 캐시 무효화로 재조회 */
export function FuelSummaryCard({ vehicleId }: { vehicleId: string }) {
  const { t, f } = useI18n()
  const queryClient = useQueryClient()
  const { data, isPending: loading, error } = useQuery({
    queryKey: queryKeys.fuelSummary(vehicleId),
    queryFn: () => fetchFuelSummary(vehicleId),
  })
  const [actionError, setActionError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function setResetPoint(recordId: string, resetPoint: boolean) {
    setActionError(null)
    setPending(true)

    try {
      await updateFuelRecord(vehicleId, recordId, { resetPoint })
      // 기준점이 바뀌면 목록의 구간 연비도 바뀜. 요약·목록 함께
      await queryClient.invalidateQueries({ queryKey: queryKeys.fuel(vehicleId) })
    } catch (caught) {
      setActionError(errorMessage(caught, t, t.fuel.summary.resetFailed))
    } finally {
      setPending(false)
    }
  }

  // 껍데기는 항상 렌더. 로딩 중 아래 카드 튐 방지
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>{t.fuel.summary.title}</CardTitle>
        {/* 기록이 없으면 초기화 버튼 없음 */}
        {data !== undefined &&
          data.latestRecordId !== null &&
          (data.resetPointId === null ? (
            <Button
              size="sm"
              variant="ghost"
              disabled={pending}
              onClick={() => {
                // 숫자가 크게 바뀌어 한 번 확인
                if (window.confirm(t.fuel.summary.resetConfirm)) {
                  void setResetPoint(data.latestRecordId as string, true)
                }
              }}
            >
              {pending ? t.common.processing : t.fuel.summary.reset}
            </Button>
          ) : (
            <Button
              size="sm"
              variant="ghost"
              disabled={pending}
              onClick={() => void setResetPoint(data.resetPointId as string, false)}
            >
              {pending ? t.common.processing : t.fuel.summary.unreset}
            </Button>
          ))}
      </CardHeader>

      <CardContent className="flex flex-col gap-6">
        {loading && (
          <div className="flex flex-col gap-4">
            <Skeleton className="h-12 w-40" />
            <Skeleton className="h-16" />
          </div>
        )}

        {/* 재조회 실패는 직전 값 위에 한 줄 */}
        {!loading && (error !== null || data === undefined) && (
          <ErrorText message={errorMessage(error, t, t.fuel.summary.loadFailed)} />
        )}

        {actionError !== null && <ErrorText message={actionError} />}

        {!loading && data !== undefined && (
          <>
            {data.averageEfficiency === null ? (
              <p className="text-caption leading-relaxed text-muted-foreground">
                {data.resetPointId !== null
                  ? t.fuel.summary.afterReset
                  : data.recordCount < 2
                    ? t.fuel.summary.firstRecord
                    : // 이유를 하나로 단정하지 않음. 주유량을 비운 기록만 있어도 여기로 옴
                      t.fuel.summary.noSegment}
              </p>
            ) : (
              <div className="flex items-baseline gap-3" aria-live="polite" aria-atomic="true">
                {/* 히어로 숫자라 tabular-nums 미사용 */}
                <span className="text-display text-strong">{f.efficiencyNumber(data.averageEfficiency)}</span>
                <span className="text-muted-foreground">{f.efficiencyUnit}</span>
              </div>
            )}

            {/* 초기화 이후 구간만의 값임을 명시 */}
            {data.resetPointId !== null && data.averageEfficiency !== null && (
              <p className="text-caption text-muted-foreground">{t.fuel.summary.sinceReset}</p>
            )}

            {/* 불가능한 구간을 뺀 개수 공개. 목록의 확인 필요와 연결 */}
            {data.excludedSegmentCount > 0 && (
              <p className="text-caption leading-relaxed text-muted-foreground">
                {t.fuel.summary.excluded(data.excludedSegmentCount)}
              </p>
            )}

            {/* 기록 누락 구간을 뺀 개수 공개. 목록 숫자와 평균의 불일치 설명 */}
            {data.longSegmentCount > 0 && (
              <p className="text-caption leading-relaxed text-muted-foreground">
                {t.fuel.summary.missing(data.longSegmentCount)}
              </p>
            )}

            {/* 통화가 다른 기록은 더할 수 없어 뺌. 말없이 빼지 않음 */}
            {data.otherCurrencyRecordCount > 0 && (
              <p className="text-caption leading-relaxed text-muted-foreground">
                {t.fuel.summary.otherCurrency(data.otherCurrencyRecordCount, data.currency)}
              </p>
            )}

            {/* gap-px 격자. 칸 사이 1px */}
            <dl className="grid grid-cols-2 gap-px overflow-hidden border border-border bg-border sm:grid-cols-4">
              <Stat label={t.fuel.summary.records} value={t.common.count(f.number(data.recordCount))} />
              <Stat
                label={t.fuel.summary.distance}
                value={data.totalDistance === null ? '—' : f.distance(data.totalDistance)}
              />
              <Stat label={t.fuel.summary.volume} value={f.volume(data.totalLiters)} />
              <Stat label={t.fuel.summary.cost} value={f.money(data.totalCost, data.currency)} />
            </dl>

            {/* 구간이 둘 미만이면 추이 숨김 */}
            {data.trend.length >= 2 && (
              <EfficiencyTrend trend={data.trend} averageEfficiency={data.averageEfficiency} />
            )}
          </>
        )}
      </CardContent>
    </Card>
  )
}

/**
 * 최근 구간 연비 추이. 계열 하나라 범례 없음, 눈금선은 평균 한 줄
 * 세로축은 최솟값의 90% 부터. 변화를 보는 그림이라 아래에 명시
 */
function EfficiencyTrend({
  trend,
  averageEfficiency,
}: {
  trend: FuelSummaryResponse['trend']
  /** 히어로 숫자와 같은 평균(km/L). 점들의 단순 평균은 거리 가중이 아니라 값이 달라짐 */
  averageEfficiency: number | null
}) {
  const { t, f, unitSystem } = useI18n()
  // 화면 단위로 바꾼 값으로 그림. L/100km 는 작을수록 좋아 막대 방향 의미가 뒤집힘(아래 안내)
  const values = trend.map((point) => fromKmPerLiter(unitSystem, point.efficiency))
  const average =
    averageEfficiency === null
      ? values.reduce((sum, value) => sum + value, 0) / values.length
      : fromKmPerLiter(unitSystem, averageEfficiency)
  // 평균선이 그림 밖으로 나가지 않게 범위에 포함
  const max = Math.max(...values, average)
  const floor = Math.min(...values, average) * 0.9
  const span = Math.max(max - floor, 0.01)

  const averageTop = ((max - average) / span) * 100

  return (
    <figure className="flex flex-col gap-2">
      <figcaption className="text-caption text-muted-foreground">
        {t.fuel.summary.trend(trend.length)}
      </figcaption>

      {/* mt-5: 가장 높은 막대의 말풍선 자리. 없으면 제목과 겹침. padding 이면 평균선 % 기준이 어긋남 */}
      <div className="relative mt-5 h-20">
        {/* 평균선. 1px 실선 */}
        <div
          aria-hidden="true"
          className="absolute inset-x-0 flex items-center"
          style={{ top: `${averageTop}%` }}
        >
          <div className="h-px flex-1 bg-border-strong" />
          <span className="ml-2 shrink-0 text-unit tabular-nums text-muted-foreground">
            {t.fuel.summary.average(f.number(average, 1))}
          </span>
        </div>

        <div className="flex h-full items-end gap-0.5 pr-14 sm:gap-1">
          {trend.map((point, index) => (
            <div
              key={`${point.fueledAt}-${index}`}
              // 판정 영역은 칸 전체
              className="group relative flex h-full flex-1 items-end"
              tabIndex={0}
            >
              <div
                // 홈 차트와 같은 막대 색. fill 은 입력칸 면이라 카드 위에서 안 보임
                className="w-full bg-primary transition-opacity duration-200 ease-apple group-hover:opacity-80 group-focus-visible:opacity-80"
                style={{ height: `${Math.max(((values[index] - floor) / span) * 100, 4)}%` }}
              />
              {/* 말풍선과 같은 값이 아래 표에도 */}
              <span className="pointer-events-none absolute -top-1 left-1/2 z-10 -translate-x-1/2 -translate-y-full border border-border bg-card px-2 py-1 text-unit whitespace-nowrap tabular-nums text-strong opacity-0 transition-opacity duration-200 group-hover:opacity-100 group-focus-visible:opacity-100">
                {f.date(point.fueledAt)} · {f.efficiency(point.efficiency)}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* 키보드·스크린리더용 값 목록 */}
      <details className="text-caption text-muted-foreground">
        <summary className="cursor-pointer">{t.common.showAsValues}</summary>
        <ul className="mt-2 flex flex-col gap-1">
          {trend.map((point, index) => (
            <li key={`${point.fueledAt}-${index}-row`} className="tabular-nums">
              {f.date(point.fueledAt)} · {f.efficiency(point.efficiency)}
            </li>
          ))}
        </ul>
      </details>

      <p className="text-unit text-muted-foreground">
        {t.fuel.summary.axisNote}
        {lowerIsBetter(unitSystem) && ` ${t.fuel.summary.lowerIsBetter}`}
      </p>
    </figure>
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
