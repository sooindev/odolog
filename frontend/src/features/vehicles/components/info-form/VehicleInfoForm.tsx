import { useState } from 'react'
import type { FormEvent } from 'react'

import { Button } from '@/shared/ui/base/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { Field } from '@/shared/ui/form/field'
import { Input } from '@/shared/ui/base/input'
import { FormActions } from '@/shared/ui/layout/page'
import { ErrorText } from '@/shared/ui/feedback/state'
import { ApiError } from '@/shared/api/client/client'
import { updateVehicle } from '@/features/vehicles/api/endpoints/endpoints'
import type {
  VehicleResponse,
  VehicleUpdateRequest,
} from '@/features/vehicles/api/types/types'

/**
 * 차량 정보(번호판·제조사·모델·연식) 수정.
 *
 * 주행거리는 여기 없다. 감소 금지 규칙이 붙어 있어 백엔드도 엔드포인트가 따로고,
 * 화면에서도 "정보를 고치는 순간"과 "주행거리를 적는 순간"은 서로 다르다.
 *
 * 닫힌 동안에는 값을 그대로 보여준다. 머리말에도 같은 값이 있지만 거기서는 제목·분류로
 * 흩어져 있고, 여기서는 "고칠 수 있는 항목 넷"으로 나란히 선다.
 */
export function VehicleInfoForm({
  vehicle,
  onUpdated,
}: {
  vehicle: VehicleResponse
  onUpdated: (vehicle: VehicleResponse) => void
}) {
  const [open, setOpen] = useState(false)

  return (
    <Card size="sm">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>차량 정보</CardTitle>
        {!open && (
          <Button size="sm" variant="secondary" onClick={() => setOpen(true)}>
            수정
          </Button>
        )}
      </CardHeader>

      <CardContent>
        {open ? (
          // 칸이 열리고 안쪽 내용이 0.12s 늦게 들어온다. 닫을 때는 연출하지 않는다.
          <div className="form-open">
            <div>
              <EditForm
                vehicle={vehicle}
                onUpdated={(updated) => {
                  onUpdated(updated)
                  setOpen(false)
                }}
                onCancel={() => setOpen(false)}
              />
            </div>
          </div>
        ) : (
          <dl className="flex flex-col gap-3">
            <InfoRow label="번호판" value={vehicle.plateNumber} />
            <InfoRow label="제조사" value={vehicle.manufacturer} />
            <InfoRow label="모델" value={vehicle.modelName} />
            <InfoRow
              label="연식"
              value={vehicle.modelYear === null ? '미상' : `${vehicle.modelYear}년`}
            />
          </dl>
        )}
      </CardContent>
    </Card>
  )
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <dt className="text-muted-foreground">{label}</dt>
      {/* min-w-0 + truncate: 긴 모델명 한 줄이 라벨을 밀어내지 않게 한다. */}
      <dd className="min-w-0 truncate text-right text-strong">{value}</dd>
    </div>
  )
}

function EditForm({
  vehicle,
  onUpdated,
  onCancel,
}: {
  vehicle: VehicleResponse
  onUpdated: (vehicle: VehicleResponse) => void
  onCancel: () => void
}) {
  // 숫자도 문자열로 들고 있는다. 입력 도중의 빈 문자열을 숫자로 표현할 방법이 없다.
  const [plateNumber, setPlateNumber] = useState(vehicle.plateNumber)
  const [manufacturer, setManufacturer] = useState(vehicle.manufacturer)
  const [modelName, setModelName] = useState(vehicle.modelName)
  const [modelYear, setModelYear] = useState(
    vehicle.modelYear === null ? '' : String(vehicle.modelYear),
  )
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    // 바뀐 필드만 담는다. 백엔드가 "보낸 필드만 변경"이라 안 바뀐 값을 같이 보낼 이유가 없고,
    // 번호판을 그대로 다시 보내면 중복 검사를 한 번 더 돌게 만든다.
    const request: VehicleUpdateRequest = {}
    if (plateNumber !== vehicle.plateNumber) request.plateNumber = plateNumber
    if (manufacturer !== vehicle.manufacturer) request.manufacturer = manufacturer
    if (modelName !== vehicle.modelName) request.modelName = modelName
    if (modelYear !== '' && Number(modelYear) !== vehicle.modelYear) {
      request.modelYear = Number(modelYear)
    }

    // 아무것도 안 바꿨으면 요청을 보내지 않는다. 빈 PATCH 는 서버가 할 일이 없다.
    if (Object.keys(request).length === 0) {
      onCancel()
      return
    }

    setPending(true)

    try {
      onUpdated(await updateVehicle(vehicle.id, request))
    } catch (caught) {
      // 409 는 같은 번호판을 이미 등록한 경우. 소유자별 유니크라 남의 차량은 걸리지 않는다.
      setError(
        caught instanceof ApiError ? caught.message : '차량 정보 수정에 실패했습니다.',
      )
    } finally {
      setPending(false)
    }
  }

  return (
    <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
      <Field label="번호판" htmlFor="edit-plate-number">
        <Input
          id="edit-plate-number"
          required
          maxLength={20}
          value={plateNumber}
          onChange={(event) => setPlateNumber(event.target.value)}
        />
      </Field>

      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="제조사" htmlFor="edit-manufacturer">
          <Input
            id="edit-manufacturer"
            required
            maxLength={50}
            value={manufacturer}
            onChange={(event) => setManufacturer(event.target.value)}
          />
        </Field>

        <Field label="모델" htmlFor="edit-model-name">
          <Input
            id="edit-model-name"
            required
            maxLength={100}
            value={modelName}
            onChange={(event) => setModelName(event.target.value)}
          />
        </Field>
      </div>

      <Field label="연식" htmlFor="edit-model-year">
        <Input
          id="edit-model-year"
          type="number"
          required
          min={1900}
          max={2100}
          className="tabular-nums"
          value={modelYear}
          onChange={(event) => setModelYear(event.target.value)}
        />
      </Field>

      {error !== null && <ErrorText message={error} />}

      <FormActions>
        <Button type="submit" disabled={pending}>
          {pending ? '저장 중…' : '저장'}
        </Button>
        <Button type="button" variant="ghost" onClick={onCancel}>
          취소
        </Button>
      </FormActions>
    </form>
  )
}
