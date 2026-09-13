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
import type { MaintenanceRecordResponse } from '@/features/maintenance/api/types/types'

interface Props {
  vehicleId: number
  currentOdometer: number
  /** 이력이 바뀌면 부모에게 알려 "다음 정비 시점"도 다시 계산하게 한다. */
  onChanged: () => void
}

export function MaintenanceSection({ vehicleId, currentOdometer, onChanged }: Props) {
  const [page, setPage] = useState(0)

  // 폼 상태: 'closed' | 'new' | 수정할 이력
  const [editing, setEditing] = useState<'closed' | 'new' | MaintenanceRecordResponse>('closed')
  // 조회 실패와 행동 실패는 사라져야 하는 시점이 달라서 따로 둔다.
  const [actionError, setActionError] = useState<string | null>(null)
  // 삭제 중인 이력의 id. boolean 이면 목록 전체가 잠겨 어느 줄을 지우는 중인지 안 보인다.
  const [deletingId, setDeletingId] = useState<number | null>(null)

  const load = useCallback(() => fetchRecords(vehicleId, page), [vehicleId, page])
  const { data, loading, error, reload } = useAsyncData(load, '정비 이력을 불러오지 못했습니다.')

  // 변수로 받아 둬야 타입이 좁혀진다. JSX 에서 같은 식을 두 번 쓰면 매번 새 식이라
  // 좁혀지지 않고 단언이 필요해진다.
  const errorMessage = error ?? actionError

  function refresh() {
    setEditing('closed')
    setActionError(null)
    reload()
    onChanged()
  }

  async function handleDelete(recordId: number) {
    if (!window.confirm('이 정비 이력을 삭제할까요?')) {
      return
    }

    setDeletingId(recordId)

    try {
      await deleteRecord(vehicleId, recordId)

      // 이 페이지의 마지막 한 건을 지웠으면 한 장 물러난다. 그대로 두면 빈 페이지가 되는데
      // totalElements 가 0 이 아니라 "이력이 없습니다" 도 안 뜨고, Pagination 도 사라져
      // 돌아갈 버튼이 없다.
      // page 만 바꾸면 useAsyncData 가 알아서 다시 조회한다. reload() 까지 부르면 두 번 나간다.
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
        <CardTitle>정비 이력</CardTitle>
        {editing === 'closed' && (
          <Button size="sm" variant="secondary" onClick={() => setEditing('new')}>
            이력 추가
          </Button>
        )}
      </CardHeader>

      <CardContent className="flex flex-col gap-6">
        {editing !== 'closed' && (
          // 칸이 열리고 폼이 조금 늦게 들어온다. 닫을 때는 연출하지 않는다.
          // 사라지는 것을 붙잡아 두려면 상태를 하나 더 들고 있어야 한다.
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
          <p className="py-4 text-sm text-muted-foreground">아직 등록된 정비 이력이 없습니다.</p>
        ) : (
          <ul className="divide-y divide-border">
            {data.items.map((record) => (
              // 줄에 마우스가 올라가면 오른쪽 버튼이 진해진다.
              // 완전히 숨기지는 않는다. 터치 기기에는 호버가 없어서 영영 못 찾는다.
              <li
                key={record.id}
                className="group flex flex-wrap items-start gap-x-4 gap-y-3 py-5 first:pt-0 last:pb-0 sm:flex-nowrap sm:gap-6"
              >
                {/* flex-1 + min-w-0: 남는 폭을 전부 가져가되, 긴 메모가 오른쪽 숫자 열을
                    밀어내지는 못하게 한다. */}
                {/* basis-full: 좁은 화면에서 첫 줄을 통째로 쓴다. 한 줄에 다 넣으면
                    종류와 날짜가 들어갈 폭이 100px 남짓밖에 안 된다. */}
                <div className="flex min-w-0 flex-1 basis-full flex-col gap-1 sm:basis-auto">
                  <div className="flex items-baseline gap-2.5">
                    <span className="text-[0.9375rem] font-medium tracking-[-0.01em] text-strong">
                      {SERVICE_TYPE_LABELS[record.type]}
                    </span>
                    <span className="text-xs tabular-nums text-muted-foreground">
                      {formatDate(record.serviceDate)}
                    </span>
                  </div>

                  {record.description !== null && record.description !== '' && (
                    <p className="text-[0.8125rem] leading-relaxed text-muted-foreground">
                      {record.description}
                    </p>
                  )}
                </div>

                {/* 수치를 별도 열로 빼 오른쪽 정렬한다. 줄마다 왼쪽에서 시작하면 자릿수가
                    다른 값들이 들쭉날쭉해서 세로로 훑어 읽을 수가 없다. */}
                <div className="shrink-0 sm:text-right">
                  <p className="text-[0.9375rem] tabular-nums text-strong">
                    {formatKm(record.serviceOdometer)}
                  </p>
                  <p className="text-[0.8125rem] tabular-nums text-muted-foreground">
                    {formatWon(record.cost)}
                  </p>
                </div>

                {/* 터치 기기에는 호버가 없어서 흐린 채로 남는다. 거기서는 항상 진하게. */}
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
            onChange={setPage}
          />
        )}
      </CardContent>
    </Card>
  )
}
