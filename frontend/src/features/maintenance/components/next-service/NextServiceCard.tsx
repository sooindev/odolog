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
 * 종류별 다음 정비 시점. 요청 1번
 * 종류마다 요청하던 방식(15요청)을 버리며 "일부만 뜨는" 부분 실패 상태도 사라짐
 * 이력 있는 종류만 옴 — 15줄 중 13줄이 "이력 없음"이면 빈칸 목록이 됨
 * 재조회 장치가 없는 이유 — 부모가 key 를 바꿔 새로 만듦
 */
export function NextServiceCard({ vehicleId }: { vehicleId: string }) {
  const load = useCallback(() => fetchNextServices(vehicleId), [vehicleId])
  const {
    data: results,
    loading,
    error,
    reload,
  } = useAsyncData(load, '다음 정비 시점을 불러오지 못했습니다.')

  // 주기를 고치는 중인 종류. 한 번에 하나만 연다 — 여럿이 열리면 어느 줄을 고치는지 흐려진다
  const [editing, setEditing] = useState<ServiceType | null>(null)

  // 껍데기는 항상 렌더. 카드가 통째로 사라지면 아래 내용이 위로 튐
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
          // divide-y — 항목마다 테두리를 붙이지 않고 "사이"에만. 첫 줄 위·마지막 줄 아래에 선이 안 생김
          <ul className="divide-y divide-border">
            {results.map((result) => (
              /*
                넓은 화면 3열(종류 / 마지막 정비 / 다음 정비), 좁으면 2열
                가운데 "마지막 정비"는 sm 미만에서 숨김 — 좁은 화면에서는 결론만
                lastServiceOdometer 는 백엔드가 계속 주고 있었으나 화면이 안 쓰던 값
              */
              <li
                key={result.type}
                className="grid grid-cols-[1fr_auto] items-baseline gap-x-6 gap-y-1.5 py-5 first:pt-0 last:pb-0 sm:grid-cols-[8rem_minmax(0,1fr)_auto]"
              >
                <span className="flex min-w-0 items-baseline gap-2">
                  <span className="truncate text-body font-medium tracking-[-0.015em] text-strong">
                    {SERVICE_TYPE_LABELS[result.type]}
                  </span>
                  {/*
                    빨강을 쓰지 않는다. 빨강은 "실패" 를 나르는 기능색이고(디자인 규칙 3)
                    주유 목록의 `확인 필요`(입력 오류)가 이미 그 뜻으로 쓰고 있다.
                    정비 시기가 지난 것은 잘못이 아니라 할 일이라, 모노톤 시스템의 방식대로
                    대비를 올려서 말한다 — 테두리 친 라벨 + 아래 값도 strong 으로
                  */}
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
                  {/*
                    주기를 고치는 손잡이. 이게 없으면 `지남` 이 늘 켜져 있는 경고등이 된다 —
                    엔진오일 기본값은 광유 기준 5,000km 인데 합성유는 10,000~15,000km 다
                  */}
                  <button
                    type="button"
                    className="shrink-0 text-unit text-muted-foreground underline-offset-4 transition-opacity duration-200 ease-apple hover:opacity-70 hover:underline"
                    onClick={() => setEditing(editing === result.type ? null : result.type)}
                  >
                    {result.customized ? '주기 변경됨' : '주기'}
                  </button>
                </span>

                {editing === result.type && (
                  // 행 전체 폭을 쓴다. 오른쪽 끝에서 열면 입력칸 두 개가 들어갈 자리가 없다
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

/**
 * 이 차량에서 쓸 주기. 비우면 기본값으로 되돌아간다
 * 두 칸을 언제나 함께 보낸다 — 서버가 "안 보냄" 과 "비움" 을 가르지 않는다(전체 교체)
 */
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
  /*
   * 덮어쓴 적이 없으면 **빈 칸으로 연다.** 적용 중인 값(=기본값)을 채워 두면
   * 아무것도 안 고치고 저장했을 때 기본값과 똑같은 커스텀 설정이 생기고,
   * 버튼이 "주기 변경됨" 으로 바뀐다 — 값은 같은데 상태만 달라진다.
   * 나중에 기본 권장 주기를 손보면 그 차만 옛 값에 묶인 채 아무도 모른다.
   *
   * 도움말("비우면 기본값을 씁니다")과도 그래야 앞뒤가 맞는다.
   * 지금 적용 중인 값은 placeholder 로 보여 준다
   */
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
    // 정비 폼과 같은 펼침 연출. 닫을 때는 연출 없음
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

/** 근거 — 마지막으로 이 정비를 한 시점 */
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

/** 결론 — 권장 주기 없음(OTHER) / 정상 계산 두 경우
 *  이력 없는 종류는 서버가 안 보내므로 여기서 다룰 필요 없음 */
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
