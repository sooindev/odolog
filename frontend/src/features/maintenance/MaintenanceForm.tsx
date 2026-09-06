import { useState } from 'react'
import type { FormEvent } from 'react'

import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { Textarea } from '@/shared/ui/textarea'
import { ErrorText } from '@/shared/ui/state'
import { ApiError } from '@/shared/api/client'
import { todayString } from '@/shared/lib/format'
import { registerRecord, updateRecord } from '@/features/maintenance/api'
import { SERVICE_TYPES, SERVICE_TYPE_LABELS } from '@/shared/api/types'
import type {
  MaintenanceRecordResponse,
  MaintenanceRecordUpdateRequest,
  ServiceType,
} from '@/shared/api/types'

interface Props {
  vehicleId: number
  /** null이면 새 이력 등록, 값이 있으면 그 이력 수정 */
  record: MaintenanceRecordResponse | null
  /** 등록 폼의 주행거리 기본값 (차량의 현재 주행거리) */
  defaultOdometer: number
  onSaved: () => void
  onCancel: () => void
}

export function MaintenanceForm({ vehicleId, record, defaultOdometer, onSaved, onCancel }: Props) {
  const [type, setType] = useState<ServiceType>(record?.type ?? 'ENGINE_OIL')
  const [description, setDescription] = useState(record?.description ?? '')
  const [cost, setCost] = useState(String(record?.cost ?? 0))
  const [serviceOdometer, setServiceOdometer] = useState(
    String(record?.serviceOdometer ?? defaultOdometer),
  )
  const [serviceDate, setServiceDate] = useState(record?.serviceDate ?? todayString())
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setPending(true)

    try {
      if (record === null) {
        await registerRecord(vehicleId, {
          type,
          description,
          cost: Number(cost),
          serviceOdometer: Number(serviceOdometer),
          serviceDate,
        })
      } else {
        // 백엔드가 "보낸 필드만 변경"이므로 바뀐 것만 담는다.
        // cost를 0으로 바꾸는 것과 안 보내는 것은 다르므로 값 비교로 판단한다.
        const request: MaintenanceRecordUpdateRequest = {}
        if (type !== record.type) request.type = type
        if (description !== (record.description ?? '')) request.description = description
        if (Number(cost) !== record.cost) request.cost = Number(cost)
        if (Number(serviceOdometer) !== record.serviceOdometer) {
          request.serviceOdometer = Number(serviceOdometer)
        }
        if (serviceDate !== record.serviceDate) request.serviceDate = serviceDate

        await updateRecord(vehicleId, record.id, request)
      }

      onSaved()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : '저장에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    <form className="flex flex-col gap-3" onSubmit={handleSubmit}>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="type">정비 종류</Label>
        {/*
          shadcn Select 대신 브라우저 기본 <select>를 쓴다.
          선택지가 5개뿐이라 커스텀 드롭다운의 복잡한 구조가 필요 없고,
          모바일에서는 OS 기본 선택 UI가 뜨는 게 오히려 편하다.
        */}
        <select
          id="type"
          className="border-input h-8 w-full rounded-lg border bg-transparent px-2.5 text-sm"
          value={type}
          onChange={(event) => setType(event.target.value as ServiceType)}
        >
          {SERVICE_TYPES.map((serviceType) => (
            <option key={serviceType} value={serviceType}>
              {SERVICE_TYPE_LABELS[serviceType]}
            </option>
          ))}
        </select>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="serviceDate">정비 날짜</Label>
          {/* type="date"는 값을 YYYY-MM-DD 문자열로 준다. 백엔드 LocalDate와 그대로 맞는다. */}
          <Input
            id="serviceDate"
            type="date"
            required
            value={serviceDate}
            onChange={(event) => setServiceDate(event.target.value)}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <Label htmlFor="serviceOdometer">정비 시 주행거리 (km)</Label>
          <Input
            id="serviceOdometer"
            type="number"
            required
            min={0}
            value={serviceOdometer}
            onChange={(event) => setServiceOdometer(event.target.value)}
          />
        </div>
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="cost">비용 (원)</Label>
        <Input
          id="cost"
          type="number"
          min={0}
          value={cost}
          onChange={(event) => setCost(event.target.value)}
        />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="description">메모</Label>
        <Textarea
          id="description"
          rows={2}
          maxLength={200}
          value={description}
          onChange={(event) => setDescription(event.target.value)}
        />
      </div>

      {error !== null && <ErrorText message={error} />}

      <div className="flex gap-2">
        <Button type="submit" disabled={pending}>
          {pending ? '저장 중…' : record === null ? '등록' : '수정'}
        </Button>
        <Button type="button" variant="outline" onClick={onCancel}>
          취소
        </Button>
      </div>
    </form>
  )
}
