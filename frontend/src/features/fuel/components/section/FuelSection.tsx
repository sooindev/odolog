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
  vehicleId: number
  currentOdometer: number
  /** 기록이 바뀌면 부모에게 알려 연비 요약과 차량 주행거리를 다시 받게 한다. */
  onChanged: () => void
}

export function FuelSection({ vehicleId, currentOdometer, onChanged }: Props) {
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<'closed' | 'new' | FuelRecordResponse>('closed')
  const [actionError, setActionError] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)

  const load = useCallback(() => fetchFuelRecords(vehicleId, page), [vehicleId, page])
  const { data, loading, error, reload } = useAsyncData(load, '주유 기록을 불러오지 못했습니다.')

  // 변수로 받아야 타입이 좁혀진다. JSX 에서 같은 식을 두 번 쓰면 매번 새 식으로 본다.
  const errorMessage = error ?? actionError

  function refresh() {
    setEditing('closed')
    setActionError(null)
    reload()
    onChanged()
  }

  async function handleDelete(recordId: number) {
    if (!window.confirm('이 주유 기록을 삭제할까요? 연비가 다시 계산됩니다.')) {
      return
    }

    setDeletingId(recordId)

    try {
      await deleteFuelRecord(vehicleId, recordId)

      // 이 페이지의 마지막 한 건을 지웠으면 한 장 물러난다. page 만 바꾸면
      // useAsyncData 가 알아서 다시 조회한다 — reload() 까지 부르면 요청이 두 번 나간다.
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
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>주유 기록</CardTitle>
        {editing === 'closed' && (
          <Button size="sm" variant="secondary" onClick={() => setEditing('new')}>
            주유 추가
          </Button>
        )}
      </CardHeader>

      <CardContent className="flex flex-col gap-6">
        {editing !== 'closed' && (
          // 칸이 열리고 폼이 0.12s 늦게 들어온다. 닫을 때는 연출하지 않는다.
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
          <p className="text-[0.8125rem] text-muted-foreground">
            아직 주유 기록이 없습니다. 두 번째 기록부터 연비가 계산됩니다.
          </p>
        ) : (
          <>
            <ul className="border-t border-border">
              {data.items.map((record) => (
                <li key={record.id} className="border-b border-border">
                  {/* 좁은 화면에서는 두 줄로 접는다. 한 줄에 다 넣으면 왼쪽에 100px 남짓만 남는다. */}
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-2 py-4">
                    <div className="flex min-w-0 basis-full flex-col gap-1 sm:basis-auto sm:flex-1">
                      <div className="flex items-baseline gap-2">
                        {/* 연비가 이 행에서 가장 중요한 값이라 맨 앞에 둔다. */}
                        {record.efficiency === null ? (
                          <span className="text-muted-foreground">연비 —</span>
                        ) : (
                          <span className="text-figure tabular-nums text-strong">
                            {record.efficiency.toFixed(2)}
                            <span className="ml-1 text-sm text-muted-foreground">km/L</span>
                          </span>
                        )}
                      </div>
                      <span className="text-[0.8125rem] text-muted-foreground">
                        {formatDate(record.fueledAt)} · {formatKm(record.odometer)}
                        {record.distance !== null && ` · +${formatKm(record.distance)}`}
                      </span>
                      {record.memo !== null && record.memo !== '' && (
                        <span className="truncate text-[0.8125rem] text-muted-foreground">
                          {record.memo}
                        </span>
                      )}
                    </div>

                    <div className="flex flex-col items-end gap-1 tabular-nums">
                      <span className="text-strong">{record.liters.toFixed(2)} L</span>
                      <span className="text-[0.8125rem] text-muted-foreground">
                        {formatWon(record.totalCost)}
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
              onChange={setPage}
            />
          </>
        )}
      </CardContent>
    </Card>
  )
}
