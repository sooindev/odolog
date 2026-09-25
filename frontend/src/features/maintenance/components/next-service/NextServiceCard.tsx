import { useCallback } from 'react'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { ErrorText, Skeleton } from '@/shared/ui/feedback/state'
import { formatDate, formatKm } from '@/shared/lib/format/format'
import { useAsyncData } from '@/shared/lib/hooks/useAsyncData'
import { fetchNextServices } from '@/features/maintenance/api/endpoints/endpoints'
import { SERVICE_TYPE_LABELS } from '@/features/maintenance/api/types/types'
import type { NextServiceResponse } from '@/features/maintenance/api/types/types'

/**
 * 종류별 다음 정비 시점. 요청 1번
 * 종류마다 요청하던 방식(15요청)을 버리며 "일부만 뜨는" 부분 실패 상태도 사라짐
 * 이력 있는 종류만 옴 — 15줄 중 13줄이 "이력 없음"이면 빈칸 목록이 됨
 * 재조회 장치가 없는 이유 — 부모가 key 를 바꿔 새로 만듦
 */
export function NextServiceCard({ vehicleId }: { vehicleId: number }) {
  const load = useCallback(() => fetchNextServices(vehicleId), [vehicleId])
  const {
    data: results,
    loading,
    error,
  } = useAsyncData(load, '다음 정비 시점을 불러오지 못했습니다.')

  // 껍데기는 항상 렌더. 카드가 통째로 사라지면 아래 내용이 위로 튐
  return (
    <Card>
      <CardHeader>
        <CardTitle>다음 정비 시점</CardTitle>
        <CardDescription>
          종류별 권장 주기와 마지막 정비 기록으로 계산합니다. 지난 것이 위에 옵니다.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {loading && (
          <div className="flex flex-col gap-5">
            {[0, 1, 2].map((row) => (
              <Skeleton key={row} className="h-4" />
            ))}
          </div>
        )}

        {!loading && error !== null && <ErrorText message={error} />}

        {!loading && error === null && results !== null && results.length === 0 && (
          <p className="text-caption leading-relaxed text-muted-foreground">
            아직 계산할 이력이 없습니다. 정비 이력을 등록하면 그 종류의 권장 주기로 다음 시점을
            알려 드립니다.
          </p>
        )}

        {!loading && error === null && results !== null && results.length > 0 && (
          // divide-y — 항목마다 테두리를 붙이지 않고 "사이"에만. 첫 줄 위·마지막 줄 아래에 선이 안 생김
          <ul className="divide-y divide-border">
            {results.map((result) => (
              /*
                넓은 화면 3열(종류 / 마지막 정비 / 다음 정비), 좁으면 2열
                가운데 "마지막 정비"는 sm 미만에서 숨김 — 좁은 화면에서는 결론만
                lastServiceOdometer 는 백엔드가 계속 주고 있었으나 화면이 안 쓰던 값
              */
              <li
                key={result.type}
                className="grid grid-cols-[1fr_auto] items-baseline gap-x-6 gap-y-1.5 py-5 first:pt-0 last:pb-0 sm:grid-cols-[8rem_minmax(0,1fr)_auto]"
              >
                <span className="flex min-w-0 items-baseline gap-2">
                  <span className="truncate text-body font-medium tracking-[-0.015em] text-strong">
                    {SERVICE_TYPE_LABELS[result.type]}
                  </span>
                  {/*
                    빨강을 쓰지 않는다. 빨강은 "실패" 를 나르는 기능색이고(디자인 규칙 3)
                    주유 목록의 `확인 필요`(입력 오류)가 이미 그 뜻으로 쓰고 있다.
                    정비 시기가 지난 것은 잘못이 아니라 할 일이라, 모노톤 시스템의 방식대로
                    대비를 올려서 말한다 — 테두리 친 라벨 + 아래 값도 strong 으로
                  */}
                  {result.overdue && (
                    <span className="shrink-0 border border-strong/30 px-1.5 py-0.5 text-unit font-medium text-strong">
                      지남
                    </span>
                  )}
                </span>

                <span className="order-3 text-caption tabular-nums text-muted-foreground sm:order-none">
                  {describeLast(result)}
                </span>

                <span
                  className={`text-right text-caption tabular-nums ${
                    result.overdue ? 'font-medium text-strong' : 'text-foreground'
                  }`}
                >
                  {describeNext(result)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

/** 근거 — 마지막으로 이 정비를 한 시점 */
function describeLast(result: NextServiceResponse) {
  if (result.lastServiceDate === null) {
    return ''
  }

  const parts = [formatDate(result.lastServiceDate)]
  if (result.lastServiceOdometer !== null) {
    parts.push(formatKm(result.lastServiceOdometer))
  }

  return `마지막 ${parts.join(' · ')}`
}

/** 결론 — 권장 주기 없음(OTHER) / 정상 계산 두 경우
 *  이력 없는 종류는 서버가 안 보내므로 여기서 다룰 필요 없음 */
function describeNext(result: NextServiceResponse) {
  const parts: string[] = []
  if (result.nextServiceOdometer !== null) {
    parts.push(formatKm(result.nextServiceOdometer))
  }
  if (result.nextServiceDate !== null) {
    parts.push(formatDate(result.nextServiceDate))
  }

  if (parts.length === 0) {
    return '권장 주기 없음'
  }

  return parts.join(' 또는 ')
}
