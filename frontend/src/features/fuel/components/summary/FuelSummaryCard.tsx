import { useCallback, useState } from 'react'

import { Button } from '@/shared/ui/base/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { ErrorText, Skeleton } from '@/shared/ui/feedback/state'
import { ApiError } from '@/shared/api/client/client'
import { formatDate, formatKm, formatNumber, formatWon } from '@/shared/lib/format/format'
import { useAsyncData } from '@/shared/lib/hooks/useAsyncData'
import { fetchFuelSummary, updateFuelRecord } from '@/features/fuel/api/endpoints/endpoints'
import type { FuelSummaryResponse } from '@/features/fuel/api/types/types'

/**
 * 평균 연비가 주인공. 이 앱이 주유 기록을 받는 이유가 이 숫자 하나
 * 재조회는 부모가 key 를 바꿔 재생성 (NextServiceCard 와 같은 방식)
 */
export function FuelSummaryCard({
  vehicleId,
  onChanged,
}: {
  vehicleId: string
  /** 기준점이 바뀌면 목록의 구간 연비도 달라지므로 부모에게 알림 */
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
      // 성공하면 부모가 새로 만드므로 실패했을 때만 복구
      setPending(false)
    }
  }

  // 껍데기는 항상 렌더. 이 카드는 key 로 재생성되므로 기록을 저장할 때마다 로딩을 거치는데,
  // 카드를 통째로 없애면 그때마다 아래 주유 기록 카드가 위로 튀었다 내려온다 (NextServiceCard 와 같은 이유)
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>연비</CardTitle>
        {/* 기록이 없으면 초기화할 것도 없음. 눌러도 아무 일 없는 버튼을 두지 않음 */}
        {data !== null &&
          data.latestRecordId !== null &&
          (data.resetPointId === null ? (
            <Button
              size="sm"
              variant="ghost"
              disabled={pending}
              onClick={() => {
                // 되돌릴 수 있지만(해제 버튼) 숫자가 크게 바뀌므로 한 번 확인
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
                {/* 히어로 숫자에는 tabular-nums 금지 — 세로로 줄 맞출 상대가 없음 */}
                <span className="text-display text-strong">{data.averageEfficiency.toFixed(2)}</span>
                <span className="text-muted-foreground">km/L</span>
              </div>
            )}

            {/* 초기화 이후 구간만 센 값임을 명시. 안 적으면 전체 평균으로 오해함 */}
            {data.resetPointId !== null && data.averageEfficiency !== null && (
              <p className="text-caption text-muted-foreground">연비 초기화 이후 구간만 계산한 값입니다.</p>
            )}

            {/*
              물리적으로 불가능한 구간은 평균에서 뺐다. 말없이 빼면 그것도 거짓말이라 개수를 밝힌다
              목록에서는 그 행에 "확인 필요"가 붙어 있어 어느 기록인지 찾아갈 수 있다
            */}
            {data.excludedSegmentCount > 0 && (
              <p className="text-caption leading-relaxed text-muted-foreground">
                계산할 수 없는 구간 {data.excludedSegmentCount}곳을 평균에서 뺐습니다. 목록에서 `확인
                필요` 가 붙은 기록의 주행거리나 주유량을 확인해 주세요.
              </p>
            )}

            {/*
              기록 누락 가능성. 한 번 안 적거나 지우면 그 구간 거리가 두 배가 되고 연비도 두 배가 됨
              평균에서 빼 두므로 여기서도 뺐다고 말해야 함 — 목록의 숫자와 평균이 안 맞아 보이기 때문
            */}
            {data.longSegmentCount > 0 && (
              <p className="text-caption leading-relaxed text-muted-foreground">
                주유 기록이 빠진 것으로 보이는 구간 {data.longSegmentCount}곳을 평균에서 뺐습니다.
                목록에서 `기록 빠짐?` 이 붙은 구간의 기록을 채워 넣으면 다시 계산됩니다.
              </p>
            )}

            {/* 격자 사이로 부모의 선 색이 비침. 칸마다 border 면 맞닿는 자리가 2px */}
            <dl className="grid grid-cols-2 gap-px overflow-hidden border border-border bg-border sm:grid-cols-4">
              <Stat label="기록" value={`${formatNumber(data.recordCount)}건`} />
              <Stat
                label="주행"
                value={data.totalDistance === null ? '—' : formatKm(data.totalDistance)}
              />
              <Stat label="주유량" value={`${data.totalLiters.toFixed(2)} L`} />
              <Stat label="총 유류비" value={formatWon(data.totalCost)} />
            </dl>

            {/* 평균 하나로는 추세를 알 수 없다. 구간이 둘 미만이면 그릴 것이 없어 숨긴다 */}
            {data.trend.length >= 2 && <EfficiencyTrend trend={data.trend} />}
          </>
        )}
      </CardContent>
    </Card>
  )
}

/**
 * 최근 구간 연비. 라이브러리 없이 HTML·CSS 로만 (홈 차트와 같은 방식)
 *
 * 계열이 하나라 색이 구분할 것이 없고 그래서 범례도 없다.
 * 눈금선은 평균 한 줄뿐 — 이 차트가 답하는 질문은 "지금 평균보다 나은가" 하나다.
 * 축 눈금을 촘촘히 깔면 카드 안의 작은 그림이 홈의 큰 차트와 경쟁한다
 *
 * 세로축을 0 이 아니라 **최솟값의 90%** 에서 시작한다. 0 부터 그리면 12~14km/L 같은
 * 실제 차이가 막대 끝 몇 px 로 뭉개져 추세가 안 보인다 — 여기서 보려는 것은 절대량이 아니라 변화다.
 * 대신 축이 0 이 아니라는 것을 아래에 적는다
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
        {/* 평균선. 1px 실선 — 점선은 "예측"이나 "임계선"으로 읽힌다 */}
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
              // 칸 전체가 판정 영역. 막대만이면 얇아서 못 맞춘다
              className="group relative flex h-full flex-1 items-end"
              tabIndex={0}
            >
              <div
                className="w-full bg-fill transition-colors duration-200 ease-apple group-hover:bg-card-hover group-focus-visible:bg-card-hover"
                style={{ height: `${Math.max(((point.efficiency - floor) / span) * 100, 4)}%` }}
              />
              {/* 말풍선은 정보를 보태는 장치다. 아래 표가 같은 값을 키보드로도 준다 */}
              <span className="pointer-events-none absolute -top-1 left-1/2 z-10 -translate-x-1/2 -translate-y-full border border-border bg-card px-2 py-1 text-unit whitespace-nowrap tabular-nums text-strong opacity-0 transition-opacity duration-200 group-hover:opacity-100 group-focus-visible:opacity-100">
                {formatDate(point.fueledAt)} · {point.efficiency.toFixed(2)} km/L
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* 마우스를 올려야만 보이는 값을 만들지 않는다 — 키보드·스크린리더 몫 */}
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
