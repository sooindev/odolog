import { useState } from 'react'
import type { FormEvent } from 'react'

import { Button } from '@/shared/ui/base/button'
import { Field } from '@/shared/ui/form/field'
import { Input } from '@/shared/ui/base/input'
import { DateInput } from '@/shared/ui/form/date-input'
import { Textarea } from '@/shared/ui/base/textarea'
import { FormActions } from '@/shared/ui/layout/page'
import { ErrorText } from '@/shared/ui/feedback/state'
import { ApiError } from '@/shared/api/client/client'
import { formatKm, todayString } from '@/shared/lib/format/format'
import { MAX_AMOUNT, MAX_ODOMETER } from '@/shared/lib/limits/limits'
import { looksBigJump, looksPast } from '@/shared/lib/odometer/odometer'
import {
  registerFuelRecord,
  updateFuelRecord,
} from '@/features/fuel/api/endpoints/endpoints'
import type { FuelRecordResponse } from '@/features/fuel/api/types/types'

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
  defaultOdometer: number
  onSaved: () => void
  onCancel: () => void
}) {
  // 폼을 연 시점의 차량 주행거리 고정. 입력칸과 판정 기준의 어긋남 방지
  const [baseOdometer] = useState(defaultOdometer)

  // 입력 중 빈 값 표현을 위해 문자열 보관
  const [fueledAt, setFueledAt] = useState(record?.fueledAt ?? todayString())
  const [odometer, setOdometer] = useState(String(record?.odometer ?? defaultOdometer))
  // 주유량 없이 저장된 기록 대비
  const [liters, setLiters] = useState(record?.liters == null ? '' : String(record.liters))
  const [totalCost, setTotalCost] = useState(record?.totalCost == null ? '' : String(record.totalCost))
  const [memo, setMemo] = useState(record?.memo ?? '')
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  // 빈 칸은 null. Number('') 는 0
  const litersValue = liters === '' ? null : Number(liters)
  const costValue = totalCost === '' ? null : Number(totalCost)

  // 입력 중 리터당 단가. 영수증 대조용
  const pricePerLiter =
    litersValue !== null && costValue !== null && litersValue > 0
      ? Math.round(costValue / litersValue)
      : null

  // 주행거리 판정 규칙은 shared/lib/odometer. 막지 않고 안내만
  const odometerValue = Number(odometer)
  const past = odometer !== '' && looksPast(odometerValue, baseOdometer)
  const bigJump = odometer !== '' && looksBigJump(odometerValue, baseOdometer)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    // 저장 전 경고를 모아 한 번만 확인
    // 주행거리 그대로: 구간 거리 0, 차량 주행거리 미갱신
    // 주유량·금액 비움: 각각 못 하게 되는 일 안내
    const warnings: string[] = []

    if (record === null && Number(odometer) === baseOdometer) {
      warnings.push(
        `· 주행거리가 차량의 현재 값(${formatKm(baseOdometer)})과 같습니다.\n` +
          '  이번 구간의 연비가 계산되지 않고, 차량 주행거리도 올라가지 않습니다.',
      )
    }
    // 수정 시에는 이번에 새로 비운 경우만
    if (litersValue === null && (record === null || record.liters !== null)) {
      warnings.push('· 주유량이 비어 있어 이번 구간의 연비를 계산할 수 없습니다.')
    }
    if (costValue === null && (record === null || record.totalCost !== null)) {
      warnings.push('· 결제 금액이 비어 있어 유류비 합계와 리터당 단가에서 빠집니다.')
    }
    if (bigJump) {
      // 급증은 되돌리기 어려움. force 정정으로만 복구
      warnings.push(
        `· 주행거리가 ${formatKm(baseOdometer)} 에서 ${formatKm(odometerValue)} 로 크게 뜁니다.\n` +
          '  자리수가 틀리면 차량 주행거리가 그 값에 묶입니다.',
      )
    }

    if (warnings.length > 0) {
      const confirmed = window.confirm(
        `이대로 저장하면:\n\n${warnings.join('\n')}\n\n계속할까요?`,
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
          odometer: Number(odometer),
          liters: litersValue,
          totalCost: costValue,
          memo: memo === '' ? undefined : memo,
        })
      } else {
        // 바뀐 필드만. 비움은 clear 플래그
        await updateFuelRecord(vehicleId, record.id, {
          fueledAt: fueledAt === record.fueledAt ? undefined : fueledAt,
          odometer: Number(odometer) === record.odometer ? undefined : Number(odometer),
          liters: litersValue !== null && litersValue !== record.liters ? litersValue : undefined,
          clearLiters: litersValue === null && record.liters !== null ? true : undefined,
          totalCost: costValue !== null && costValue !== record.totalCost ? costValue : undefined,
          clearTotalCost: costValue === null && record.totalCost !== null ? true : undefined,
          memo: memo === (record.memo ?? '') ? undefined : memo,
        })
      }

      onSaved()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : '저장에 실패했습니다.')
      setPending(false)
    }
  }

  return (
    <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="주유 날짜" htmlFor="fuel-date">
          <DateInput id="fuel-date" required value={fueledAt} onChange={setFueledAt} />
        </Field>

        <Field
          label="주행거리 (km)"
          htmlFor="fuel-odometer"
          // 상태별 도움말 셋. 비었을 때 우선
          hint={
            odometer === ''
              ? '주행거리를 적지 않으면 연비를 계산할 수 없습니다.'
              : past
                ? `차량에 기록된 ${baseOdometer.toLocaleString()}km 보다 작습니다. 과거 기록이면 그대로 두세요.`
                : bigJump
                  ? `차량에 기록된 ${baseOdometer.toLocaleString()}km 에서 크게 뜁니다. 자리수를 확인해 주세요.`
                  : '계기판 숫자. 이 값이 차량 주행거리보다 크면 차량 쪽도 함께 올라갑니다.'
          }
        >
          <Input
            id="fuel-odometer"
            type="number"
            // 주행거리에 첫 포커스
            autoFocus
            required
            min={0}
            max={MAX_ODOMETER}
            className="tabular-nums"
            value={odometer}
            onChange={(event) => setOdometer(event.target.value)}
          />
        </Field>
      </div>

      <div className="grid gap-5 sm:grid-cols-2">
        {/* step 0.01: 소수 2자리 */}
        <Field
          label="주유량 (L)"
          htmlFor="fuel-liters"
          // 선택 입력. 비우면 못 하게 되는 일만 안내
          hint={liters === '' ? '비우면 이번 구간의 연비를 계산할 수 없습니다.' : undefined}
        >
          <Input
            id="fuel-liters"
            type="number"
            min={0.01}
            max={9999.99}
            step={0.01}
            className="tabular-nums"
            placeholder="32.45"
            value={liters}
            onChange={(event) => setLiters(event.target.value)}
          />
        </Field>

        <Field
          label="결제 금액 (원)"
          htmlFor="fuel-cost"
          hint={
            pricePerLiter !== null
              ? `리터당 약 ${pricePerLiter.toLocaleString()}원`
              : totalCost === ''
                ? '비우면 유류비 합계에서 빠집니다.'
                : undefined
          }
        >
          <Input
            id="fuel-cost"
            type="number"
            min={0}
            max={MAX_AMOUNT}
            className="tabular-nums"
            value={totalCost}
            onChange={(event) => setTotalCost(event.target.value)}
          />
        </Field>
      </div>

      <Field label="메모" htmlFor="fuel-memo" hint="선택">
        <Textarea
          id="fuel-memo"
          rows={2}
          maxLength={255}
          placeholder="주유소 이름 등"
          value={memo}
          onChange={(event) => setMemo(event.target.value)}
        />
      </Field>

      {error !== null && <ErrorText message={error} />}

      <FormActions>
        <Button type="submit" disabled={pending}>
          {pending ? '저장 중…' : record === null ? '등록' : '수정'}
        </Button>
        <Button type="button" variant="ghost" onClick={onCancel}>
          취소
        </Button>
      </FormActions>
    </form>
  )
}
