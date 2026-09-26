import { useCallback, useState } from 'react'

import { Button } from '@/shared/ui/base/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { ErrorText, Skeleton } from '@/shared/ui/feedback/state'
import { ApiError } from '@/shared/api/client/client'
import { formatDate, formatKm, formatNumber, formatWon } from '@/shared/lib/format/format'
import { useAsyncData } from '@/shared/lib/hooks/useAsyncData'
import { fetchFuelSummary, updateFuelRecord } from '@/features/fuel/api/endpoints/endpoints'
import type { FuelSummaryResponse } from '@/features/fuel/api/types/types'

/** 평균 연비 카드. 부모가 key 를 바꿔 재생성 */
export function FuelSummaryCard({
  vehicleId,
  onChanged,
}: {
  vehicleId: string
  /** 기준점 변경 시 부모에 알림. 목록 구간 연비도 변경 */
  onChanged: () => void
}) {
  const load = useCallback(() => fetchFuelSummary(vehicleId), [vehicleId])
  const { data, loading, error } = useAsyncData(load, '주유 요약을 불러오지 못했습니다.')
  const [actionError, setActionError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function setResetPoint(recordId: string, resetPoint: boolean) {
    setActionError(null)
    setPending(true)

    try {
      await updateFuelRecord(vehicleId, recordId, { resetPoint })
      onChanged()
    } catch (caught) {
      setActionError(caught instanceof ApiError ? caught.message : '연비 초기화에 실패했습니다.')
      // 성공 시 부모가 재생성, 실패 시에만 복구
      setPending(false)
    }
  }

  // 껍데기는 항상 렌더. 로딩 중 아래 카드 튐 방지
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>연비</CardTitle>
        {/* 기록이 없으면 초기화 버튼 없음 */}
        {data !== null &&
          data.latestRecordId !== null &&
          (data.resetPointId === null ? (
            <Button
              size="sm"
              variant="ghost"
              disabled={pending}
              onClick={() => {
                // 숫자가 크게 바뀌어 한 번 확인
                if (window.confirm('지금까지의 기록을 연비 계산에서 빼고 다시 셉니다. 계속할까요?')) {
                  void setResetPoint(data.latestRecordId as string, true)
                }
              }}
            >
              {pending ? '처리 중…' : '연비 초기화'}
            </Button>
          ) : (
            <Button
              size="sm"
              variant="ghost"
              disabled={pending}
              onClick={() => void setResetPoint(data.resetPointId as string, false)}
            >
              {pending ? '처리 중…' : '초기화 해제'}
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

        {!loading && (error !== null || data === null) && (
          <ErrorText message={error ?? '주유 요약을 불러오지 못했습니다.'} />
        )}

        {actionError !== null && <ErrorText message={actionError} />}

        {!loading && data !== null && (
          <>
            {data.averageEfficiency === null ? (
              <p className="text-caption leading-relaxed text-muted-foreground">
                {data.resetPointId !== null
                  ? '연비를 초기화했습니다. 다음 주유 기록부터 다시 계산합니다.'
                  : data.recordCount < 2
                    ? '첫 주유 기록은 기준점이 됩니다. 다음 주유 기록부터 연비를 계산합니다.'
                    : /*
                        이유를 하나로 단정하지 않는다. 주행거리가 그대로인 경우만이 아니라
                        주유량을 비운 기록만 있어도 여기로 온다 — 전에는 500km 를 달렸는데
                        "주행거리가 늘어난 기록이 없어" 라고 말했다
                      */
                      '아직 연비를 계산할 수 있는 구간이 없습니다. 주행거리가 늘고 주유량이 적힌 기록이 두 건 이어져야 계산됩니다.'}
              </p>
            ) : (
              <div className="flex items-baseline gap-3" aria-live="polite" aria-atomic="true">
                {/* 히어로 숫자라 tabular-nums 미사용 */}
                <span className="text-display text-strong">{data.averageEfficiency.toFixed(2)}</span>
                <span className="text-muted-foreground">km/L</span>
              </div>
            )}

            {/* 초기화 이후 구간만의 값임을 명시 */}
            {data.resetPointId !== null && data.averageEfficiency !== null && (
              <p className="text-caption text-muted-foreground">연비 초기화 이후 구간만 계산한 값입니다.</p>
            )}

            {/* 불가능한 구간을 뺀 개수 공개. 목록의 확인 필요와 연결 */}
            {data.excludedSegmentCount > 0 && (
              <p className="text-caption leading-relaxed text-muted-foreground">
                계산할 수 없는 구간 {data.excludedSegmentCount}곳을 평균에서 뺐습니다. 목록에서 `확인
                필요` 가 붙은 기록의 주행거리나 주유량을 확인해 주세요.
              </p>
            )}

            {/* 기록 누락 구간을 뺀 개수 공개. 목록 숫자와 평균의 불일치 설명 */}
            {data.longSegmentCount > 0 && (
              <p className="text-caption leading-relaxed text-muted-foreground">
                주유 기록이 빠진 것으로 보이는 구간 {data.longSegmentCount}곳을 평균에서 뺐습니다.
                목록에서 `기록 빠짐?` 이 붙은 구간의 기록을 채워 넣으면 다시 계산됩니다.
              </p>
            )}

            {/* gap-px 격자. 칸 사이 1px */}
            <dl className="grid grid-cols-2 gap-px overflow-hidden border border-border bg-border sm:grid-cols-4">
              <Stat label="기록" value={`${formatNumber(data.recordCount)}건`} />
              <Stat
                label="주행"
                value={data.totalDistance === null ? '—' : formatKm(data.totalDistance)}
              />
              <Stat label="주유량" value={`${data.totalLiters.toFixed(2)} L`} />
              <Stat label="총 유류비" value={formatWon(data.totalCost)} />
            </dl>

            {/* 구간이 둘 미만이면 추이 숨김 */}
            {data.trend.length >= 2 && <EfficiencyTrend trend={data.trend} />}
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
function EfficiencyTrend({ trend }: { trend: FuelSummaryResponse['trend'] }) {
  const values = trend.map((point) => point.efficiency)
  const max = Math.max(...values)
  const floor = Math.min(...values) * 0.9
  const span = Math.max(max - floor, 0.01)

  const average = values.reduce((sum, value) => sum + value, 0) / values.length
  const averageTop = ((max - average) / span) * 100

  return (
    <figure className="flex flex-col gap-2">
      <figcaption className="text-caption text-muted-foreground">
        최근 {trend.length}회 구간 연비
      </figcaption>

      <div className="relative h-20">
        {/* 평균선. 1px 실선 */}
        <div
          aria-hidden="true"
          className="absolute inset-x-0 flex items-center"
          style={{ top: `${averageTop}%` }}
        >
          <div className="h-px flex-1 bg-border-strong" />
          <span className="ml-2 shrink-0 text-unit tabular-nums text-muted-foreground">
            평균 {average.toFixed(1)}
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
                className="w-full bg-fill transition-colors duration-200 ease-apple group-hover:bg-card-hover group-focus-visible:bg-card-hover"
                style={{ height: `${Math.max(((point.efficiency - floor) / span) * 100, 4)}%` }}
              />
              {/* 말풍선과 같은 값이 아래 표에도 */}
              <span className="pointer-events-none absolute -top-1 left-1/2 z-10 -translate-x-1/2 -translate-y-full border border-border bg-card px-2 py-1 text-unit whitespace-nowrap tabular-nums text-strong opacity-0 transition-opacity duration-200 group-hover:opacity-100 group-focus-visible:opacity-100">
                {formatDate(point.fueledAt)} · {point.efficiency.toFixed(2)} km/L
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* 키보드·스크린리더용 값 목록 */}
      <details className="text-caption text-muted-foreground">
        <summary className="cursor-pointer">값으로 보기</summary>
        <ul className="mt-2 flex flex-col gap-1">
          {trend.map((point, index) => (
            <li key={`${point.fueledAt}-${index}-row`} className="tabular-nums">
              {formatDate(point.fueledAt)} · {point.efficiency.toFixed(2)} km/L
            </li>
          ))}
        </ul>
      </details>

      <p className="text-unit text-muted-foreground">
        세로축은 0 부터가 아니라 최솟값 근처에서 시작합니다. 절대량이 아니라 변화를 보는 그림입니다.
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
