import { useState } from 'react'
import type { FormEvent } from 'react'

import { Button } from '@/shared/ui/base/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { Field } from '@/shared/ui/form/field'
import { Input } from '@/shared/ui/base/input'
import { ErrorText } from '@/shared/ui/state'
import { ApiError } from '@/shared/api/client'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import { fromKm, toKm } from '@/shared/lib/units'
import { MAX_ODOMETER } from '@/shared/lib/limits'
import { looksBigJump } from '@/shared/lib/odometer'
import { updateOdometer } from '@/features/vehicles/api/endpoints'
import type { VehicleResponse } from '@/features/vehicles/api/types'

/** 직전 409 문구와 그때의 값 */
export interface OdometerConflict {
  message: string
  staleOdometer: number
}

/**
 * 주행거리 갱신 폼
 * 부모가 차량 주행거리를 key 로 줌. 값이 오르면 폼 재생성, 옛 값 저장 방지
 */
export function OdometerForm({
  vehicle,
  onUpdated,
  conflict,
  onConflict,
}: {
  vehicle: VehicleResponse
  onUpdated: (vehicle: VehicleResponse) => void
  /** 직전 409 문구와 그때의 값. 다시 불러와 값이 바뀐 뒤에만 "현재" 를 말함 */
  conflict: OdometerConflict | null
  onConflict: (message: string | null) => void
}) {
  const { t, f, unitSystem } = useI18n()
  // 입력칸은 화면 단위. 손대지 않았으면 저장값(km) 그대로 써서 왕복 반올림 오차 차단
  const initial = String(Math.round(fromKm(unitSystem, vehicle.odometer)))
  const [odometer, setOdometer] = useState(initial)
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    onConflict(null)

    const next = odometer === initial ? vehicle.odometer : toKm(unitSystem, Number(odometer))

    // 감소는 확인 후 force 로. 자리수 오타·계기판 교체의 유일한 복구 경로
    // 급증도 확인. 올라간 값은 force 정정으로만 복구
    if (looksBigJump(next, vehicle.odometer)) {
      const confirmed = window.confirm(
        t.vehicles.odometer.bigJump(f.distance(vehicle.odometer), f.distance(next)),
      )
      if (!confirmed) {
        return
      }
    }

    let force = false
    if (next < vehicle.odometer) {
      const confirmed = window.confirm(t.vehicles.odometer.decrease(f.distance(vehicle.odometer)))
      if (!confirmed) {
        return
      }
      force = true
    }

    setPending(true)

    try {
      onUpdated(await updateOdometer(vehicle.id, { odometer: next, force }))
    } catch (caught) {
      // 409 = 다른 곳에서 값이 오른 경우. 부모가 다시 불러오고 현재 값과 함께 표시
      const message = errorMessage(caught, t, t.vehicles.odometer.failed)
      if (caught instanceof ApiError && caught.status === 409) {
        onConflict(message)
      } else {
        setError(message)
      }
    } finally {
      setPending(false)
    }
  }

  return (
    <Card size="sm">
      <CardHeader>
        <CardTitle>{t.vehicles.odometer.title}</CardTitle>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-3" onSubmit={handleSubmit}>
          {/* 값 하나짜리 폼이라 버튼을 입력칸 옆에 */}
          <Field label={t.vehicles.odometer.label(f.distanceUnit)} htmlFor="odometer">
            <div className="flex gap-2">
              <Input
                id="odometer"
                type="number"
                required
                min={0}
                max={Math.floor(fromKm(unitSystem, MAX_ODOMETER))}
                className="flex-1 tabular-nums"
                value={odometer}
                onChange={(event) => setOdometer(event.target.value)}
              />
              <Button type="submit" variant="secondary" disabled={pending}>
                {pending ? t.common.saving : t.vehicles.odometer.submit}
              </Button>
            </div>
          </Field>

          {error !== null && <ErrorText message={error} />}
          {conflict !== null && (
            <ErrorText
              message={
                // 재조회 전·실패 시에는 옛 값이라 "현재" 를 붙이지 않음
                vehicle.odometer === conflict.staleOdometer
                  ? conflict.message
                  : t.vehicles.odometer.conflict(conflict.message, f.distance(vehicle.odometer))
              }
            />
          )}
        </form>
      </CardContent>
    </Card>
  )
}
