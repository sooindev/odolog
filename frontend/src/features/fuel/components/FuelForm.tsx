import { useState } from 'react'
import type { FormEvent } from 'react'

import { Button } from '@/shared/ui/base/button'
import { Field } from '@/shared/ui/form/field'
import { Input } from '@/shared/ui/base/input'
import { DateInput } from '@/shared/ui/form/date-input'
import { Textarea } from '@/shared/ui/base/textarea'
import { FormActions } from '@/shared/ui/layout/page'
import { ErrorText } from '@/shared/ui/state'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import { todayString } from '@/shared/lib/format'
import { MAX_ODOMETER } from '@/shared/lib/limits'
import { fromMinor, inputStep, maxMajor, toMinor } from '@/shared/lib/money'
import { looksBigJump, looksPast } from '@/shared/lib/odometer'
import { fromKm, fromLiters, toKm, toLiters } from '@/shared/lib/units'
import {
  registerFuelRecord,
  updateFuelRecord,
} from '@/features/fuel/api/endpoints'
import type { FuelRecordResponse } from '@/features/fuel/api/types'

/** 등록·수정 겸용. record 가 null 이면 등록 */
export function FuelForm({
  vehicleId,
  record,
  defaultOdometer,
  onSaved,
  onCancel,
}: {
  vehicleId: string
  record: FuelRecordResponse | null
  /** 차량의 현재 주행거리(km) */
  defaultOdometer: number
  onSaved: () => void
  onCancel: () => void
}) {
  const { t, f, unitSystem, timeZone, currency: userCurrency } = useI18n()

  // 폼을 연 시점의 차량 주행거리 고정(km). 입력칸과 판정 기준의 어긋남 방지
  const [baseOdometer] = useState(defaultOdometer)
  // 수정은 기록의 통화, 등록은 지금 사용자 통화. 서버도 같은 규칙
  const currency = record?.currency ?? userCurrency

  // 입력칸은 화면 단위(마일·갤런·달러). 처음 값 문자열을 기억해 손대지 않은 칸은 저장값 그대로
  const initialOdometerKm = record?.odometer ?? defaultOdometer
  const [initial] = useState(() => ({
    odometer: String(Math.round(fromKm(unitSystem, initialOdometerKm))),
    volume: record?.liters == null ? '' : String(Math.round(fromLiters(unitSystem, record.liters) * 100) / 100),
    cost: record?.totalCost == null ? '' : String(fromMinor(record.totalCost, currency)),
  }))

  const [fueledAt, setFueledAt] = useState(record?.fueledAt ?? todayString(timeZone))
  const [odometer, setOdometer] = useState(initial.odometer)
  const [volume, setVolume] = useState(initial.volume)
  const [totalCost, setTotalCost] = useState(initial.cost)
  const [memo, setMemo] = useState(record?.memo ?? '')
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  // 저장 단위(km·L·최소 단위)로 바꾼 값. 빈 칸은 null(Number('') 는 0)
  const odometerKm = odometer === initial.odometer ? initialOdometerKm : toKm(unitSystem, Number(odometer))
  const liters =
    volume === '' ? null : volume === initial.volume ? (record?.liters ?? null) : toLiters(unitSystem, Number(volume))
  const costMinor =
    totalCost === '' ? null : totalCost === initial.cost ? (record?.totalCost ?? null) : toMinor(Number(totalCost), currency)

  // 입력 중 단가(화면 부피 단위당). 영수증 대조용
  const volumeValue = volume === '' ? null : Number(volume)
  const pricePerUnit =
    volumeValue !== null && costMinor !== null && volumeValue > 0 ? Math.round(costMinor / volumeValue) : null

  // 주행거리 판정 규칙은 shared/lib/odometer. 막지 않고 안내만
  const past = odometer !== '' && looksPast(odometerKm, baseOdometer)
  const bigJump = odometer !== '' && looksBigJump(odometerKm, baseOdometer)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    // 저장 전 경고를 모아 한 번만 확인
    // 주행거리 그대로: 구간 거리 0, 차량 주행거리 미갱신
    // 주유량·금액 비움: 각각 못 하게 되는 일 안내
    const warnings: string[] = []

    if (record === null && odometerKm === baseOdometer) {
      warnings.push(t.fuel.form.warnSameOdometer(f.distance(baseOdometer)))
    }
    // 수정 시에는 이번에 새로 비운 경우만
    if (liters === null && (record === null || record.liters !== null)) {
      warnings.push(t.fuel.form.warnNoVolume)
    }
    if (costMinor === null && (record === null || record.totalCost !== null)) {
      warnings.push(t.fuel.form.warnNoCost)
    }
    if (bigJump) {
      // 급증은 되돌리기 어려움. force 정정으로만 복구
      warnings.push(t.fuel.form.warnBigJump(f.distance(baseOdometer), f.distance(odometerKm)))
    }

    if (warnings.length > 0) {
      const confirmed = window.confirm(
        `${t.fuel.form.confirmIntro}\n\n${warnings.join('\n')}\n\n${t.fuel.form.confirmOutro}`,
      )
      if (!confirmed) {
        return
      }
    }

    setPending(true)

    try {
      if (record === null) {
        await registerFuelRecord(vehicleId, {
          fueledAt,
          odometer: odometerKm,
          liters,
          totalCost: costMinor,
          memo: memo === '' ? undefined : memo,
        })
      } else {
        // 바뀐 필드만. 비움은 clear 플래그
        const request = {
          fueledAt: fueledAt === record.fueledAt ? undefined : fueledAt,
          odometer: odometerKm === record.odometer ? undefined : odometerKm,
          liters: liters !== null && liters !== record.liters ? liters : undefined,
          clearLiters: liters === null && record.liters !== null ? true : undefined,
          totalCost: costMinor !== null && costMinor !== record.totalCost ? costMinor : undefined,
          clearTotalCost: costMinor === null && record.totalCost !== null ? true : undefined,
          memo: memo === (record.memo ?? '') ? undefined : memo,
        }

        // 변경 없으면 요청 생략
        if (Object.values(request).every((value) => value === undefined)) {
          onCancel()
          return
        }

        await updateFuelRecord(vehicleId, record.id, request)
      }

      onSaved()
    } catch (caught) {
      setError(errorMessage(caught, t, t.fuel.form.failed))
      setPending(false)
    }
  }

  return (
    <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
      <div className="grid gap-5 sm:grid-cols-2">
        <Field label={t.fuel.form.date} htmlFor="fuel-date">
          <DateInput id="fuel-date" required value={fueledAt} onChange={setFueledAt} />
        </Field>

        <Field
          label={t.fuel.form.odometer(f.distanceUnit)}
          htmlFor="fuel-odometer"
          // 상태별 도움말. 비었을 때 우선
          hint={
            odometer === ''
              ? t.fuel.form.odometerEmpty
              : past
                ? t.odometerHints.past(f.distance(baseOdometer))
                : bigJump
                  ? t.odometerHints.bigJump(f.distance(baseOdometer))
                  : t.fuel.form.odometerHint
          }
        >
          <Input
            id="fuel-odometer"
            type="number"
            // 주행거리에 첫 포커스
            autoFocus
            required
            min={0}
            max={Math.floor(fromKm(unitSystem, MAX_ODOMETER))}
            className="tabular-nums"
            value={odometer}
            onChange={(event) => setOdometer(event.target.value)}
          />
        </Field>
      </div>

      <div className="grid gap-5 sm:grid-cols-2">
        {/* step 0.01: 소수 2자리. 상한은 서버 BigDecimal(6,2) 를 화면 단위로 */}
        <Field
          label={t.fuel.form.volume(f.volumeUnit)}
          htmlFor="fuel-liters"
          // 선택 입력. 비우면 못 하게 되는 일만 안내
          hint={volume === '' ? t.fuel.form.volumeEmpty : undefined}
        >
          <Input
            id="fuel-liters"
            type="number"
            min={0.01}
            max={Math.floor(fromLiters(unitSystem, 9999.99) * 100) / 100}
            step={0.01}
            className="tabular-nums"
            placeholder={f.volumeUnit === 'gal' ? '9.87' : '32.45'}
            value={volume}
            onChange={(event) => setVolume(event.target.value)}
          />
        </Field>

        <Field
          label={t.fuel.form.cost(currency)}
          htmlFor="fuel-cost"
          hint={
            pricePerUnit !== null
              ? t.fuel.form.pricePer(f.money(pricePerUnit, currency), f.volumeUnit)
              : totalCost === ''
                ? t.fuel.form.costEmpty
                : undefined
          }
        >
          <Input
            id="fuel-cost"
            type="number"
            min={0}
            max={maxMajor(currency)}
            step={inputStep(currency)}
            className="tabular-nums"
            value={totalCost}
            onChange={(event) => setTotalCost(event.target.value)}
          />
        </Field>
      </div>

      <Field label={t.fuel.form.memo} htmlFor="fuel-memo" hint={t.common.optional}>
        <Textarea
          id="fuel-memo"
          rows={2}
          maxLength={255}
          placeholder={t.fuel.form.memoPlaceholder}
          value={memo}
          onChange={(event) => setMemo(event.target.value)}
        />
      </Field>

      {error !== null && <ErrorText message={error} />}

      <FormActions>
        <Button type="submit" disabled={pending}>
          {pending ? t.common.saving : record === null ? t.common.register : t.common.save}
        </Button>
        <Button type="button" variant="ghost" onClick={onCancel}>
          {t.common.cancel}
        </Button>
      </FormActions>
    </form>
  )
}
