import { useCallback, useState } from 'react'

import { FuelForm } from '@/features/fuel/components/form/FuelForm'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { Pagination } from '@/shared/ui/nav/pagination'
import { ErrorText, Skeleton } from '@/shared/ui/feedback/state'
import { ApiError } from '@/shared/api/client/client'
import { formatDate, formatKm, formatWon } from '@/shared/lib/format/format'
import { useAsyncData } from '@/shared/lib/hooks/useAsyncData'
import { deleteFuelRecord, fetchFuelRecords } from '@/features/fuel/api/endpoints/endpoints'
import type { FuelRecordResponse } from '@/features/fuel/api/types/types'

interface Props {
  vehicleId: string
  currentOdometer: number
  /** 기록이 바뀌면 부모에게 알려 연비 요약과 차량 주행거리 재조회 */
  onChanged: () => void
}

export function FuelSection({ vehicleId, currentOdometer, onChanged }: Props) {
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<'closed' | 'new' | FuelRecordResponse>('closed')
  const [actionError, setActionError] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const load = useCallback(() => fetchFuelRecords(vehicleId, page), [vehicleId, page])
  const { data, loading, error, reload } = useAsyncData(load, '주유 기록을 불러오지 못했습니다.')

  // 변수로 받아야 타입이 좁혀짐. JSX 에서 같은 식을 두 번 쓰면 매번 새 식
  const errorMessage = error ?? actionError

  function refresh() {
    setEditing('closed')
    setActionError(null)
    reload()
    onChanged()
  }

  async function handleDelete(recordId: string) {
    // 지우면 다음 기록의 구간이 그만큼 길어지는데 주유량은 안 늘어 연비가 뜬다.
    // 구간이 셋 미만이면 '평소'가 없어 서버가 그걸 못 잡으므로, 여기서 미리 말한다
    if (
      !window.confirm(
        '이 주유 기록을 삭제할까요?\n\n' +
          '지운 기록의 주유량이 함께 사라져 다음 기록의 연비가 실제보다 높게 나옵니다.',
      )
    ) {
      return
    }

    setDeletingId(recordId)

    try {
      await deleteFuelRecord(vehicleId, recordId)

      // 이 페이지의 마지막 한 건이었으면 한 장 뒤로
      // page 만 바꾸면 useAsyncData 가 재조회 — reload() 까지 부르면 두 번 나감
      if (data !== null && data.items.length === 1 && page > 0) {
        setEditing('closed')
        setActionError(null)
        setPage((current) => current - 1)
        onChanged()
      } else {
        refresh()
      }
    } catch (caught) {
      setActionError(caught instanceof ApiError ? caught.message : '삭제에 실패했습니다.')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <Card>
      {/* 375px 에서 제목 + 필터 + 버튼이 한 줄에 안 들어간다. 줄여서 맞추지 않고 접는다 */}
      <CardHeader className="flex flex-row flex-wrap items-center justify-between gap-y-3">
        <CardTitle className="min-w-0">주유 기록</CardTitle>
        {editing === 'closed' && (
          <Button size="sm" variant="secondary" onClick={() => setEditing('new')}>
            주유 추가
          </Button>
        )}
      </CardHeader>

      <CardContent className="flex flex-col gap-6">
        {editing !== 'closed' && (
          // 칸이 열리고 폼은 0.12s 지연. 닫을 때는 연출 없음
          <div className="form-open">
            <div>
              <FuelForm
                vehicleId={vehicleId}
                record={editing === 'new' ? null : editing}
                defaultOdometer={currentOdometer}
                onSaved={refresh}
                onCancel={() => setEditing('closed')}
              />
            </div>
          </div>
        )}

        {errorMessage !== null && <ErrorText message={errorMessage} />}

        {loading ? (
          <div className="flex flex-col gap-4">
            {[0, 1, 2].map((row) => (
              <Skeleton key={row} className="h-12" />
            ))}
          </div>
        ) : data === null || data.items.length === 0 ? (
          <p className="text-caption text-muted-foreground">
            아직 주유 기록이 없습니다. 두 번째 기록부터 연비가 계산됩니다.
          </p>
        ) : (
          <>
            <ul className="border-t border-border">
              {data.items.map((record) => (
                <li key={record.id} className="border-b border-border">
                  {/* 좁은 화면에서는 두 줄로. 한 줄이면 왼쪽에 100px 남짓만 남음 */}
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-2 py-4">
                    <div className="flex min-w-0 basis-full flex-col gap-1 sm:basis-auto sm:flex-1">
                      <div className="flex items-baseline gap-2">
                        {/* 이 행에서 가장 중요한 값이라 맨 앞 */}
                        {record.efficiency === null ? (
                          /*
                            연비가 없는 이유가 셋이라 문구도 셋이다. "—" 만 두면 왜 없는지 알 수 없다.

                            주유량만 없는 경우를 distance 로 가려낸다 — 기준점이거나 직전 기록이
                            없으면 서버가 distance 도 null 로 주므로, distance 가 있는데 연비가
                            없다는 것은 "달린 거리는 아는데 얼마나 넣었는지 모른다" 뿐이다.
                            여기서 "기준 기록" 이라고 적으면 거짓말이 된다 — 첫 기록도 아니고
                            다음 주유부터 계산되는 것도 아니다(바로 다음 기록은 이미 나온다).
                          */
                          <span className="text-caption text-muted-foreground">
                            {record.distance !== null && record.liters === null ? (
                              <>
                                주유량 없음
                                <span className="ml-1 text-muted-foreground">· 연비 계산 안 됨</span>
                              </>
                            ) : (
                              <>
                                {record.resetPoint ? '연비 기준점' : '기준 기록'}
                                {/* 연비가 없는 이유를 말하는 문구라 faint 를 쓰지 않는다 — 대비 3.2 라 읽어야 하는 값에는 못 쓴다 */}
                                <span className="ml-1 text-muted-foreground">· 다음 주유부터 계산</span>
                              </>
                            )}
                          </span>
                        ) : (
                          <span className="flex items-baseline gap-1.5">
                            <span className="text-figure tabular-nums text-strong">
                              {record.efficiency.toFixed(2)}
                              <span className="ml-1 text-caption text-muted-foreground">km/L</span>
                            </span>
                            {/* 물리적으로 불가능한 값. 숫자를 지우지 않고 옆에 붙임 —
                                무엇을 잘못 적었는지 보려면 그 값이 남아 있어야 함 */}
                            {record.efficiencySuspicious && (
                              <span className="text-unit text-destructive">확인 필요</span>
                            )}
                            {/* 기록이 빠진(또는 지운) 구간. 빨강을 쓰지 않는다 —
                                빨강은 "실패"를 나르는 기능색이고 이건 잘못이 아니라 빈자리다 */}
                            {record.missingRecordSuspected && (
                              <span className="text-unit text-muted-foreground">기록 빠짐?</span>
                            )}
                          </span>
                        )}
                      </div>
                      <span className="text-caption text-muted-foreground">
                        {formatDate(record.fueledAt)} · {formatKm(record.odometer)}
                        {record.distance !== null && ` · +${formatKm(record.distance)}`}
                      </span>
                      {record.memo !== null && record.memo !== '' && (
                        <span className="truncate text-caption text-muted-foreground">
                          {record.memo}
                        </span>
                      )}
                    </div>

                    <div className="flex flex-col items-end gap-1 tabular-nums">
                      {/* 안 적은 값은 "0.00 L" 이 아니라 "—" 다. 0 을 찍으면 적은 값처럼 보인다 */}
                      <span className="text-strong">
                        {record.liters === null ? '— L' : `${record.liters.toFixed(2)} L`}
                      </span>
                      <span className="text-caption text-muted-foreground">
                        {record.totalCost === null ? '— 원' : formatWon(record.totalCost)}
                      </span>
                    </div>

                    <div className="flex shrink-0 gap-1">
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => setEditing(record)}
                        disabled={deletingId === record.id}
                      >
                        수정
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => handleDelete(record.id)}
                        disabled={deletingId === record.id}
                      >
                        {deletingId === record.id ? '삭제 중…' : '삭제'}
                      </Button>
                    </div>
                  </div>
                </li>
              ))}
            </ul>

            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              hasNext={data.hasNext}
              /* 수정 중인 행이 이 페이지에 없어지므로 폼을 닫는다. 이전 에러도 함께 지운다 */
              onChange={(next) => {
                setPage(next)
                setEditing('closed')
                setActionError(null)
              }}
            />
          </>
        )}
      </CardContent>
    </Card>
  )
}
