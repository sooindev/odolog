import { useState } from 'react'
import type { FormEvent } from 'react'
import { ChevronDown } from 'lucide-react'
import { cn } from 'cn'

import { Button } from '@/shared/ui/base/button'
import { controlClassName } from '@/shared/ui/form/control'
import { Field } from '@/shared/ui/form/field'
import { FormActions } from '@/shared/ui/layout/page'
import { Input } from '@/shared/ui/base/input'
import { DateInput } from '@/shared/ui/form/date-input'
import { Textarea } from '@/shared/ui/base/textarea'
import { ErrorText } from '@/shared/ui/feedback/state'
import { ApiError } from '@/shared/api/client/client'
import { formatKm, todayString } from '@/shared/lib/format/format'
import { MAX_AMOUNT, MAX_ODOMETER } from '@/shared/lib/limits/limits'
import { looksBigJump, looksPast } from '@/shared/lib/odometer/odometer'
import { registerRecord, updateRecord } from '@/features/maintenance/api/endpoints/endpoints'
import { SERVICE_TYPE_GROUPS, SERVICE_TYPE_LABELS } from '@/features/maintenance/api/types/types'
import type {
  MaintenanceRecordResponse,
  MaintenanceRecordUpdateRequest,
  ServiceType,
} from '@/features/maintenance/api/types/types'

interface Props {
  vehicleId: string
  /** null 이면 등록, 값이 있으면 수정 */
  record: MaintenanceRecordResponse | null
  /** 등록 시 주행거리 기본값(차량의 현재 값) */
  defaultOdometer: number
  /** 저장한 종류 전달. 목록 필터 해제 판단용 */
  onSaved: (savedType: ServiceType) => void
  onCancel: () => void
}

export function MaintenanceForm({ vehicleId, record, defaultOdometer, onSaved, onCancel }: Props) {
  // 폼을 연 시점의 차량 주행거리 고정. 입력칸과 판정 기준의 어긋남 방지
  const [baseOdometer] = useState(defaultOdometer)

  const [type, setType] = useState<ServiceType>(record?.type ?? 'ENGINE_OIL')
  const [description, setDescription] = useState(record?.description ?? '')
  const [cost, setCost] = useState(String(record?.cost ?? 0))
  const [serviceOdometer, setServiceOdometer] = useState(
    String(record?.serviceOdometer ?? defaultOdometer),
  )
  const [serviceDate, setServiceDate] = useState(record?.serviceDate ?? todayString())
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  // 주행거리 판정 규칙은 shared/lib/odometer. 안내만
  const odometerValue = Number(serviceOdometer)
  const past = serviceOdometer !== '' && looksPast(odometerValue, baseOdometer)
  const bigJump = serviceOdometer !== '' && looksBigJump(odometerValue, baseOdometer)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    // 급증은 되돌리기 어려움. force 정정으로만 복구
    if (bigJump) {
      const confirmed = window.confirm(
        `주행거리가 ${formatKm(baseOdometer)} 에서 ${formatKm(odometerValue)} 로 크게 뜁니다.\n` +
          '자리수가 틀리면 차량 주행거리가 그 값에 묶입니다.\n\n이대로 저장할까요?',
      )
      if (!confirmed) {
        return
      }
    }

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
        // 바뀐 필드만
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

      onSaved(type)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : '저장에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    // 한 겹 안쪽 면(sunken). 카드 중첩 대신
    <form
      className="flex flex-col gap-5 border border-border bg-sunken p-4 sm:gap-6 sm:p-6"
      onSubmit={handleSubmit}
    >
      {/* 짝이 되는 값끼리 2열. sm 미만은 1열 */}
      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="정비 종류" htmlFor="type">
          {/*
            네이티브 select. 모바일 OS 선택 UI 사용
            controlClassName 으로 입력창과 높이·포커스 통일, 펼침 목록은 color-scheme 이 테마 반영
          */}
          <div className="relative">
            <select
              id="type"
              // 정비 종류에 첫 포커스
              autoFocus
              // appearance-none: OS 기본 화살표 제거 후 같은 톤 화살표
              className={cn(controlClassName, 'appearance-none pr-10')}
              value={type}
              onChange={(event) => setType(event.target.value as ServiceType)}
            >
              {/* optgroup 으로 부위별 묶음 */}
              {SERVICE_TYPE_GROUPS.map((group) => (
                <optgroup key={group.label} label={group.label}>
                  {group.types.map((serviceType) => (
                    <option key={serviceType} value={serviceType}>
                      {SERVICE_TYPE_LABELS[serviceType]}
                    </option>
                  ))}
                </optgroup>
              ))}
            </select>

            {/* pointer-events-none: select 클릭 가로채기 방지 */}
            <ChevronDown
              className="pointer-events-none absolute top-1/2 right-3.5 size-4 -translate-y-1/2 text-muted-foreground"
              aria-hidden="true"
            />
          </div>
        </Field>

        <Field label="정비 날짜" htmlFor="serviceDate">
          {/* YYYY-MM-DD 문자열. 터치면 드럼 휠, 아니면 네이티브 date */}
          <DateInput id="serviceDate" required value={serviceDate} onChange={setServiceDate} />
        </Field>
      </div>

      <div className="grid gap-5 sm:grid-cols-2">
        <Field
          label="정비 시 주행거리 (km)"
          htmlFor="serviceOdometer"
          // 비었을 때 안내 우선. 다음 정비 계산의 재료
          hint={
            serviceOdometer === ''
              ? '주행거리를 적지 않으면 다음 정비 시점을 계산할 수 없습니다.'
              : past
                ? `차량에 기록된 ${baseOdometer.toLocaleString()}km 보다 작습니다. 과거 기록이면 그대로 두세요.`
                : undefined
          }
        >
          <Input
            id="serviceOdometer"
            type="number"
            required
            min={0}
            max={MAX_ODOMETER}
            className="tabular-nums"
            value={serviceOdometer}
            onChange={(event) => setServiceOdometer(event.target.value)}
          />
        </Field>

        <Field label="비용 (원)" htmlFor="cost">
          <Input
            id="cost"
            type="number"
            required
            min={0}
            max={MAX_AMOUNT}
            className="tabular-nums"
            value={cost}
            onChange={(event) => setCost(event.target.value)}
          />
        </Field>
      </div>

      <Field label="메모" htmlFor="description" hint="선택">
        <Textarea
          id="description"
          rows={2}
          maxLength={255}
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
