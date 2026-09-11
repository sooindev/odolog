import { useCallback } from 'react'

import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { ErrorText, Skeleton } from '@/shared/ui/state'
import { formatDate, formatKm } from '@/shared/lib/format'
import { useAsyncData } from '@/shared/lib/useAsyncData'
import { fetchNextService } from '@/features/maintenance/api/endpoints'
import { SERVICE_TYPES, SERVICE_TYPE_LABELS } from '@/features/maintenance/api/types'
import type { NextServiceResponse } from '@/features/maintenance/api/types'

/**
 * 다음 정비 시점을 종류별로 보여준다.
 * 백엔드 API가 종류 하나씩만 계산하므로 요청이 종류 수만큼 나간다.
 * 실제로 느려지면 "전체 종류 한 번에" API 추가를 검토한다 (CLAUDE.md 백로그).
 *
 * 이력이 바뀌면 부모가 key 를 바꿔 이 컴포넌트를 새로 만든다. 그래서 여기엔 재조회 장치가 없다.
 */
export function NextServiceCard({ vehicleId }: { vehicleId: number }) {
  // Promise.all: 5개 요청을 순서대로 기다리지 않고 동시에 보낸다.
  // 하나만 실패해도 전체가 실패한다. 부분 성공을 보여줄 수도 있지만,
  // 5개 중 3개만 뜨는 화면이 더 헷갈려서 통째로 에러로 처리한다.
  const load = useCallback(
    () => Promise.all(SERVICE_TYPES.map((type) => fetchNextService(vehicleId, type))),
    [vehicleId],
  )
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
      </CardHeader>
      <CardContent>
        {loading && (
          <div className="flex flex-col gap-4">
            {SERVICE_TYPES.map((type) => (
              <Skeleton key={type} className="h-4" />
            ))}
          </div>
        )}

        {!loading && error !== null && <ErrorText message={error} />}

        {!loading && error === null && results !== null && (
          // divide-y: 항목마다 테두리를 직접 붙이지 않고 "사이"에만 선을 넣는다.
          // 첫 줄 위와 마지막 줄 아래에 선이 생기지 않아 카드 안쪽이 깔끔하다.
          <ul className="divide-y divide-border">
            {results.map((result) => (
              <li
                key={result.type}
                className="flex items-baseline justify-between gap-4 py-3 first:pt-0 last:pb-0"
              >
                <span className="text-[0.9375rem] tracking-[-0.01em] text-strong">
                  {SERVICE_TYPE_LABELS[result.type]}
                </span>
                <span className="text-right text-[0.8125rem] tabular-nums text-muted-foreground">
                  {describe(result)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

/** 이력 없음 / 권장 주기 없음(OTHER) / 정상 계산됨 세 경우를 문장으로 만든다. */
function describe(result: NextServiceResponse) {
  if (result.lastServiceDate === null) {
    return '이력 없음'
  }

  const parts: string[] = []
  if (result.nextServiceOdometer !== null) {
    parts.push(formatKm(result.nextServiceOdometer))
  }
  if (result.nextServiceDate !== null) {
    parts.push(formatDate(result.nextServiceDate))
  }

  if (parts.length === 0) {
    return `${formatDate(result.lastServiceDate)} 정비 · 권장 주기 없음`
  }

  return parts.join(' 또는 ')
}
