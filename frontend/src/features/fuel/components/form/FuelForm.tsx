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
import { todayString } from '@/shared/lib/format/format'
import {
  registerFuelRecord,
  updateFuelRecord,
} from '@/features/fuel/api/endpoints/endpoints'
import type { FuelRecordResponse } from '@/features/fuel/api/types/types'

/**
 * 등록·수정 겸용. record 가 null 이면 등록이다.
 * 필드 구성이 같은데 파일을 둘로 나누면 한쪽만 고치는 실수가 생긴다 (MaintenanceForm 과 같은 이유).
 */
export function FuelForm({
  vehicleId,
  record,
  defaultOdometer,
  onSaved,
  onCancel,
}: {
  vehicleId: number
  record: FuelRecordResponse | null
  defaultOdometer: number
  onSaved: () => void
  onCancel: () => void
}) {
  // 숫자도 문자열로 들고 있는다. 입력 도중의 빈 문자열을 숫자로 표현할 방법이 없다.
  const [fueledAt, setFueledAt] = useState(record?.fueledAt ?? todayString())
  const [odometer, setOdometer] = useState(String(record?.odometer ?? defaultOdometer))
  const [liters, setLiters] = useState(record === null ? '' : String(record.liters))
  const [totalCost, setTotalCost] = useState(record === null ? '' : String(record.totalCost))
  const [memo, setMemo] = useState(record?.memo ?? '')
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  // 입력 중에도 단가를 보여준다. 영수증과 대조할 수 있어 오타를 그 자리에서 잡는다.
  const litersValue = Number(liters)
  const costValue = Number(totalCost)
  const pricePerLiter =
    liters !== '' && totalCost !== '' && litersValue > 0
      ? Math.round(costValue / litersValue)
      : null

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
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
        // 바뀐 필드만 보낸다. 값 비교로 판단하는 이유: 비용을 0으로 바꾸는 것과
        // 안 보내는 것은 다르다.
        await updateFuelRecord(vehicleId, record.id, {
          fueledAt: fueledAt === record.fueledAt ? undefined : fueledAt,
          odometer: Number(odometer) === record.odometer ? undefined : Number(odometer),
          liters: litersValue === record.liters ? undefined : litersValue,
          totalCost: costValue === record.totalCost ? undefined : costValue,
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
          hint="계기판 숫자. 이 값이 차량 주행거리보다 크면 차량 쪽도 함께 올라갑니다."
        >
          <Input
            id="fuel-odometer"
            type="number"
            required
            min={0}
            className="tabular-nums"
            value={odometer}
            onChange={(event) => setOdometer(event.target.value)}
          />
        </Field>
      </div>

      <div className="grid gap-5 sm:grid-cols-2">
        {/* step 0.01 — 백엔드가 소수 둘째 자리까지만 받는다(@Digits fraction = 2). */}
        <Field label="주유량 (L)" htmlFor="fuel-liters">
          <Input
            id="fuel-liters"
            type="number"
            required
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
          hint={pricePerLiter === null ? undefined : `리터당 약 ${pricePerLiter.toLocaleString()}원`}
        >
          <Input
            id="fuel-cost"
            type="number"
            required
            min={0}
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
