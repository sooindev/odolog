import { useState } from 'react'
import type { FormEvent } from 'react'

import { Button } from '@/shared/ui/base/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { Field } from '@/shared/ui/form/field'
import { Input } from '@/shared/ui/base/input'
import { FormActions } from '@/shared/ui/layout/page'
import { ErrorText } from '@/shared/ui/feedback/state'
import { useI18n } from '@/shared/i18n/context/I18nContext'
import { errorMessage } from '@/shared/i18n/errors/errorMessage'
import { updateVehicle } from '@/features/vehicles/api/endpoints/endpoints'
import type {
  VehicleResponse,
  VehicleUpdateRequest,
} from '@/features/vehicles/api/types/types'

/**
 * 차량 정보(번호판·제조사·모델·연식) 수정. 주행거리 제외
 * 닫힌 동안에는 값 4개 표시
 */
export function VehicleInfoForm({
  vehicle,
  onUpdated,
}: {
  vehicle: VehicleResponse
  onUpdated: (vehicle: VehicleResponse) => void
}) {
  const { t } = useI18n()
  const [open, setOpen] = useState(false)

  return (
    <Card size="sm">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>{t.vehicles.info.title}</CardTitle>
        {!open && (
          <Button size="sm" variant="secondary" onClick={() => setOpen(true)}>
            {t.common.edit}
          </Button>
        )}
      </CardHeader>

      <CardContent>
        {open ? (
          // 펼침 연출(안쪽 0.12s 지연). 닫을 때는 없음
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
            <InfoRow label={t.vehicles.form.plate} value={vehicle.plateNumber} />
            <InfoRow label={t.vehicles.form.manufacturer} value={vehicle.manufacturer} />
            <InfoRow label={t.vehicles.form.model} value={vehicle.modelName} />
            <InfoRow
              label={t.vehicles.form.modelYear}
              value={vehicle.modelYear === null ? t.vehicles.info.unknown : t.vehicles.info.year(vehicle.modelYear)}
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
      {/* min-w-0 + truncate: 긴 모델명 대응 */}
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
  const { t } = useI18n()
  // 입력 중 빈 값 표현을 위해 문자열 보관
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

    // 바뀐 필드만
    const request: VehicleUpdateRequest = {}
    if (plateNumber !== vehicle.plateNumber) request.plateNumber = plateNumber
    if (manufacturer !== vehicle.manufacturer) request.manufacturer = manufacturer
    if (modelName !== vehicle.modelName) request.modelName = modelName
    if (modelYear !== '' && Number(modelYear) !== vehicle.modelYear) {
      request.modelYear = Number(modelYear)
    }

    // 변경 없으면 요청 생략
    if (Object.keys(request).length === 0) {
      onCancel()
      return
    }

    setPending(true)

    try {
      onUpdated(await updateVehicle(vehicle.id, request))
    } catch (caught) {
      // 409 = 이미 등록한 번호판
      setError(errorMessage(caught, t, t.vehicles.info.failed))
    } finally {
      setPending(false)
    }
  }

  return (
    <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
      <Field label={t.vehicles.form.plate} htmlFor="edit-plate-number">
        <Input
          id="edit-plate-number"
          // 번호판에 첫 포커스
          autoFocus
          required
          maxLength={20}
          value={plateNumber}
          onChange={(event) => setPlateNumber(event.target.value)}
        />
      </Field>

      <div className="grid gap-5 sm:grid-cols-2">
        <Field label={t.vehicles.form.manufacturer} htmlFor="edit-manufacturer">
          <Input
            id="edit-manufacturer"
            required
            maxLength={50}
            value={manufacturer}
            onChange={(event) => setManufacturer(event.target.value)}
          />
        </Field>

        <Field label={t.vehicles.form.model} htmlFor="edit-model-name">
          <Input
            id="edit-model-name"
            required
            maxLength={100}
            value={modelName}
            onChange={(event) => setModelName(event.target.value)}
          />
        </Field>
      </div>

      {/* required 없음. 연식 없는 예전 데이터도 다른 칸 수정 가능, 비우면 미전송 */}
      <Field label={t.vehicles.form.modelYear} htmlFor="edit-model-year" hint={t.vehicles.info.yearHint}>
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
          {pending ? t.common.saving : t.common.save}
        </Button>
        <Button type="button" variant="ghost" onClick={onCancel}>
          {t.common.cancel}
        </Button>
      </FormActions>
    </form>
  )
}
