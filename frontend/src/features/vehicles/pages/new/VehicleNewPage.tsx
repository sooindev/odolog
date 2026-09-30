import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router'

import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { Field } from '@/shared/ui/form/field'
import { Input } from '@/shared/ui/base/input'
import { FormActions, Page } from '@/shared/ui/layout/page'
import { Section } from '@/shared/ui/layout/section'
import { ErrorText } from '@/shared/ui/feedback/state'
import { useI18n } from '@/shared/i18n/context/I18nContext'
import { errorMessage } from '@/shared/i18n/errors/errorMessage'
import { MAX_ODOMETER } from '@/shared/lib/limits/limits'
import { fromKm, toKm } from '@/shared/lib/units/units'
import { registerVehicle } from '@/features/vehicles/api/endpoints/endpoints'

export function VehicleNewPage() {
  const navigate = useNavigate()
  const { t, f, unitSystem } = useI18n()

  const [plateNumber, setPlateNumber] = useState('')
  const [manufacturer, setManufacturer] = useState('')
  const [modelName, setModelName] = useState('')
  // 입력 중 빈 값 표현을 위해 문자열 보관
  const [modelYear, setModelYear] = useState('')
  // 화면 단위(km·mi) 문자열. 저장 직전 km 로
  const [odometer, setOdometer] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setPending(true)

    try {
      const vehicle = await registerVehicle({
        plateNumber,
        manufacturer,
        modelName,
        // 전송 직전 숫자 변환
        modelYear: Number(modelYear),
        odometer: toKm(unitSystem, Number(odometer)),
      })
      navigate(`/vehicles/${vehicle.id}`, { replace: true })
    } catch (caught) {
      // 409 = 이미 등록된 번호판
      setError(errorMessage(caught, t, t.vehicles.form.failed))
    } finally {
      setPending(false)
    }
  }

  return (
    // back: 목록으로 이동. 입력을 버리는 취소와 별개
    <Page
      back={{ to: '/vehicles', label: t.vehicles.myVehicles }}
      eyebrow="Garage"
      title={t.vehicles.form.title}
    >
      {/* 왼쪽 설명 / 오른쪽 폼 2단 */}
      <Section title={t.vehicles.form.sectionTitle} description={t.vehicles.form.sectionDescription}>
        <Card>
          <CardContent>
            <form className="flex max-w-lg flex-col gap-5" onSubmit={handleSubmit}>
              <Field label={t.vehicles.form.plateNumber} htmlFor="plateNumber">
                <Input
                  id="plateNumber"
                  required
                  maxLength={20}
                  placeholder={t.vehicles.form.platePlaceholder}
                  value={plateNumber}
                  onChange={(event) => setPlateNumber(event.target.value)}
                />
              </Field>

              {/* 제조사·모델명 한 줄. 좁은 화면만 위아래 */}
              <div className="grid gap-5 sm:grid-cols-2">
                <Field label={t.vehicles.form.manufacturer} htmlFor="manufacturer">
                  <Input
                    id="manufacturer"
                    required
                    maxLength={50}
                    placeholder={t.vehicles.form.manufacturerPlaceholder}
                    value={manufacturer}
                    onChange={(event) => setManufacturer(event.target.value)}
                  />
                </Field>

                <Field label={t.vehicles.form.modelName} htmlFor="modelName">
                  <Input
                    id="modelName"
                    required
                    maxLength={100}
                    placeholder={t.vehicles.form.modelPlaceholder}
                    value={modelName}
                    onChange={(event) => setModelName(event.target.value)}
                  />
                </Field>
              </div>

              {/* 연식·주행거리 한 줄. 좁은 화면만 위아래 */}
              <div className="grid gap-5 sm:grid-cols-2">
                <Field label={t.vehicles.form.modelYear} htmlFor="modelYear">
                  <Input
                    id="modelYear"
                    type="number"
                    required
                    min={1900}
                    max={2100}
                    placeholder="2023"
                    // tabular-nums: 입력 중 흔들림 방지
                    className="tabular-nums"
                    value={modelYear}
                    onChange={(event) => setModelYear(event.target.value)}
                  />
                </Field>

                {/* 타던 차는 지금 값에서 시작. 0 이면 첫 기록부터 판정이 어긋남 */}
                <Field
                  label={t.vehicles.form.odometer(f.distanceUnit)}
                  htmlFor="odometer"
                  hint={t.vehicles.form.odometerHint}
                >
                  <Input
                    id="odometer"
                    type="number"
                    required
                    min={0}
                    max={Math.floor(fromKm(unitSystem, MAX_ODOMETER))}
                    placeholder="45000"
                    className="tabular-nums"
                    value={odometer}
                    onChange={(event) => setOdometer(event.target.value)}
                  />
                </Field>
              </div>

              {error !== null && <ErrorText message={error} />}

              <FormActions>
                <Button type="submit" disabled={pending}>
                  {pending ? t.common.registering : t.common.register}
                </Button>
                <Button type="button" variant="ghost" onClick={() => navigate(-1)}>
                  {t.common.cancel}
                </Button>
              </FormActions>
            </form>
          </CardContent>
        </Card>
      </Section>
    </Page>
  )
}
