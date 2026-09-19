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
import {
  registerFuelRecord,
  updateFuelRecord,
} from '@/features/fuel/api/endpoints/endpoints'
import type { FuelRecordResponse } from '@/features/fuel/api/types/types'

/**
 * 등록·수정 겸용. record 가 null 이면 등록
 * 필드 구성이 같은데 파일을 나누면 한쪽만 고치게 됨 (MaintenanceForm 과 같은 이유)
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
  // 숫자도 문자열 보관 — 입력 도중의 빈 문자열을 숫자로 표현할 수 없음
  const [fueledAt, setFueledAt] = useState(record?.fueledAt ?? todayString())
  const [odometer, setOdometer] = useState(String(record?.odometer ?? defaultOdometer))
  const [liters, setLiters] = useState(record === null ? '' : String(record.liters))
  const [totalCost, setTotalCost] = useState(record === null ? '' : String(record.totalCost))
  const [memo, setMemo] = useState(record?.memo ?? '')
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  // 입력 중 단가 표시. 영수증과 대조해 오타를 그 자리에서 잡기 위함
  const litersValue = Number(liters)
  const costValue = Number(totalCost)
  const pricePerLiter =
    liters !== '' && totalCost !== '' && litersValue > 0
      ? Math.round(costValue / litersValue)
      : null

  // 차량 주행거리보다 작으면 과거 기록
  // liftOdometerTo 덕에 그 값이 "지금까지 기록된 최댓값"이라 비교 하나로 자리수 오타가 걸림 (새 API 불필요)
  // 막지 않고 안내만 — 지난달 영수증 정리는 정상적인 사용이고 계기판 교체도 있음
  const odometerValue = Number(odometer)
  const looksPast =
    odometer !== '' && Number.isFinite(odometerValue) && odometerValue < defaultOdometer

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    /*
     * 등록 폼은 주행거리를 차량의 현재 값으로 미리 채운다. 계기판을 보고 고쳐 쓰라는 뜻인데,
     * 그대로 두고 저장하면 **아무 계산도 일어나지 않는다**:
     *   · 차량 주행거리는 liftOdometerTo 때문에 "지금까지 기록된 최댓값"이라 구간 거리가 0 →
     *     이번 구간의 연비가 나오지 않는다
     *   · liftOdometerTo 도 같은 값이면 올리지 않으므로 차량 쪽도 그대로다
     * 막지는 않는다 — 주행거리를 정말 모르고 지출만 남기려는 경우도 있다. 대신 묻는다.
     */
    if (record === null && Number(odometer) === defaultOdometer) {
      const confirmed = window.confirm(
        `주행거리가 차량의 현재 값(${formatKm(defaultOdometer)})과 같습니다.\n` +
          '이대로 저장하면 이번 구간의 연비가 계산되지 않고, 차량 주행거리도 올라가지 않습니다.\n\n' +
          '계기판 숫자로 고치지 않고 계속할까요?',
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
        // 바뀐 필드만. 값 비교로 판단하는 이유 — 0 으로 바꾸는 것과 안 보내는 것은 다름
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
          /*
            도움말이 상태에 따라 셋으로 갈린다. 비었을 때를 맨 앞에 두는 이유 —
            주행거리는 이 앱에서 연비의 재료이지 기록의 장식이 아니다.
            required 가 저장을 막아 주기는 하지만, 브라우저 기본 문구("이 입력란을
            작성하세요")는 **왜 필요한지** 말해 주지 않는다. 그 이유를 여기서 말한다.
          */
          hint={
            odometer === ''
              ? '주행거리를 적지 않으면 연비를 계산할 수 없습니다.'
              : looksPast
                ? `차량에 기록된 ${defaultOdometer.toLocaleString()}km 보다 작습니다. 과거 기록이면 그대로 두세요.`
                : '계기판 숫자. 이 값이 차량 주행거리보다 크면 차량 쪽도 함께 올라갑니다.'
          }
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
        {/* step 0.01 — 백엔드가 소수 2자리까지만 받음 */}
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
