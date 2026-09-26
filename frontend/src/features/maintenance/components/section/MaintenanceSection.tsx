import { useCallback, useState } from 'react'

import { MaintenanceForm } from '@/features/maintenance/components/form/MaintenanceForm'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { Pagination } from '@/shared/ui/nav/pagination'
import { ErrorText, Skeleton } from '@/shared/ui/feedback/state'
import { ApiError } from '@/shared/api/client/client'
import { formatDate, formatKm, formatWon } from '@/shared/lib/format/format'
import { useAsyncData } from '@/shared/lib/hooks/useAsyncData'
import { deleteRecord, fetchRecords } from '@/features/maintenance/api/endpoints/endpoints'
import { SERVICE_TYPE_LABELS } from '@/features/maintenance/api/types/types'
import { controlClassName } from '@/shared/ui/form/control'
import { cn } from 'cn'
import type { MaintenanceRecordResponse, ServiceType } from '@/features/maintenance/api/types/types'

interface Props {
  vehicleId: string
  currentOdometer: number
  /** 이력 변경 시 부모에 알림. 다음 정비 재계산 */
  onChanged: () => void
}

export function MaintenanceSection({ vehicleId, currentOdometer, onChanged }: Props) {
  const [page, setPage] = useState(0)
  // 종류 필터. null 이면 전체
  const [filter, setFilter] = useState<ServiceType | null>(null)

  // 'closed' | 'new' | 수정할 이력
  const [editing, setEditing] = useState<'closed' | 'new' | MaintenanceRecordResponse>('closed')
  // 조회 실패와 동작 실패 분리
  const [actionError, setActionError] = useState<string | null>(null)
  // 삭제 중인 행 id
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const load = useCallback(
    () => fetchRecords(vehicleId, page, filter),
    [vehicleId, page, filter],
  )
  const { data, loading, error, reload } = useAsyncData(load, '정비 이력을 불러오지 못했습니다.')

  // 변수로 받아 타입 좁히기
  const errorMessage = error ?? actionError

  /**
   * 방금 저장한 종류
   * 필터 밖 종류로 저장했으면 필터 해제. 저장 실패로 오해해 중복 등록하는 문제 방지
   */
  function refresh(savedType?: ServiceType) {
    setEditing('closed')
    setActionError(null)
    onChanged()

    if (savedType !== undefined && filter !== null && filter !== savedType) {
      // filter 변경만으로 재조회
      setFilter(null)
      setPage(0)
      return
    }

    reload()
  }

  async function handleDelete(recordId: string) {
    if (!window.confirm('이 정비 이력을 삭제할까요?')) {
      return
    }

    setDeletingId(recordId)

    try {
      await deleteRecord(vehicleId, recordId)

      // 페이지의 마지막 한 건이면 한 장 뒤로. 빈 페이지 고립 방지
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
        <CardTitle className="min-w-0">정비 이력</CardTitle>
        {editing === 'closed' && (
          <div className="flex items-center gap-2">
            {/* 네이티브 select. 키보드·스크린리더 기본 지원 */}
            <select
              aria-label="정비 종류로 거르기"
              // cn 으로 충돌 클래스 정리(h-8·w-auto 우선)
              // 크기는 임의 값. 토큰은 cn 이 색으로 인식
              // md 부터 13px, 모바일은 16px(iOS 확대 방지)
              className={cn(controlClassName, 'h-8 w-auto md:text-[0.8125rem]')}
              value={filter ?? ''}
              onChange={(event) => {
                setFilter(event.target.value === '' ? null : (event.target.value as ServiceType))
                // 필터 변경 시 첫 페이지로
                setPage(0)
              }}
            >
              <option value="">전체 종류</option>
              {(Object.keys(SERVICE_TYPE_LABELS) as ServiceType[]).map((type) => (
                <option key={type} value={type}>
                  {SERVICE_TYPE_LABELS[type]}
                </option>
              ))}
            </select>

            <Button size="sm" variant="secondary" onClick={() => setEditing('new')}>
              이력 추가
            </Button>
          </div>
        )}
      </CardHeader>

      <CardContent className="flex flex-col gap-6">
        {editing !== 'closed' && (
          // 펼침 연출(폼 지연). 닫을 때는 없음
          <div className="form-open">
            <div>
              <MaintenanceForm
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
          <div className="flex flex-col gap-5">
            {[0, 1].map((row) => (
              <Skeleton key={row} className="h-12" />
            ))}
          </div>
        ) : data === null || data.totalElements === 0 ? (
          // 필터 결과가 비었을 때는 별도 문구
          <p className="py-4 text-caption text-muted-foreground">
            {filter === null
              ? '아직 등록된 정비 이력이 없습니다.'
              : `${SERVICE_TYPE_LABELS[filter]} 이력이 없습니다. 위에서 '전체 종류' 로 바꾸면 전부 보입니다.`}
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
                      {SERVICE_TYPE_LABELS[record.type]}
                    </span>
                    <span className="text-caption tabular-nums text-muted-foreground">
                      {formatDate(record.serviceDate)}
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
                  <p className="text-body tabular-nums text-strong">
                    {formatKm(record.serviceOdometer)}
                  </p>
                  <p className="text-caption tabular-nums text-muted-foreground">
                    {formatWon(record.cost)}
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
                    수정
                  </Button>
                  <Button
                    size="xs"
                    variant="ghost"
                    disabled={deletingId === record.id}
                    onClick={() => handleDelete(record.id)}
                  >
                    {deletingId === record.id ? '삭제 중…' : '삭제'}
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}

        {data !== null && (
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
        )}
      </CardContent>
    </Card>
  )
}
