import { useState } from 'react'
import type { FormEvent } from 'react'

import { Button } from '@/shared/ui/base/button'
import { NativeSelect } from '@/shared/ui/form/native-select'
import { Field } from '@/shared/ui/form/field'
import { FormActions } from '@/shared/ui/layout/page'
import { Input } from '@/shared/ui/base/input'
import { DateInput } from '@/shared/ui/form/date-input'
import { Textarea } from '@/shared/ui/base/textarea'
import { ErrorText } from '@/shared/ui/state'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import { todayString } from '@/shared/lib/format'
import { MAX_ODOMETER } from '@/shared/lib/limits'
import { fromMinor, inputStep, maxMajor, toMinor } from '@/shared/lib/money'
import { looksBigJump, looksPast } from '@/shared/lib/odometer'
import { fromKm, toKm } from '@/shared/lib/units'
import { registerRecord, updateRecord } from '@/features/maintenance/api/endpoints'
import { SERVICE_TYPE_GROUPS } from '@/features/maintenance/api/types'
import type {
  MaintenanceRecordResponse,
  MaintenanceRecordUpdateRequest,
  ServiceType,
} from '@/features/maintenance/api/types'

interface Props {
  vehicleId: string
  /** null 이면 등록, 값이 있으면 수정 */
  record: MaintenanceRecordResponse | null
  /** 등록 시 주행거리 기본값(차량의 현재 값, km) */
  defaultOdometer: number
  /** 저장한 종류 전달. 목록 필터 해제 판단용 */
  onSaved: (savedType: ServiceType) => void
  onCancel: () => void
}

export function MaintenanceForm({ vehicleId, record, defaultOdometer, onSaved, onCancel }: Props) {
  const { t, f, unitSystem, timeZone, currency: userCurrency } = useI18n()

  // 폼을 연 시점의 차량 주행거리 고정(km). 입력칸과 판정 기준의 어긋남 방지
  const [baseOdometer] = useState(defaultOdometer)
  // 수정은 기록의 통화, 등록은 지금 사용자 통화. 서버도 같은 규칙
  const currency = record?.currency ?? userCurrency

  // 입력칸은 화면 단위(마일·달러). 처음 값 문자열을 기억해 손대지 않은 칸은 저장값 그대로 전송
  // 비용·주행거리는 모르면 빈칸(null). 등록은 주행거리만 차량 값으로 미리 채움
  const initialOdometerKm = record === null ? defaultOdometer : record.serviceOdometer
  const [initialOdometer] = useState(
    initialOdometerKm === null ? '' : String(Math.round(fromKm(unitSystem, initialOdometerKm))),
  )
  const [initialCost] = useState(
    record === null || record.cost === null ? '' : String(fromMinor(record.cost, currency)),
  )

  const [type, setType] = useState<ServiceType>(record?.type ?? 'ENGINE_OIL')
  const [description, setDescription] = useState(record?.description ?? '')
  const [cost, setCost] = useState(initialCost)
  const [serviceOdometer, setServiceOdometer] = useState(initialOdometer)
  const [serviceDate, setServiceDate] = useState(record?.serviceDate ?? todayString(timeZone))
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  // 판정·전송은 km. 손대지 않았으면 원래 km 그대로(마일 왕복 오차 차단). 빈칸은 null(Number('') 는 0)
  const odometerKm =
    serviceOdometer === ''
      ? null
      : serviceOdometer === initialOdometer
        ? initialOdometerKm
        : toKm(unitSystem, Number(serviceOdometer))
  const costMinor =
    cost === '' ? null : cost === initialCost ? (record?.cost ?? null) : toMinor(Number(cost), currency)

  // 주행거리 판정 규칙은 shared/lib/odometer. 안내만
  const past = odometerKm !== null && looksPast(odometerKm, baseOdometer)
  const bigJump = odometerKm !== null && looksBigJump(odometerKm, baseOdometer)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    // 급증은 되돌리기 어려움. force 정정으로만 복구
    if (bigJump) {
      const confirmed = window.confirm(
        t.odometerHints.bigJumpConfirm(f.distance(baseOdometer), f.distance(odometerKm ?? 0)),
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
          cost: costMinor,
          serviceOdometer: odometerKm,
          serviceDate,
        })
      } else {
        // 바뀐 필드만
        const request: MaintenanceRecordUpdateRequest = {}
        if (type !== record.type) request.type = type
        if (description !== (record.description ?? '')) request.description = description
        // 비움은 clear 플래그. null 을 보내면 '안 보냄' 과 구분되지 않음
        if (costMinor !== record.cost) {
          if (costMinor === null) request.clearCost = true
          else request.cost = costMinor
        }
        if (odometerKm !== record.serviceOdometer) {
          if (odometerKm === null) request.clearServiceOdometer = true
          else request.serviceOdometer = odometerKm
        }
        if (serviceDate !== record.serviceDate) request.serviceDate = serviceDate

        // 변경 없으면 요청 생략
        if (Object.keys(request).length === 0) {
          onCancel()
          return
        }

        await updateRecord(vehicleId, record.id, request)
      }

      onSaved(type)
    } catch (caught) {
      setError(errorMessage(caught, t, t.maintenance.form.failed))
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
        <Field label={t.maintenance.form.type} htmlFor="type">
          {/* 정비 종류에 첫 포커스 */}
          <NativeSelect id="type" autoFocus value={type} onChange={(next) => setType(next as ServiceType)}>
            {/* optgroup 으로 부위별 묶음 */}
            {SERVICE_TYPE_GROUPS.map((group) => (
              <optgroup key={group.key} label={t.serviceTypeGroups[group.key]}>
                {group.types.map((serviceType) => (
                  <option key={serviceType} value={serviceType}>
                    {t.serviceTypes[serviceType]}
                  </option>
                ))}
              </optgroup>
            ))}
          </NativeSelect>
        </Field>

        <Field label={t.maintenance.form.date} htmlFor="serviceDate">
          {/* YYYY-MM-DD 문자열. 터치면 드럼 휠, 아니면 네이티브 date */}
          <DateInput id="serviceDate" required value={serviceDate} onChange={setServiceDate} />
        </Field>
      </div>

      <div className="grid gap-5 sm:grid-cols-2">
        <Field
          label={t.maintenance.form.odometer(f.distanceUnit)}
          htmlFor="serviceOdometer"
          // 비었을 때 안내 우선. 다음 정비 계산의 재료
          hint={
            serviceOdometer === ''
              ? t.maintenance.form.odometerEmpty
              : past
                ? t.odometerHints.past(f.distance(baseOdometer))
                : undefined
          }
        >
          <Input
            id="serviceOdometer"
            type="number"
            min={0}
            max={Math.floor(fromKm(unitSystem, MAX_ODOMETER))}
            className="tabular-nums"
            value={serviceOdometer}
            onChange={(event) => setServiceOdometer(event.target.value)}
          />
        </Field>

        {/* 선택 입력. 빈칸은 0 이 아니라 null 로 보냄(0원은 다른 사실) */}
        <Field
          label={t.maintenance.form.cost(currency)}
          htmlFor="cost"
          hint={cost === '' ? t.maintenance.form.costEmpty : undefined}
        >
          <Input
            id="cost"
            type="number"
            min={0}
            max={maxMajor(currency)}
            step={inputStep(currency)}
            className="tabular-nums"
            value={cost}
            onChange={(event) => setCost(event.target.value)}
          />
        </Field>
      </div>

      <Field label={t.maintenance.form.memo} htmlFor="description" hint={t.common.optional}>
        <Textarea
          id="description"
          rows={2}
          maxLength={255}
          placeholder={t.maintenance.form.memoPlaceholder}
          value={description}
          onChange={(event) => setDescription(event.target.value)}
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
