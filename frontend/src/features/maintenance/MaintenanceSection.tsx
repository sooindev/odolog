import { useCallback, useState } from 'react'

import { MaintenanceForm } from '@/features/maintenance/MaintenanceForm'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { LoadingText, ErrorText } from '@/shared/ui/state'
import { ApiError } from '@/shared/api/client'
import { formatKm, formatWon } from '@/shared/lib/format'
import { useAsyncData } from '@/shared/lib/useAsyncData'
import { deleteRecord, fetchRecords } from '@/features/maintenance/api'
import { SERVICE_TYPE_LABELS } from '@/shared/api/types'
import type { MaintenanceRecordResponse } from '@/shared/api/types'

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
  // 조회 실패와 달리 "삭제 버튼을 눌렀는데 실패"는 사용자의 행동에 대한 답이라 따로 둔다.
  const [actionError, setActionError] = useState<string | null>(null)

  const load = useCallback(() => fetchRecords(vehicleId, page), [vehicleId, page])
  const { data, loading, error, reload } = useAsyncData(load, '정비 이력을 불러오지 못했습니다.')

  // 변수로 한 번 받아 두면 TypeScript 가 아래에서 null 이 아님을 알아준다.
  // JSX 안에서 (error ?? actionError) 를 두 번 쓰면 매번 새 식이라 좁혀지지 않아 단언이 필요해진다.
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

    try {
      await deleteRecord(vehicleId, recordId)
      refresh()
    } catch (caught) {
      setActionError(caught instanceof ApiError ? caught.message : '삭제에 실패했습니다.')
    }
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="text-base">정비 이력</CardTitle>
        {editing === 'closed' && (
          <Button size="sm" onClick={() => setEditing('new')}>
            이력 추가
          </Button>
        )}
      </CardHeader>

      <CardContent className="flex flex-col gap-4">
        {editing !== 'closed' && (
          <MaintenanceForm
            vehicleId={vehicleId}
            record={editing === 'new' ? null : editing}
            defaultOdometer={currentOdometer}
            onSaved={refresh}
            onCancel={() => setEditing('closed')}
          />
        )}

        {errorMessage !== null && <ErrorText message={errorMessage} />}

        {loading ? (
          <LoadingText />
        ) : data === null || data.totalElements === 0 ? (
          <p className="text-muted-foreground text-sm">아직 등록된 정비 이력이 없습니다.</p>
        ) : (
          <ul className="divide-y">
            {data.items.map((record) => (
              <li key={record.id} className="flex items-start justify-between gap-4 py-3">
                <div className="text-sm">
                  <p className="font-medium">
                    {SERVICE_TYPE_LABELS[record.type]}
                    <span className="text-muted-foreground ml-2 font-normal">
                      {record.serviceDate}
                    </span>
                  </p>
                  <p className="text-muted-foreground">
                    {formatKm(record.serviceOdometer)} · {formatWon(record.cost)}
                  </p>
                  {record.description !== null && record.description !== '' && (
                    <p className="text-muted-foreground mt-1">{record.description}</p>
                  )}
                </div>

                <div className="flex shrink-0 gap-1">
                  <Button size="sm" variant="ghost" onClick={() => setEditing(record)}>
                    수정
                  </Button>
                  <Button size="sm" variant="ghost" onClick={() => handleDelete(record.id)}>
                    삭제
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}

        {data !== null && data.totalPages > 1 && (
          <div className="flex items-center justify-center gap-3">
            <Button
              variant="outline"
              size="sm"
              disabled={data.page === 0}
              onClick={() => setPage((current) => current - 1)}
            >
              이전
            </Button>
            <span className="text-muted-foreground text-sm">
              {data.page + 1} / {data.totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={!data.hasNext}
              onClick={() => setPage((current) => current + 1)}
            >
              다음
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
