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
import { todayString } from '@/shared/lib/format/format'
import { MAX_AMOUNT, MAX_ODOMETER } from '@/shared/lib/limits/limits'
import { registerRecord, updateRecord } from '@/features/maintenance/api/endpoints/endpoints'
import { SERVICE_TYPE_GROUPS, SERVICE_TYPE_LABELS } from '@/features/maintenance/api/types/types'
import type {
  MaintenanceRecordResponse,
  MaintenanceRecordUpdateRequest,
  ServiceType,
} from '@/features/maintenance/api/types/types'

interface Props {
  vehicleId: number
  /** null 이면 등록, 값이 있으면 그 이력 수정 */
  record: MaintenanceRecordResponse | null
  /** 등록 시 주행거리 기본값 (차량의 현재 주행거리) */
  defaultOdometer: number
  onSaved: () => void
  onCancel: () => void
}

export function MaintenanceForm({ vehicleId, record, defaultOdometer, onSaved, onCancel }: Props) {
  /*
   * 폼이 열린 시점의 차량 주행거리를 붙잡아 둔다.
   * props 를 그대로 쓰면 입력칸(state)은 열 때의 값인데 판정 기준만 최신으로 갱신되어,
   * 폼이 열려 있는 동안 다른 카드에서 차량 값이 오르면 경고가 어긋난다.
   * key 로 폼을 재생성하는 방법은 쓸 수 없다 — 입력 중인 내용이 날아간다.
   */
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

  // 주유 폼과 같은 안내. 차량 주행거리는 지금까지 기록된 최댓값이라 그보다 작으면 과거 기록
  // 막지 않고 안내만
  const odometerValue = Number(serviceOdometer)
  const looksPast =
    serviceOdometer !== '' &&
    Number.isFinite(odometerValue) &&
    odometerValue < baseOdometer

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
        // 바뀐 필드만. 0 으로 바꾸는 것과 안 보내는 것이 달라 값 비교로 판단
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
    // 폼은 한 겹 안쪽 면. 카드 안에 카드 대신 배경 농도만 낮춰 선을 늘리지 않고 층을 만듦
    <form
      className="flex flex-col gap-5 border border-border bg-sunken p-4 sm:gap-6 sm:p-6"
      onSubmit={handleSubmit}
    >
      {/* 한 줄에 하나씩 쌓으면 폼이 실제보다 길어 보이고 오른쪽이 빔
          짝이 되는 값끼리 2열로. sm 미만에서는 자동으로 풀림 */}
      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="정비 종류" htmlFor="type">
          {/*
            shadcn Select 대신 브라우저 기본 select
            커스텀 드롭다운의 구조가 필요 없고, 모바일에서 OS 기본 선택 UI 가 뜨는 편이 나음
            input 과 같은 controlClassName 으로 높이·포커스 반응을 맞춤
            펼침 목록 색은 CSS 로 못 건드리지만 color-scheme 덕에 브라우저가 테마에 맞게 그림
          */}
          <div className="relative">
            <select
              id="type"
              // 폼을 열면 종류부터 고른다. 나머지 칸은 기본값이 쓸 만하게 채워져 있다
              autoFocus
              // appearance-none — OS 기본 화살표 제거. 색을 못 바꿔 테마와 따로 놀아서
              // 같은 톤의 화살표를 직접 얹음
              className={cn(controlClassName, 'appearance-none pr-10')}
              value={type}
              onChange={(event) => setType(event.target.value as ServiceType)}
            >
              {/*
                optgroup 으로 묶기. 15개는 평평한 목록으로 훑어 찾기 어려움
                구역 제목 생김새는 브라우저·OS 마다 다르지만 기본 선택 UI 의 이점이 더 큼
              */}
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

            {/* pointer-events-none — 아이콘이 클릭을 가로채면 select 가 안 열림 */}
            <ChevronDown
              className="pointer-events-none absolute top-1/2 right-3.5 size-4 -translate-y-1/2 text-muted-foreground"
              aria-hidden="true"
            />
          </div>
        </Field>

        <Field label="정비 날짜" htmlFor="serviceDate">
          {/* 값은 어느 쪽이든 YYYY-MM-DD 문자열이라 백엔드 LocalDate 와 그대로 맞음
              터치면 드럼 휠, 아니면 네이티브 date */}
          <DateInput id="serviceDate" required value={serviceDate} onChange={setServiceDate} />
        </Field>
      </div>

      <div className="grid gap-5 sm:grid-cols-2">
        <Field
          label="정비 시 주행거리 (km)"
          htmlFor="serviceOdometer"
          /*
            주유 폼과 같은 방식. 비었을 때를 맨 앞에 둔다 — 이 값은 다음 정비 시점의 재료라
            없으면 그 종류의 주기 계산이 아예 안 된다.
            required 가 저장을 막지만 브라우저 기본 문구는 왜 필요한지 말해 주지 않는다.
          */
          hint={
            serviceOdometer === ''
              ? '주행거리를 적지 않으면 다음 정비 시점을 계산할 수 없습니다.'
              : looksPast
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
