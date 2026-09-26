import { useCallback, useState } from 'react'
import type { FormEvent } from 'react'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { ErrorText, Skeleton } from '@/shared/ui/feedback/state'
import { formatDate, formatKm } from '@/shared/lib/format/format'
import { useAsyncData } from '@/shared/lib/hooks/useAsyncData'
import { Button } from '@/shared/ui/base/button'
import { Field } from '@/shared/ui/form/field'
import { Input } from '@/shared/ui/base/input'
import { FormActions } from '@/shared/ui/layout/page'
import { ApiError } from '@/shared/api/client/client'
import {
  changeServiceInterval,
  fetchNextServices,
} from '@/features/maintenance/api/endpoints/endpoints'
import { SERVICE_TYPE_LABELS } from '@/features/maintenance/api/types/types'
import type { NextServiceResponse, ServiceType } from '@/features/maintenance/api/types/types'

/**
 * 종류별 다음 정비 시점. 요청 1번, 이력 있는 종류만
 * 재조회는 부모의 key 변경
 */
export function NextServiceCard({ vehicleId }: { vehicleId: string }) {
  const load = useCallback(() => fetchNextServices(vehicleId), [vehicleId])
  const {
    data: results,
    loading,
    error,
    reload,
  } = useAsyncData(load, '다음 정비 시점을 불러오지 못했습니다.')

  // 주기 편집 중인 종류. 한 번에 하나
  const [editing, setEditing] = useState<ServiceType | null>(null)

  // 껍데기는 항상 렌더. 아래 내용 튐 방지
  return (
    <Card>
      <CardHeader>
        <CardTitle>다음 정비 시점</CardTitle>
        <CardDescription>
          종류별 권장 주기와 마지막 정비 기록으로 계산합니다. 지난 것이 위에 옵니다.
        </CardDescription>
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
          <p className="text-caption leading-relaxed text-muted-foreground">
            아직 계산할 이력이 없습니다. 정비 이력을 등록하면 그 종류의 권장 주기로 다음 시점을
            알려 드립니다.
          </p>
        )}

        {!loading && error === null && results !== null && results.length > 0 && (
          // divide-y: 항목 사이에만 선
          <ul className="divide-y divide-border">
            {results.map((result) => (
              // 넓은 화면 3열(종류 / 마지막 정비 / 다음 정비), 좁으면 2열
              <li
                key={result.type}
                className="grid grid-cols-[1fr_auto] items-baseline gap-x-6 gap-y-1.5 py-5 first:pt-0 last:pb-0 sm:grid-cols-[8rem_minmax(0,1fr)_auto]"
              >
                <span className="flex min-w-0 items-baseline gap-2">
                  <span className="truncate text-body font-medium tracking-[-0.015em] text-strong">
                    {SERVICE_TYPE_LABELS[result.type]}
                  </span>
                  {/* 지남 표시. 빨강(실패) 대신 테두리 + strong */}
                  {result.overdue && (
                    <span className="shrink-0 border border-strong/30 px-1.5 py-0.5 text-unit font-medium text-strong">
                      지남
                    </span>
                  )}
                </span>

                <span className="order-3 text-caption tabular-nums text-muted-foreground sm:order-none">
                  {describeLast(result)}
                </span>

                <span className="flex items-baseline justify-end gap-2 text-right">
                  <span
                    className={`text-caption tabular-nums ${
                      result.overdue ? 'font-medium text-strong' : 'text-foreground'
                    }`}
                  >
                    {describeNext(result)}
                  </span>
                  {/* 주기 편집 버튼 */}
                  <button
                    type="button"
                    className="shrink-0 text-unit text-muted-foreground underline-offset-4 transition-opacity duration-200 ease-apple hover:opacity-70 hover:underline"
                    onClick={() => setEditing(editing === result.type ? null : result.type)}
                  >
                    {result.customized ? '주기 변경됨' : '주기'}
                  </button>
                </span>

                {editing === result.type && (
                  // 편집 폼은 행 전체 폭
                  <div className="col-span-full">
                    <IntervalForm
                      vehicleId={vehicleId}
                      result={result}
                      onSaved={() => {
                        setEditing(null)
                        reload()
                      }}
                      onCancel={() => setEditing(null)}
                    />
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

/** 차량별 주기 편집. 두 칸을 항상 함께 전송(전체 교체), 비우면 기본값 */
function IntervalForm({
  vehicleId,
  result,
  onSaved,
  onCancel,
}: {
  vehicleId: string
  result: NextServiceResponse
  onSaved: () => void
  onCancel: () => void
}) {
  // 덮어쓴 적이 없으면 빈 칸으로 시작. 기본값을 채우면 그대로 저장 시 customized 로 바뀜
  // 적용 중인 값은 placeholder
  const [km, setKm] = useState(
    result.customized && result.intervalKm !== null ? String(result.intervalKm) : '',
  )
  const [months, setMonths] = useState(
    result.customized && result.intervalMonths !== null ? String(result.intervalMonths) : '',
  )
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setPending(true)

    try {
      await changeServiceInterval(vehicleId, result.type, {
        intervalKm: km === '' ? null : Number(km),
        intervalMonths: months === '' ? null : Number(months),
      })
      onSaved()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : '주기를 저장하지 못했습니다.')
      setPending(false)
    }
  }

  return (
    // 펼침 연출. 닫을 때는 없음
    <form className="form-open" onSubmit={handleSubmit}>
      <div className="flex flex-col gap-4 bg-sunken p-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            label="주행거리 주기 (km)"
            htmlFor={`interval-km-${result.type}`}
            hint={km === '' ? '비어 있으면 기본값(회색 숫자)을 씁니다.' : undefined}
          >
            <Input
              id={`interval-km-${result.type}`}
              type="number"
              autoFocus
              min={1}
              max={500000}
              placeholder={result.intervalKm === null ? '없음' : String(result.intervalKm)}
              className="tabular-nums"
              value={km}
              onChange={(event) => setKm(event.target.value)}
            />
          </Field>

          <Field
            label="기간 주기 (개월)"
            htmlFor={`interval-months-${result.type}`}
            hint={months === '' ? '비어 있으면 기본값(회색 숫자)을 씁니다.' : undefined}
          >
            <Input
              id={`interval-months-${result.type}`}
              type="number"
              min={1}
              max={120}
              placeholder={result.intervalMonths === null ? '없음' : String(result.intervalMonths)}
              className="tabular-nums"
              value={months}
              onChange={(event) => setMonths(event.target.value)}
            />
          </Field>
        </div>

        <p className="text-caption leading-relaxed text-muted-foreground">
          이 차량에만 적용됩니다. 둘 다 비우면 기본 권장 주기로 돌아갑니다.
        </p>

        {error !== null && <ErrorText message={error} />}

        <FormActions>
          <Button type="submit" size="sm" disabled={pending}>
            {pending ? '저장 중…' : '저장'}
          </Button>
          <Button type="button" size="sm" variant="ghost" onClick={onCancel}>
            취소
          </Button>
        </FormActions>
      </div>
    </form>
  )
}

/** 마지막 정비 시점 */
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

/** 다음 정비 시점. 주기 없음(OTHER) 또는 계산값 */
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
