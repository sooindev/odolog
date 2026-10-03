import { useCallback, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router'

import { MaintenanceSection } from '@/features/maintenance/components/MaintenanceSection'
import { NextServiceCard } from '@/features/maintenance/components/NextServiceCard'
import { VehicleInfoForm } from '@/features/vehicles/components/VehicleInfoForm'
import { GettingStartedCard } from '@/features/vehicles/components/GettingStartedCard'
import { FuelSection } from '@/features/fuel/components/FuelSection'
import { FuelSummaryCard } from '@/features/fuel/components/FuelSummaryCard'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { Field } from '@/shared/ui/form/field'
import { Input } from '@/shared/ui/base/input'
import { Page } from '@/shared/ui/layout/page'
import { ErrorText, Skeleton } from '@/shared/ui/state'
import { ApiError } from '@/shared/api/client'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import { useAsyncData } from '@/shared/lib/hooks/useAsyncData'
import { fromKm, toKm } from '@/shared/lib/units'
import { useCountUp } from '@/shared/lib/hooks/useCountUp'
import { MAX_ODOMETER } from '@/shared/lib/limits'
import { looksBigJump } from '@/shared/lib/odometer'
import { deleteVehicle, fetchVehicle, updateOdometer } from '@/features/vehicles/api/endpoints'
import type { VehicleResponse } from '@/features/vehicles/api/types'

export function VehicleDetailPage() {
  // URL 파라미터는 문자열
  const { vehicleId } = useParams<{ vehicleId: string }>()
  const navigate = useNavigate()
  const { t } = useI18n()

  // 공개 id 문자열 그대로
  const id = vehicleId ?? ''

  // 남의 차량·없는 차량 모두 404
  const load = useCallback(() => fetchVehicle(id), [id])
  const {
    data: vehicle,
    loading,
    error,
    reload: reloadVehicle,
    setData: setVehicle,
  } = useAsyncData(load, t.vehicles.detail.loadFailed)

  // 주행거리 409 문구. 폼은 주행거리 key 로 재생성되므로 재조회 뒤에도 남게 여기 보관
  const [odometerConflict, setOdometerConflict] = useState<string | null>(null)

  // 정비 이력 변경 시 증가. 다음 정비 카드 재생성
  const [maintenanceVersion, setMaintenanceVersion] = useState(0)
  // 주유 요약 카드 재생성용
  const [fuelVersion, setFuelVersion] = useState(0)
  // 주유 목록 재생성용. 연비 기준점 변경 때만 사용
  const [fuelListVersion, setFuelListVersion] = useState(0)
  // 정비 목록 재조회용. 목록 밖(시작하기 카드)에서 이력이 생겼을 때만 사용
  const [maintenanceListVersion, setMaintenanceListVersion] = useState(0)
  const [actionError, setActionError] = useState<string | null>(null)
  const [deleting, setDeleting] = useState(false)

  // 정비·주유로 차량 값이 바뀌는 재조회. 주행거리 409 문구는 그 순간 낡으므로 함께 지움
  function refreshVehicle() {
    setOdometerConflict(null)
    reloadVehicle()
  }

  if (loading) {
    return <VehicleDetailSkeleton />
  }

  // 처음부터 못 불러온 경우만 화면 전체를 오류로. 재조회 실패는 직전 값을 두고 위에 한 줄
  if (vehicle === null) {
    return (
      <Page
        back={{ to: '/vehicles', label: t.vehicles.myVehicles }}
        eyebrow="Garage"
        title={t.vehicles.detail.loadFailed}
      >
        <ErrorText message={error ?? t.vehicles.detail.notFound} />
      </Page>
    )
  }

  async function handleDelete() {
    // 함께 삭제되는 것을 모두 명시
    if (!window.confirm(t.vehicles.detail.deleteConfirm)) {
      return
    }

    setDeleting(true)

    try {
      await deleteVehicle(id)
      navigate('/vehicles', { replace: true })
    } catch (caught) {
      setActionError(errorMessage(caught, t, t.vehicles.detail.deleteFailed))
      // 성공 시 화면 이탈, 실패 시에만 복구
      setDeleting(false)
    }
  }

  return (
    // 머리말은 Page 담당
    <Page
      back={{ to: '/vehicles', label: t.vehicles.myVehicles }}
      // 차량 식별은 번호판
      eyebrow={vehicle.plateNumber}
      title={`${vehicle.manufacturer} ${vehicle.modelName}`}
      description={
        vehicle.modelYear === null ? t.vehicles.unknownYear : t.vehicles.modelYear(vehicle.modelYear)
      }
    >
      {/*
        왼쪽 현재 상태, 오른쪽 이력·다음 정비
        minmax(0,1fr): 긴 메모의 격자 넘침 방지
      */}
      {error !== null && <ErrorText message={error} />}

      <div className="grid gap-10 lg:grid-cols-[21rem_minmax(0,1fr)] lg:gap-16">
        {/* self-start: sticky 동작 조건 */}
        <div className="flex flex-col gap-8 lg:sticky lg:top-28 lg:self-start lg:gap-10">
          <OdometerHero odometer={vehicle.odometer} />

          {/*
            차량 주행거리를 key 로. 정비·주유로 값이 오르면 폼 재생성, 옛 값 저장 방지
            접두사: 형제 key 충돌 방지
          */}
          <OdometerForm
            key={`odometer-form-${vehicle.odometer}`}
            vehicle={vehicle}
            onUpdated={setVehicle}
            conflict={odometerConflict}
            // 409 뒤 재조회가 실패하면 "현재" 값을 말할 수 없음
            reloadFailed={error !== null}
            onConflict={(message) => {
              setOdometerConflict(message)
              // 다른 곳에서 값이 오름. 최신 값을 받아 입력 기준도 맞춤
              if (message !== null) reloadVehicle()
            }}
          />

          {/* 응답으로 바로 교체. 재조회 불필요 */}
          <VehicleInfoForm vehicle={vehicle} onUpdated={setVehicle} />

          {/* 되돌릴 수 없는 동작은 괘선 아래 맨 끝. 채우지 않은 빨간 버튼 */}
          <div className="flex flex-col gap-4 border-t border-border pt-8">
            {actionError !== null && <ErrorText message={actionError} />}

            <div className="flex flex-wrap items-center justify-between gap-3">
              <p className="text-caption text-muted-foreground">{t.vehicles.detail.deleteNote}</p>
              <Button
                variant="destructive"
                size="sm"
                className="shrink-0"
                disabled={deleting}
                onClick={handleDelete}
              >
                {deleting ? t.common.deleting : t.vehicles.detail.delete}
              </Button>
            </div>
          </div>
        </div>

        <div className="flex min-w-0 flex-col gap-10">
          {/*
            key 변경으로 재생성
            접두사 필수: 세 카운터가 모두 0 에서 시작해 숫자만이면 형제 key 충돌
          */}
          {/* 타던 차의 첫 단계 안내. 다 끝나면 스스로 사라짐. 재생성 대신 version 으로 재조회(깜빡임 방지) */}
          <GettingStartedCard
            vehicle={vehicle}
            version={maintenanceVersion + fuelVersion}
            onServicesSaved={() => {
              setMaintenanceVersion((current) => current + 1)
              setMaintenanceListVersion((current) => current + 1)
              // 그때 주행거리를 적었다면 차량 값이 올랐을 수 있음
              refreshVehicle()
            }}
          />

          {/* 지남 판정이 차량의 현재 주행거리 기준이라 주행거리도 key 에 */}
          <NextServiceCard
            key={`next-service-${maintenanceVersion}-${vehicle.odometer}`}
            vehicleId={vehicle.id}
          />

          {/* 재생성 대신 version 으로 재조회. 열어 둔 수정 폼·필터·페이지 유지 */}
          <MaintenanceSection
            vehicleId={vehicle.id}
            currentOdometer={vehicle.odometer}
            refreshVersion={maintenanceListVersion}
            onChanged={() => {
              setMaintenanceVersion((current) => current + 1)
              // 서버가 올렸을 수 있는 차량 주행거리 재조회
              refreshVehicle()
            }}
          />

          <FuelSummaryCard
            key={`fuel-summary-${fuelVersion}`}
            vehicleId={vehicle.id}
            onChanged={() => {
              setFuelVersion((current) => current + 1)
              setFuelListVersion((current) => current + 1)
            }}
          />

          {/* 시작하기 카드가 스크롤해 오는 자리 */}
          {/* tabIndex -1: 스크롤과 함께 포커스도 옮기려면 필요 */}
          <div id="fuel-section" tabIndex={-1} className="scroll-mt-28 outline-none">
          <FuelSection
            key={`fuel-list-${fuelListVersion}`}
            vehicleId={vehicle.id}
            currentOdometer={vehicle.odometer}
            onChanged={() => {
              setFuelVersion((current) => current + 1)
              // 서버가 올렸을 수 있는 차량 주행거리 재조회
              refreshVehicle()
            }}
          />
          </div>
        </div>
      </div>
    </Page>
  )
}

function VehicleDetailSkeleton() {
  return (
    <div className="flex flex-col gap-10">
      <div className="flex flex-col gap-5">
        <Skeleton className="h-4 w-20" />
        <div className="flex flex-col gap-2">
          <Skeleton className="h-3 w-24" />
          <Skeleton className="h-9 w-56" />
          <Skeleton className="h-4 w-20" />
        </div>
      </div>

      <div className="grid gap-10 lg:grid-cols-[21rem_minmax(0,1fr)] lg:gap-16">
        <div className="flex flex-col gap-8">
          <Skeleton className="h-13 w-48" />
          <Skeleton className="h-40" />
        </div>
        <div className="flex flex-col gap-6">
          <Skeleton className="h-64" />
          <Skeleton className="h-72" />
        </div>
      </div>
    </div>
  )
}

/**
 * 주행거리 히어로 숫자. 값이 바뀔 때만 굴러감
 * tabular-nums 는 굴러가는 동안만
 */
function OdometerHero({ odometer }: { odometer: number }) {
  const { t, f, unitSystem } = useI18n()
  // 화면 단위로 바꾼 뒤 굴림. km 로 굴리면 마일 화면에서 중간값이 튐
  const { value, running } = useCountUp(Math.round(fromKm(unitSystem, odometer)))

  return (
    <div className="flex flex-col gap-4 border-b border-border pb-8">
      <p className="text-eyebrow text-muted-foreground uppercase">Odometer</p>
      {/* 굴러가는 숫자는 aria-hidden, 확정 값만 aria-live 로 한 번 안내 */}
      <p
        aria-hidden="true"
        className={`flex items-baseline gap-3 text-display text-strong ${
          running ? 'tabular-nums' : ''
        }`}
      >
        {f.number(value)}
        <span className="text-eyebrow text-muted-foreground uppercase">{f.distanceUnit}</span>
      </p>
      <p className="sr-only" aria-live="polite" aria-atomic="true">
        {t.vehicles.detail.odometerAnnounce(f.distance(odometer))}
      </p>
    </div>
  )
}

function OdometerForm({
  vehicle,
  onUpdated,
  conflict,
  reloadFailed,
  onConflict,
}: {
  vehicle: VehicleResponse
  onUpdated: (vehicle: VehicleResponse) => void
  /** 직전 409 문구. 현재 값은 다시 불러온 vehicle 로 그림 */
  conflict: string | null
  reloadFailed: boolean
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
              message={reloadFailed ? conflict : t.vehicles.odometer.conflict(conflict, f.distance(vehicle.odometer))}
            />
          )}
        </form>
      </CardContent>
    </Card>
  )
}
