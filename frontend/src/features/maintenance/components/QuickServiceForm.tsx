import { useState } from 'react'
import type { FormEvent } from 'react'

import { Button } from '@/shared/ui/base/button'
import { Input } from '@/shared/ui/base/input'
import { ErrorText } from '@/shared/ui/state'
import { DateInput } from '@/shared/ui/form/date-input'
import { monthsAgo } from '@/shared/ui/form/date-parts'
import { FormActions } from '@/shared/ui/layout/page'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import { todayString } from '@/shared/lib/format'
import { MAX_ODOMETER } from '@/shared/lib/limits'
import { looksBigJump } from '@/shared/lib/odometer'
import { fromKm, toKm } from '@/shared/lib/units'
import { registerRecord } from '@/features/maintenance/api/endpoints'
import type { ServiceType } from '@/features/maintenance/api/types'

/** 타던 차에서 가장 먼저 궁금한 것들. 전부 권장 주기가 있는 종류 */
const QUICK_TYPES: ServiceType[] = ['ENGINE_OIL', 'TIRE', 'BRAKE_PAD', 'BATTERY', 'CABIN_FILTER']

/** "석 달 전쯤" 처럼 기억하는 단위 */
const PRESETS = [3, 6, 12] as const

type Choice = 'unknown' | (typeof PRESETS)[number] | 'date'

interface Row {
  choice: Choice
  /** choice 가 'date' 일 때만 */
  date: string
  /** 화면 단위 문자열. 비우면 모름 */
  odometer: string
}

/**
 * 기억나는 최근 정비를 한 번에. 타던 차의 시작 단계
 * 비용은 묻지 않음(대부분 기억 못 함). 날짜만으로 다음 정비가 계산됨
 */
export function QuickServiceForm({
  vehicleId,
  currentOdometer,
  onSaved,
  onCancel,
}: {
  vehicleId: string
  /** 차량의 현재 주행거리(km). 급증 확인 기준 */
  currentOdometer: number
  onSaved: () => void
  onCancel: () => void
}) {
  const { t, f, unitSystem, timeZone } = useI18n()
  const today = todayString(timeZone)

  const [rows, setRows] = useState<Record<ServiceType, Row>>(
    () =>
      Object.fromEntries(
        QUICK_TYPES.map((type) => [type, { choice: 'unknown', date: today, odometer: '' }]),
      ) as Record<ServiceType, Row>,
  )
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)
  // 일부만 저장된 채 실패했는지. 닫을 때 부모가 새 기록을 다시 읽게
  const [savedSome, setSavedSome] = useState(false)

  function update(type: ServiceType, patch: Partial<Row>) {
    setRows((current) => ({ ...current, [type]: { ...current[type], ...patch } }))
  }

  function dateOf(row: Row) {
    return row.choice === 'date' ? row.date : monthsAgo(today, row.choice as number)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    const picked = QUICK_TYPES.filter((type) => rows[type].choice !== 'unknown')
    if (picked.length === 0) {
      setError(t.maintenance.quick.nothingPicked)
      return
    }

    // 급증은 되돌리기 어려움. 가장 큰 값 하나로 한 번만 확인
    const largest = Math.max(
      ...picked
        .filter((type) => rows[type].odometer !== '')
        .map((type) => toKm(unitSystem, Number(rows[type].odometer))),
      0,
    )
    if (looksBigJump(largest, currentOdometer)) {
      const confirmed = window.confirm(
        t.odometerHints.bigJumpConfirm(f.distance(currentOdometer), f.distance(largest)),
      )
      if (!confirmed) {
        return
      }
    }

    setPending(true)
    let saved = 0

    try {
      // 순서대로. 동시에 보내면 서버의 차량 주행거리 갱신이 서로 겹침
      for (const type of picked) {
        const row = rows[type]
        await registerRecord(vehicleId, {
          type,
          serviceDate: dateOf(row),
          serviceOdometer: row.odometer === '' ? null : toKm(unitSystem, Number(row.odometer)),
          cost: null,
        })
        // 저장된 줄은 모름으로 되돌림. 중간에 실패해 다시 눌러도 같은 기록이 두 번 들어가지 않게
        update(type, { choice: 'unknown', odometer: '' })
        saved += 1
        setSavedSome(true)
      }
      onSaved()
    } catch (caught) {
      const reason = errorMessage(caught, t, t.maintenance.quick.failed)
      // 저장된 줄이 있으면 밝힘. 안 그러면 아무것도 안 들어간 줄 알고 다시 적음
      setError(saved > 0 ? t.maintenance.quick.partlySaved(saved, reason) : reason)
      setPending(false)
    }
  }

  return (
    <form className="flex flex-col gap-5 border border-border bg-sunken p-4 sm:p-6" onSubmit={handleSubmit}>
      <p className="text-caption leading-relaxed text-muted-foreground">{t.maintenance.quick.intro}</p>

      <ul className="divide-y divide-border">
        {QUICK_TYPES.map((type) => {
          const row = rows[type]
          const choices: Choice[] = ['unknown', ...PRESETS, 'date']

          return (
            <li key={type} className="flex flex-col gap-3 py-4 first:pt-0 last:pb-0">
              <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
                <span className="text-body font-medium text-strong">{t.serviceTypes[type]}</span>

                {/* 한 줄 선택. 고른 칸만 채워진 면 */}
                <div role="group" aria-label={t.serviceTypes[type]} className="flex flex-wrap gap-1">
                  {choices.map((choice) => (
                    <Button
                      key={String(choice)}
                      type="button"
                      size="xs"
                      variant={row.choice === choice ? 'secondary' : 'ghost'}
                      aria-pressed={row.choice === choice}
                      onClick={() => update(type, { choice })}
                    >
                      {choice === 'unknown'
                        ? t.maintenance.quick.unknown
                        : choice === 'date'
                          ? t.maintenance.quick.pickDate
                          : t.maintenance.quick.monthsAgo(choice)}
                    </Button>
                  ))}
                </div>
              </div>

              {/* 고른 뒤에만. 모르는 항목은 한 줄로 끝 */}
              {row.choice !== 'unknown' && (
                <div className="grid gap-3 sm:grid-cols-2">
                  {row.choice === 'date' ? (
                    <DateInput
                      id={`quick-date-${type}`}
                      // 칸 이름이 없는 자리. 어느 정비의 날짜인지 읽어 줌
                      ariaLabel={`${t.serviceTypes[type]} · ${t.maintenance.quick.pickDate}`}
                      required
                      value={row.date}
                      onChange={(date) => update(type, { date })}
                    />
                  ) : (
                    // 버튼이 고른 날짜를 보여 줌. "몇 달 전" 이 실제로 언제인지
                    <p className="flex h-11 items-center text-caption tabular-nums text-muted-foreground">
                      {f.date(dateOf(row))}
                    </p>
                  )}
                  <Input
                    id={`quick-odometer-${type}`}
                    type="number"
                    min={0}
                    max={Math.floor(fromKm(unitSystem, MAX_ODOMETER))}
                    aria-label={t.maintenance.quick.odometer(f.distanceUnit)}
                    placeholder={t.maintenance.quick.odometer(f.distanceUnit)}
                    className="tabular-nums"
                    value={row.odometer}
                    onChange={(event) => update(type, { odometer: event.target.value })}
                  />
                </div>
              )}
            </li>
          )
        })}
      </ul>

      {error !== null && <ErrorText message={error} />}

      <FormActions>
        <Button type="submit" disabled={pending}>
          {pending ? t.common.saving : t.maintenance.quick.submit}
        </Button>
        <Button type="button" variant="ghost" onClick={savedSome ? onSaved : onCancel}>
          {t.common.cancel}
        </Button>
      </FormActions>
    </form>
  )
}
