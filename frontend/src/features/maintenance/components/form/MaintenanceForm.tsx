import { useState } from 'react'
import type { FormEvent } from 'react'
import { ChevronDown } from 'lucide-react'
import { cn } from 'cn'

import { Button } from '@/shared/ui/base/button'
import { controlClassName } from '@/shared/ui/form/control'
import { Field } from '@/shared/ui/form/field'
import { FormActions } from '@/shared/ui/layout/page'
import { Input } from '@/shared/ui/base/input'
import { Textarea } from '@/shared/ui/base/textarea'
import { ErrorText } from '@/shared/ui/feedback/state'
import { ApiError } from '@/shared/api/client/client'
import { todayString } from '@/shared/lib/format/format'
import { registerRecord, updateRecord } from '@/features/maintenance/api/endpoints/endpoints'
import { SERVICE_TYPES, SERVICE_TYPE_LABELS } from '@/features/maintenance/api/types/types'
import type {
  MaintenanceRecordResponse,
  MaintenanceRecordUpdateRequest,
  ServiceType,
} from '@/features/maintenance/api/types/types'

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
    // 폼은 목록과 같은 평면에 두지 않고 한 겹 안쪽 면으로 내린다.
    // 카드 안에 또 카드를 넣는 대신 배경 농도만 낮춰, 선을 늘리지 않고 층을 만든다.
    <form
      className="flex flex-col gap-6 border border-border bg-sunken p-6"
      onSubmit={handleSubmit}
    >
      {/* 넓은 열에서 필드를 한 줄에 하나씩 쌓으면 폼이 실제보다 길어 보이고 오른쪽이 빈다.
          짝이 되는 값끼리 2열로 묶는다. sm 미만에서는 자동으로 한 줄씩 풀린다. */}
      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="정비 종류" htmlFor="type">
          {/*
            shadcn Select 대신 브라우저 기본 <select>를 쓴다.
            선택지가 5개뿐이라 커스텀 드롭다운의 복잡한 구조가 필요 없고,
            모바일에서는 OS 기본 선택 UI가 뜨는 게 오히려 편하다.

            <input>과 같은 controlClassName 을 씌워 높이·곡률·포커스 반응을 맞춘다.
            펼쳐지는 목록의 색은 CSS로 못 건드리지만, index.css 가 테마마다 color-scheme 을
            지정해 두어서 브라우저가 알아서 라이트/다크 목록을 그려 준다.
          */}
          <div className="relative">
            <select
              id="type"
              // appearance-none: OS가 그려 주는 기본 화살표를 지운다. 그 화살표는 색을 바꿀 수
              // 없어서 테마와 따로 논다. 대신 아래에 같은 톤의 화살표를 직접 얹는다.
              className={cn(controlClassName, 'appearance-none pr-10')}
              value={type}
              onChange={(event) => setType(event.target.value as ServiceType)}
            >
              {SERVICE_TYPES.map((serviceType) => (
                <option key={serviceType} value={serviceType}>
                  {SERVICE_TYPE_LABELS[serviceType]}
                </option>
              ))}
            </select>

            {/* pointer-events-none: 아이콘이 클릭을 가로채면 select가 열리지 않는다. */}
            <ChevronDown
              className="pointer-events-none absolute top-1/2 right-3.5 size-4 -translate-y-1/2 text-muted-foreground"
              aria-hidden="true"
            />
          </div>
        </Field>

        <Field label="정비 날짜" htmlFor="serviceDate">
          {/* type="date"는 값을 YYYY-MM-DD 문자열로 준다. 백엔드 LocalDate와 그대로 맞는다. */}
          <Input
            id="serviceDate"
            type="date"
            required
            className="tabular-nums"
            value={serviceDate}
            onChange={(event) => setServiceDate(event.target.value)}
          />
        </Field>
      </div>

      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="정비 시 주행거리 (km)" htmlFor="serviceOdometer">
          <Input
            id="serviceOdometer"
            type="number"
            required
            min={0}
            className="tabular-nums"
            value={serviceOdometer}
            onChange={(event) => setServiceOdometer(event.target.value)}
          />
        </Field>

        <Field label="비용 (원)" htmlFor="cost">
          <Input
            id="cost"
            type="number"
            min={0}
            className="tabular-nums"
            value={cost}
            onChange={(event) => setCost(event.target.value)}
          />
        </Field>
      </div>

      <Field label="메모" htmlFor="description" hint="최대 200자">
        <Textarea
          id="description"
          rows={2}
          maxLength={200}
          placeholder="교체한 부품, 정비소 이름 등"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
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
