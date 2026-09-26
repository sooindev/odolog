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
  /** 기록 변경 시 부모에 알림. 연비 요약·차량 주행거리 재조회 */
  onChanged: () => void
}

export function FuelSection({ vehicleId, currentOdometer, onChanged }: Props) {
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<'closed' | 'new' | FuelRecordResponse>('closed')
  const [actionError, setActionError] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const load = useCallback(() => fetchFuelRecords(vehicleId, page), [vehicleId, page])
  const { data, loading, error, reload } = useAsyncData(load, '주유 기록을 불러오지 못했습니다.')

  // 변수로 받아 타입 좁히기
  const errorMessage = error ?? actionError

  function refresh() {
    setEditing('closed')
    setActionError(null)
    reload()
    onChanged()
  }

  async function handleDelete(recordId: string) {
    // 삭제 시 다음 기록 연비 상승 안내. 구간이 적으면 서버가 못 잡는 경우 대비
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

      // 페이지의 마지막 한 건이면 한 장 뒤로. page 변경만으로 재조회
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
      {/* 좁은 화면은 줄바꿈으로 접기 */}
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
          // 펼침 연출(폼 0.12s 지연). 닫을 때는 없음
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
                  {/* 좁은 화면은 두 줄 */}
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-2 py-4">
                    <div className="flex min-w-0 basis-full flex-col gap-1 sm:basis-auto sm:flex-1">
                      <div className="flex items-baseline gap-2">
                        {/* 연비를 맨 앞에 */}
                        {record.efficiency === null ? (
                          // 연비가 없는 이유별 문구
                          // distance 는 있고 liters 가 없으면 주유량 없음, 그 외는 기준 기록·기준점
                          <span className="text-caption text-muted-foreground">
                            {record.distance !== null && record.liters === null ? (
                              <>
                                주유량 없음
                                <span className="ml-1 text-muted-foreground">· 연비 계산 안 됨</span>
                              </>
                            ) : (
                              <>
                                {record.resetPoint ? '연비 기준점' : '기준 기록'}
                                {/* 읽어야 하는 문구라 faint 미사용 */}
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
                            {/* 불가능한 값. 숫자 유지 + 확인 필요 */}
                            {record.efficiencySuspicious && (
                              <span className="text-unit text-destructive">확인 필요</span>
                            )}
                            {/* 기록 누락 구간. 잘못이 아닌 빈자리라 회색 */}
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
                      {/* 안 적은 값은 — */}
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
              // 페이지 이동 시 수정 폼·에러 초기화
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
