import { useCallback } from 'react'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { ErrorText, Skeleton } from '@/shared/ui/feedback/state'
import { formatDate, formatKm } from '@/shared/lib/format/format'
import { useAsyncData } from '@/shared/lib/hooks/useAsyncData'
import { fetchNextServices } from '@/features/maintenance/api/endpoints/endpoints'
import { SERVICE_TYPE_LABELS } from '@/features/maintenance/api/types/types'
import type { NextServiceResponse } from '@/features/maintenance/api/types/types'

/**
 * 다음 정비 시점을 종류별로 보여준다.
 *
 * <p>전에는 종류마다 요청을 보내 Promise.all 로 묶었다(5종 = 5요청). 종류가 15개가 되면서
 * 그 방식을 버리고 서버가 한 번에 돌려주는 /next-services 로 바꿨다 — 요청 1번이다.
 * 덤으로 "5개 중 3개만 뜨는" 부분 실패 상태가 원리적으로 사라졌다.
 *
 * <p><b>이력이 있는 종류만 온다.</b> "다음 정비 시점"은 마지막 정비가 있어야 나오는 값이라,
 * 15줄 중 13줄이 "이력 없음"이면 카드가 빈칸 목록이 된다.
 *
 * 이력이 바뀌면 부모가 key 를 바꿔 이 컴포넌트를 새로 만든다. 그래서 여기엔 재조회 장치가 없다.
 */
export function NextServiceCard({ vehicleId }: { vehicleId: number }) {
  const load = useCallback(() => fetchNextServices(vehicleId), [vehicleId])
  const {
    data: results,
    loading,
    error,
  } = useAsyncData(load, '다음 정비 시점을 불러오지 못했습니다.')

  // 카드 껍데기는 항상 그린다. 상태에 따라 카드가 통째로 사라지면 아래 내용이 위로 튄다.
  return (
    <Card>
      <CardHeader>
        <CardTitle>다음 정비 시점</CardTitle>
        <CardDescription>종류별 권장 주기와 마지막 정비 기록으로 계산합니다.</CardDescription>
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
          <p className="text-[0.8125rem] leading-relaxed text-muted-foreground">
            아직 계산할 이력이 없습니다. 정비 이력을 등록하면 그 종류의 권장 주기로 다음 시점을
            알려 드립니다.
          </p>
        )}

        {!loading && error === null && results !== null && results.length > 0 && (
          // divide-y: 항목마다 테두리를 직접 붙이지 않고 "사이"에만 선을 넣는다.
          // 첫 줄 위와 마지막 줄 아래에 선이 생기지 않아 카드 안쪽이 깔끔하다.
          <ul className="divide-y divide-border">
            {results.map((result) => (
              /*
                넓은 화면에서는 3열(종류 / 마지막 정비 / 다음 정비), 좁으면 2열로 접힌다.
                가운데 "마지막 정비"는 sm 미만에서 숨긴다 — 근거 없이 결과만 보여주는 게
                아니라, 좁은 화면에서는 결론만 남기는 것이다.

                `lastServiceOdometer` 는 백엔드가 계속 내려주고 있었는데 화면이 한 번도
                쓰지 않던 값이다. 열이 하나 늘면서 비로소 자리를 찾았다.
              */
              <li
                key={result.type}
                className="grid grid-cols-[1fr_auto] items-baseline gap-x-6 gap-y-1.5 py-5 first:pt-0 last:pb-0 sm:grid-cols-[8rem_minmax(0,1fr)_auto]"
              >
                <span className="text-[0.9375rem] font-medium tracking-[-0.015em] text-strong">
                  {SERVICE_TYPE_LABELS[result.type]}
                </span>

                <span className="order-3 text-xs tabular-nums text-muted-foreground sm:order-none">
                  {describeLast(result)}
                </span>

                <span className="text-right text-[0.8125rem] tabular-nums text-foreground">
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

/** 근거: 마지막으로 이 정비를 한 시점. */
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

/** 결론: 권장 주기 없음(OTHER) / 정상 계산됨 두 경우.
 *  이력 없는 종류는 서버가 아예 안 보내므로 여기서 다룰 필요가 없다. */
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
