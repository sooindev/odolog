import { useCallback, useState } from 'react'
import type { FormEvent } from 'react'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { ErrorText, Skeleton } from '@/shared/ui/state'
import { useI18n } from '@/shared/i18n/I18nContext'
import type { I18nValue } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import { useAsyncData } from '@/shared/lib/hooks/useAsyncData'
import { fromKm, toKm } from '@/shared/lib/units'
import { Button } from '@/shared/ui/base/button'
import { Field } from '@/shared/ui/form/field'
import { Input } from '@/shared/ui/base/input'
import { FormActions } from '@/shared/ui/layout/page'
import {
  changeServiceInterval,
  fetchNextServices,
} from '@/features/maintenance/api/endpoints'
import type { NextServiceResponse, ServiceType } from '@/features/maintenance/api/types'

/**
 * 종류별 다음 정비 시점. 요청 1번, 이력 있는 종류만
 * 재조회는 부모의 key 변경
 */
export function NextServiceCard({ vehicleId }: { vehicleId: string }) {
  const i18n = useI18n()
  const { t } = i18n
  const load = useCallback(() => fetchNextServices(vehicleId), [vehicleId])
  const {
    data: results,
    loading,
    error,
    reload,
  } = useAsyncData(load, t.maintenance.next.loadFailed)

  // 주기 편집 중인 종류. 한 번에 하나
  const [editing, setEditing] = useState<ServiceType | null>(null)

  // 껍데기는 항상 렌더. 아래 내용 튐 방지
  return (
    <Card>
      <CardHeader>
        <CardTitle>{t.maintenance.next.title}</CardTitle>
        <CardDescription>{t.maintenance.next.description}</CardDescription>
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
            {t.maintenance.next.empty}
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
                    {t.serviceTypes[result.type]}
                  </span>
                  {/* 지남 표시. 빨강(실패) 대신 테두리 + strong */}
                  {result.overdue && (
                    <span className="shrink-0 border border-strong/30 px-1.5 py-0.5 text-unit font-medium text-strong">
                      {t.maintenance.next.overdue}
                    </span>
                  )}
                  {/* 곧. 지남보다 한 단계 낮은 대비(기본 괘선 + 보조 글자) */}
                  {result.dueSoon && (
                    <span className="shrink-0 border border-border px-1.5 py-0.5 text-unit text-muted-foreground">
                      {t.maintenance.next.dueSoon}
                    </span>
                  )}
                </span>

                <span className="order-3 text-caption tabular-nums text-muted-foreground sm:order-none">
                  {describeLast(result, i18n)}
                </span>

                <span className="flex items-baseline justify-end gap-2 text-right">
                  <span
                    className={`text-caption tabular-nums ${
                      result.overdue ? 'font-medium text-strong' : 'text-foreground'
                    }`}
                  >
                    {describeNext(result, i18n)}
                  </span>
                  {/* 주기 편집 버튼 */}
                  <button
                    type="button"
                    className="shrink-0 text-unit text-muted-foreground underline-offset-4 transition-opacity duration-200 ease-apple hover:opacity-70 hover:underline"
                    onClick={() => setEditing(editing === result.type ? null : result.type)}
                  >
                    {result.customized ? t.maintenance.next.intervalCustomized : t.maintenance.next.interval}
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
  const { t, f, unitSystem } = useI18n()
  // 거리 주기는 화면 단위(마일)로 보여 주고 km 로 저장
  const shown = (km: number) => String(Math.round(fromKm(unitSystem, km)))

  // 직접 정한 칸만 채우고 기본값 칸은 비움. 기본값을 채우면 그대로 저장 시 설정으로 굳음
  // 기본값은 placeholder(비우면 쓰일 값). 처음 값을 기억해 손대지 않으면 저장값(km) 그대로
  const [initialKm] = useState(result.customIntervalKm === null ? '' : shown(result.customIntervalKm))
  const [km, setKm] = useState(initialKm)
  const [months, setMonths] = useState(
    result.customIntervalMonths === null ? '' : String(result.customIntervalMonths),
  )
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setPending(true)

    try {
      await changeServiceInterval(vehicleId, result.type, {
        intervalKm: km === '' ? null : km === initialKm ? result.customIntervalKm : toKm(unitSystem, Number(km)),
        intervalMonths: months === '' ? null : Number(months),
      })
      onSaved()
    } catch (caught) {
      setError(errorMessage(caught, t, t.maintenance.next.intervalFailed))
      setPending(false)
    }
  }

  return (
    // 펼침 연출. 닫을 때는 없음
    <form className="form-open" onSubmit={handleSubmit}>
      {/* 감싸는 div: 잘리는 선을 바깥으로 미는 여백이 면 안쪽에 들어가지 않게 */}
      <div>
        <div className="flex flex-col gap-4 bg-sunken p-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field
              label={t.maintenance.next.intervalDistance(f.distanceUnit)}
              htmlFor={`interval-km-${result.type}`}
              hint={km === '' ? t.maintenance.next.intervalEmptyHint : undefined}
            >
              <Input
                id={`interval-km-${result.type}`}
                type="number"
                autoFocus
                min={1}
                max={Math.floor(fromKm(unitSystem, 500_000))}
                placeholder={result.defaultIntervalKm === null ? t.common.none : shown(result.defaultIntervalKm)}
                className="tabular-nums"
                value={km}
                onChange={(event) => setKm(event.target.value)}
              />
            </Field>

            <Field
              label={t.maintenance.next.intervalMonths}
              htmlFor={`interval-months-${result.type}`}
              hint={months === '' ? t.maintenance.next.intervalEmptyHint : undefined}
            >
              <Input
                id={`interval-months-${result.type}`}
                type="number"
                min={1}
                max={120}
                placeholder={
                  result.defaultIntervalMonths === null ? t.common.none : String(result.defaultIntervalMonths)
                }
                className="tabular-nums"
                value={months}
                onChange={(event) => setMonths(event.target.value)}
              />
            </Field>
          </div>

          <p className="text-caption leading-relaxed text-muted-foreground">
            {t.maintenance.next.intervalNote}
          </p>

          {error !== null && <ErrorText message={error} />}

          <FormActions>
            <Button type="submit" size="sm" disabled={pending}>
              {pending ? t.common.saving : t.common.save}
            </Button>
            <Button type="button" size="sm" variant="ghost" onClick={onCancel}>
              {t.common.cancel}
            </Button>
          </FormActions>
        </div>
      </div>
    </form>
  )
}

/** 마지막 정비 시점 */
function describeLast(result: NextServiceResponse, { t, f }: I18nValue) {
  if (result.lastServiceDate === null) {
    return ''
  }

  const parts = [f.date(result.lastServiceDate)]
  if (result.lastServiceOdometer !== null) {
    parts.push(f.distance(result.lastServiceOdometer))
  }

  return t.maintenance.next.last(parts.join(' · '))
}

/** 다음 정비 시점. 주기 없음(OTHER) 또는 계산값 */
function describeNext(result: NextServiceResponse, { t, f }: I18nValue) {
  const parts: string[] = []
  if (result.nextServiceOdometer !== null) {
    parts.push(f.distance(result.nextServiceOdometer))
  }
  if (result.nextServiceDate !== null) {
    parts.push(f.date(result.nextServiceDate))
  }

  if (parts.length === 0) {
    return t.maintenance.next.noInterval
  }

  return parts.join(t.maintenance.next.or)
}
