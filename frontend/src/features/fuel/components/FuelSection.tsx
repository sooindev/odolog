import { useState } from 'react'
import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query'

import { FuelForm } from '@/features/fuel/components/FuelForm'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { Pagination } from '@/shared/ui/pagination'
import { ErrorText, Skeleton } from '@/shared/ui/state'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import { usePageInRange } from '@/shared/lib/hooks/usePageInRange'
import { useRecordList } from '@/shared/lib/hooks/useRecordList'
import { invalidateAfterFuel, queryKeys } from '@/shared/api/queryKeys'
import { deleteFuelRecord, fetchFuelRecords } from '@/features/fuel/api/endpoints'
import type { FuelRecordResponse } from '@/features/fuel/api/types'

interface Props {
  vehicleId: string
  currentOdometer: number
  /** 기록 변경 시 부모에 알림. 차량 주행거리 409 문구 정리 */
  onChanged: () => void
}

export function FuelSection({ vehicleId, currentOdometer, onChanged }: Props) {
  const { t, f } = useI18n()
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)

  // 페이지를 옮기는 동안 직전 목록 유지. 깜빡임 방지
  const { data, isPending, error } = useQuery({
    queryKey: queryKeys.fuelList(vehicleId, page),
    queryFn: () => fetchFuelRecords(vehicleId, page),
    placeholderData: keepPreviousData,
  })
  usePageInRange(data ?? null, setPage)

  // 삭제 시 다음 기록 연비 상승 안내. 구간이 적으면 서버가 못 잡는 경우 대비
  const list = useRecordList<FuelRecordResponse>({
    setPage,
    remove: (recordId) => deleteFuelRecord(vehicleId, recordId),
    confirmMessage: t.fuel.deleteConfirm,
    failedMessage: t.fuel.deleteFailed,
    afterChange: () => {
      onChanged()
      return invalidateAfterFuel(queryClient, vehicleId)
    },
  })
  const { editing, setEditing, deletingId } = list

  // 변수로 받아 타입 좁히기
  const shownError = error !== null ? errorMessage(error, t, t.fuel.loadFailed) : list.actionError

  // 전체의 첫 기록. 주행거리 내림차순이라 마지막 장의 마지막 행
  function isFirstRecord(index: number) {
    return data !== undefined && !data.hasNext && index === data.items.length - 1
  }

  return (
    <Card>
      {/* 좁은 화면은 줄바꿈으로 접기 */}
      <CardHeader className="flex flex-row flex-wrap items-center justify-between gap-y-3">
        <CardTitle className="min-w-0">{t.fuel.title}</CardTitle>
        {editing === 'closed' && (
          <Button size="sm" variant="secondary" onClick={() => setEditing('new')}>
            {t.fuel.add}
          </Button>
        )}
      </CardHeader>

      <CardContent className="flex flex-col gap-6">
        {editing !== 'closed' && (
          // 펼침 연출(폼 0.12s 지연). 닫을 때는 없음
          <div className="form-open">
            <div>
              {/* key 필수. 폼이 열린 채 다른 행을 고르면 이전 입력이 새 기록에 덮어써짐 */}
              <FuelForm
                key={editing === 'new' ? 'new' : editing.id}
                vehicleId={vehicleId}
                record={editing === 'new' ? null : editing}
                defaultOdometer={currentOdometer}
                onSaved={() => void list.saved(editing === 'new')}
                onCancel={() => setEditing('closed')}
              />
            </div>
          </div>
        )}

        {shownError !== null && <ErrorText message={shownError} />}

        {isPending ? (
          <div className="flex flex-col gap-4">
            {[0, 1, 2].map((row) => (
              <Skeleton key={row} className="h-12" />
            ))}
          </div>
        ) : data === undefined || data.items.length === 0 ? (
          <p className="text-caption text-muted-foreground">{t.fuel.empty}</p>
        ) : (
          <>
            <ul className="border-t border-border">
              {data.items.map((record, index) => (
                <li key={record.id} className="border-b border-border">
                  {/* 좁은 화면은 두 줄 */}
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-2 py-4">
                    <div className="flex min-w-0 basis-full flex-col gap-1 sm:basis-auto sm:flex-1">
                      <div className="flex items-baseline gap-2">
                        {/* 연비를 맨 앞에 */}
                        {record.efficiency === null ? (
                          // 연비가 없는 이유별 문구. 주유량 없음 / 기준점·첫 기록 / 직전과 같은 주행거리
                          <span className="text-caption text-muted-foreground">
                            {record.distance !== null && record.liters === null ? (
                              <>
                                {t.fuel.noLiters}
                                <span className="ml-1 text-muted-foreground">{t.fuel.noEfficiency}</span>
                              </>
                            ) : record.resetPoint || isFirstRecord(index) ? (
                              <>
                                {record.resetPoint ? t.fuel.resetPoint : t.fuel.baseline}
                                {/* 읽어야 하는 문구라 faint 미사용 */}
                                <span className="ml-1 text-muted-foreground">{t.fuel.fromNext}</span>
                              </>
                            ) : (
                              // 직전 기록과 주행거리가 같아 구간 없음
                              <>
                                {t.fuel.sameOdometer}
                                <span className="ml-1 text-muted-foreground">{t.fuel.noEfficiency}</span>
                              </>
                            )}
                          </span>
                        ) : (
                          <span className="flex items-baseline gap-1.5">
                            <span className="text-figure tabular-nums text-strong">
                              {f.efficiencyNumber(record.efficiency)}
                              <span className="ml-1 text-caption text-muted-foreground">{f.efficiencyUnit}</span>
                            </span>
                            {/* 불가능한 값. 숫자 유지 + 확인 필요 */}
                            {record.efficiencySuspicious && (
                              <span className="text-unit text-destructive">{t.fuel.suspicious}</span>
                            )}
                            {/* 기록 누락 구간. 잘못이 아닌 빈자리라 회색 */}
                            {record.missingRecordSuspected && (
                              <span className="text-unit text-muted-foreground">{t.fuel.missing}</span>
                            )}
                          </span>
                        )}
                      </div>
                      <span className="text-caption text-muted-foreground">
                        {f.date(record.fueledAt)} · {f.distance(record.odometer)}
                        {record.distance !== null && ` · +${f.distance(record.distance)}`}
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
                        {record.liters === null ? `— ${f.volumeUnit}` : f.volume(record.liters)}
                      </span>
                      <span className="text-caption text-muted-foreground">
                        {record.totalCost === null ? '—' : f.money(record.totalCost, record.currency)}
                      </span>
                    </div>

                    <div className="flex shrink-0 gap-1">
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => setEditing(record)}
                        disabled={deletingId === record.id}
                      >
                        {t.common.edit}
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => list.handleDelete(record.id)}
                        disabled={deletingId === record.id}
                      >
                        {deletingId === record.id ? t.common.deleting : t.common.delete}
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
              onChange={list.changePage}
            />
          </>
        )}
      </CardContent>
    </Card>
  )
}
