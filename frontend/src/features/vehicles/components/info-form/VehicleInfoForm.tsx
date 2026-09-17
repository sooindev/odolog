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
 * 차량 정보(번호판·제조사·모델·연식) 수정
 * 주행거리 제외 — 감소 금지 규칙이 붙어 엔드포인트가 따로고 쓰는 순간도 다름
 * 닫힌 동안에는 값 4개를 그대로 표시. 머리말에도 같은 값이 있지만 거기서는 제목·분류로 흩어져 있음
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
          // 칸이 열리고 안쪽 내용은 0.12s 지연. 닫을 때는 연출 없음
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
      {/* min-w-0 + truncate — 긴 모델명이 라벨을 밀어내지 않게 */}
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
  // 숫자도 문자열 보관 — 입력 도중의 빈 문자열을 숫자로 표현할 수 없음
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

    // 바뀐 필드만. 번호판을 그대로 다시 보내면 중복 검사가 한 번 더 돎
    const request: VehicleUpdateRequest = {}
    if (plateNumber !== vehicle.plateNumber) request.plateNumber = plateNumber
    if (manufacturer !== vehicle.manufacturer) request.manufacturer = manufacturer
    if (modelName !== vehicle.modelName) request.modelName = modelName
    if (modelYear !== '' && Number(modelYear) !== vehicle.modelYear) {
      request.modelYear = Number(modelYear)
    }

    // 변경이 없으면 요청 생략
    if (Object.keys(request).length === 0) {
      onCancel()
      return
    }

    setPending(true)

    try {
      onUpdated(await updateVehicle(vehicle.id, request))
    } catch (caught) {
      // 409 = 내가 이미 등록한 번호판. 소유자별 유니크라 남의 차량은 안 걸림
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

      {/*
        required 제외. 연식이 없는 차량(@NotNull 이전 데이터)은 이 칸이 비어서 시작하는데,
        required 면 모르는 연식을 지어내야만 제조사 오타를 고칠 수 있음
        비워 두면 request 에 안 담기고, 부분 수정에서 "안 보냄"은 "그대로 둠"
      */}
      <Field label="연식" htmlFor="edit-model-year" hint="비워 두면 바꾸지 않습니다">
        <Input
          id="edit-model-year"
          type="number"
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
