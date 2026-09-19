import { useCallback, useState } from 'react'

import { Button } from '@/shared/ui/base/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { ErrorText, Skeleton } from '@/shared/ui/feedback/state'
import { ApiError } from '@/shared/api/client/client'
import { formatKm, formatNumber, formatWon } from '@/shared/lib/format/format'
import { useAsyncData } from '@/shared/lib/hooks/useAsyncData'
import { fetchFuelSummary, updateFuelRecord } from '@/features/fuel/api/endpoints/endpoints'

/**
 * 평균 연비가 주인공. 이 앱이 주유 기록을 받는 이유가 이 숫자 하나
 * 재조회는 부모가 key 를 바꿔 재생성 (NextServiceCard 와 같은 방식)
 */
export function FuelSummaryCard({
  vehicleId,
  onChanged,
}: {
  vehicleId: number
  /** 기준점이 바뀌면 목록의 구간 연비도 달라지므로 부모에게 알림 */
  onChanged: () => void
}) {
  const load = useCallback(() => fetchFuelSummary(vehicleId), [vehicleId])
  const { data, loading, error } = useAsyncData(load, '주유 요약을 불러오지 못했습니다.')
  const [actionError, setActionError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function setResetPoint(recordId: number, resetPoint: boolean) {
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
                  void setResetPoint(data.latestRecordId as number, true)
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
              onClick={() => void setResetPoint(data.resetPointId as number, false)}
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
                    : '주행거리가 늘어난 기록이 없어 연비를 계산할 수 없습니다.'}
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
              <p className="text-xs text-muted-foreground">연비 초기화 이후 구간만 계산한 값입니다.</p>
            )}

            {/*
              물리적으로 불가능한 구간은 평균에서 뺐다. 말없이 빼면 그것도 거짓말이라 개수를 밝힌다
              목록에서는 그 행에 "확인 필요"가 붙어 있어 어느 기록인지 찾아갈 수 있다
            */}
            {data.excludedSegmentCount > 0 && (
              <p className="text-xs leading-relaxed text-muted-foreground">
                계산할 수 없는 구간 {data.excludedSegmentCount}곳을 평균에서 뺐습니다. 목록에서 `확인
                필요` 가 붙은 기록의 주행거리나 주유량을 확인해 주세요.
              </p>
            )}

            {/*
              기록 누락 가능성. 한 번 안 적으면 그 구간 거리가 두 배가 되고 연비도 두 배가 되어 평균에 섞임
              말해 주면 빠진 기록을 채워 넣게 되고 그러면 저절로 맞아짐
            */}
            {data.longSegmentCount > 0 && (
              <p className="text-xs leading-relaxed text-muted-foreground">
                평소보다 긴 구간이 {data.longSegmentCount}곳 있습니다. 주유 기록이 빠졌다면 채워
                넣으면 연비가 다시 계산됩니다.
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
          </>
        )}
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
