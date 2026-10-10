import { useCallback } from 'react'
import type { Dispatch, SetStateAction } from 'react'
import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query'

import { MaintenanceForm } from '@/features/maintenance/components/MaintenanceForm'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { Pagination } from '@/shared/ui/pagination'
import { ErrorText, Skeleton } from '@/shared/ui/state'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import { usePageInRange } from '@/shared/lib/hooks/usePageInRange'
import { useRecordList } from '@/shared/lib/hooks/useRecordList'
import { invalidateAfterMaintenance, queryKeys } from '@/shared/api/queryKeys'
import { deleteRecord, fetchRecords } from '@/features/maintenance/api/endpoints'
import { SERVICE_TYPES } from '@/features/maintenance/api/types'
import { controlClassName } from '@/shared/ui/form/control'
import { cn } from '@/shared/ui/cn'
import type { MaintenanceRecordResponse, ServiceType } from '@/features/maintenance/api/types'
import { ALL_MAINTENANCE } from '@/features/maintenance/components/maintenanceView'
import type { MaintenanceView } from '@/features/maintenance/components/maintenanceView'

interface Props {
  vehicleId: string
  currentOdometer: number
  /** 페이지·필터. 부모가 쥐고 있어 다른 카드의 저장에도 열린 폼은 그대로 */
  view: MaintenanceView
  onViewChange: Dispatch<SetStateAction<MaintenanceView>>
  /** 이력 변경 시 부모에 알림. 차량 주행거리 409 문구 정리 */
  onChanged: () => void
}

export function MaintenanceSection({ vehicleId, currentOdometer, view, onViewChange, onChanged }: Props) {
  const { t, f } = useI18n()
  const queryClient = useQueryClient()
  const { page, filter } = view

  const setPage = useCallback<Dispatch<SetStateAction<number>>>(
    (next) =>
      onViewChange((current) => ({
        ...current,
        page: typeof next === 'function' ? next(current.page) : next,
      })),
    [onViewChange],
  )

  // 페이지·필터를 옮기는 동안 직전 목록 유지. 깜빡임 방지
  const { data, isPending, error } = useQuery({
    queryKey: queryKeys.maintenanceList(vehicleId, page, filter),
    queryFn: () => fetchRecords(vehicleId, page, filter),
    placeholderData: keepPreviousData,
  })
  usePageInRange(data ?? null, setPage)

  const list = useRecordList<MaintenanceRecordResponse>({
    setPage,
    remove: (recordId) => deleteRecord(vehicleId, recordId),
    confirmMessage: t.maintenance.deleteConfirm,
    failedMessage: t.maintenance.deleteFailed,
    afterChange: () => {
      onChanged()
      return invalidateAfterMaintenance(queryClient, vehicleId)
    },
  })
  const { editing, setEditing, deletingId } = list

  // 변수로 받아 타입 좁히기
  const shownError = error !== null ? errorMessage(error, t, t.maintenance.loadFailed) : list.actionError

  /**
   * 방금 저장한 종류
   * 필터 밖 종류로 저장했으면 필터 해제. 저장 실패로 오해해 중복 등록하는 문제 방지
   */
  function handleSaved(savedType: ServiceType, created: boolean) {
    if (filter !== null && filter !== savedType) {
      onViewChange(ALL_MAINTENANCE)
    }
    void list.saved(created)
  }

  return (
    <Card>
      {/* 좁은 화면은 줄바꿈으로 접기 */}
      <CardHeader className="flex flex-row flex-wrap items-center justify-between gap-y-3">
        <CardTitle className="min-w-0">{t.maintenance.title}</CardTitle>
        {editing === 'closed' && (
          // max-w-full + select min-w-0: 좁으면 select 가 가장 긴 종류 이름 폭을 포기
          <div className="flex max-w-full min-w-0 items-center gap-2">
            {/* 네이티브 select. 키보드·스크린리더 기본 지원 */}
            <select
              aria-label={t.maintenance.filterLabel}
              // cn 으로 충돌 클래스 정리(h-8·w-auto 우선)
              // md 부터 13px, 모바일은 16px(iOS 확대 방지)
              className={cn(controlClassName, 'h-8 w-auto min-w-0 md:text-caption')}
              value={filter ?? ''}
              // 필터 변경 시 첫 페이지로
              onChange={(event) =>
                onViewChange({
                  page: 0,
                  filter: event.target.value === '' ? null : (event.target.value as ServiceType),
                })
              }
            >
              <option value="">{t.maintenance.allTypes}</option>
              {SERVICE_TYPES.map((type) => (
                <option key={type} value={type}>
                  {t.serviceTypes[type]}
                </option>
              ))}
            </select>

            <Button size="sm" variant="secondary" onClick={() => setEditing('new')}>
              {t.maintenance.add}
            </Button>
          </div>
        )}
      </CardHeader>

      <CardContent className="flex flex-col gap-6">
        {editing !== 'closed' && (
          // 펼침 연출(폼 지연). 닫을 때는 없음
          <div className="form-open">
            <div>
              {/* key 필수. 폼이 열린 채 다른 행을 고르면 이전 입력이 새 기록에 덮어써짐 */}
              <MaintenanceForm
                key={editing === 'new' ? 'new' : editing.id}
                vehicleId={vehicleId}
                record={editing === 'new' ? null : editing}
                defaultOdometer={currentOdometer}
                onSaved={(type) => handleSaved(type, editing === 'new')}
                onCancel={() => setEditing('closed')}
              />
            </div>
          </div>
        )}

        {shownError !== null && <ErrorText message={shownError} />}

        {isPending ? (
          <div className="flex flex-col gap-5">
            {[0, 1].map((row) => (
              <Skeleton key={row} className="h-12" />
            ))}
          </div>
        ) : data === undefined || data.totalElements === 0 ? (
          // 필터 결과가 비었을 때는 별도 문구
          <p className="py-4 text-caption text-muted-foreground">
            {filter === null
              ? t.maintenance.empty
              : t.maintenance.emptyFiltered(t.serviceTypes[filter])}
          </p>
        ) : (
          <ul className="divide-y divide-border">
            {data.items.map((record) => (
              // 호버 시 버튼 진하게. 터치 대비 완전히 숨기지 않음
              <li
                key={record.id}
                className="group flex flex-wrap items-start gap-x-4 gap-y-3 py-5 first:pt-0 last:pb-0 sm:flex-nowrap sm:gap-6"
              >
                {/* flex-1 + min-w-0: 긴 메모의 숫자 열 밀림 방지 */}
                {/* basis-full: 좁은 화면에서 첫 줄 전체 */}
                <div className="flex min-w-0 flex-1 basis-full flex-col gap-1 sm:basis-auto">
                  <div className="flex items-baseline gap-2.5">
                    <span className="text-body font-medium tracking-[-0.01em] text-strong">
                      {t.serviceTypes[record.type]}
                    </span>
                    <span className="text-caption tabular-nums text-muted-foreground">
                      {f.date(record.serviceDate)}
                    </span>
                  </div>

                  {record.description !== null && record.description !== '' && (
                    <p className="text-caption leading-relaxed text-muted-foreground">
                      {record.description}
                    </p>
                  )}
                </div>

                {/* 수치는 오른쪽 정렬 열 */}
                <div className="shrink-0 sm:text-right">
                  {/* 모르고 비운 값은 — (0 으로 보이면 적은 값처럼 읽힘) */}
                  <p className="text-body tabular-nums text-strong">
                    {record.serviceOdometer === null ? `— ${f.distanceUnit}` : f.distance(record.serviceOdometer)}
                  </p>
                  <p className="text-caption tabular-nums text-muted-foreground">
                    {record.cost === null ? '—' : f.money(record.cost, record.currency)}
                  </p>
                </div>

                {/* 터치 기기에서는 항상 진하게 */}
                <div className="ml-auto flex shrink-0 gap-0.5 opacity-70 transition-opacity duration-200 ease-apple group-hover:opacity-100 [@media(pointer:coarse)]:opacity-100">
                  <Button
                    size="xs"
                    variant="ghost"
                    disabled={deletingId === record.id}
                    onClick={() => setEditing(record)}
                  >
                    {t.common.edit}
                  </Button>
                  <Button
                    size="xs"
                    variant="ghost"
                    disabled={deletingId === record.id}
                    onClick={() => list.handleDelete(record.id)}
                  >
                    {deletingId === record.id ? t.common.deleting : t.common.delete}
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}

        {data !== undefined && (
          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            hasNext={data.hasNext}
            // 페이지 이동 시 수정 폼·에러 초기화
            onChange={list.changePage}
          />
        )}
      </CardContent>
    </Card>
  )
}
