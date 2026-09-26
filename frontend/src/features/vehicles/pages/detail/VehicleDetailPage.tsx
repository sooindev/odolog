import { useCallback, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router'

import { MaintenanceSection } from '@/features/maintenance/components/section/MaintenanceSection'
import { NextServiceCard } from '@/features/maintenance/components/next-service/NextServiceCard'
import { VehicleInfoForm } from '@/features/vehicles/components/info-form/VehicleInfoForm'
import { FuelSection } from '@/features/fuel/components/section/FuelSection'
import { FuelSummaryCard } from '@/features/fuel/components/summary/FuelSummaryCard'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { Field } from '@/shared/ui/form/field'
import { Input } from '@/shared/ui/base/input'
import { Page } from '@/shared/ui/layout/page'
import { ErrorText, Skeleton } from '@/shared/ui/feedback/state'
import { ApiError } from '@/shared/api/client/client'
import { formatKm, formatNumber } from '@/shared/lib/format/format'
import { useAsyncData } from '@/shared/lib/hooks/useAsyncData'
import { useCountUp } from '@/shared/lib/hooks/useCountUp'
import { MAX_ODOMETER } from '@/shared/lib/limits/limits'
import { looksBigJump } from '@/shared/lib/odometer/odometer'
import { deleteVehicle, fetchVehicle, updateOdometer } from '@/features/vehicles/api/endpoints/endpoints'
import type { VehicleResponse } from '@/features/vehicles/api/types/types'

export function VehicleDetailPage() {
  // URL 파라미터는 문자열
  const { vehicleId } = useParams<{ vehicleId: string }>()
  const navigate = useNavigate()

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
  } = useAsyncData(load, '차량을 불러오지 못했습니다.')

  // 정비 이력 변경 시 증가. 다음 정비 카드 재생성
  const [maintenanceVersion, setMaintenanceVersion] = useState(0)
  // 주유 요약 카드 재생성용
  const [fuelVersion, setFuelVersion] = useState(0)
  // 주유 목록 재생성용. 연비 기준점 변경 때만 사용
  const [fuelListVersion, setFuelListVersion] = useState(0)
  const [actionError, setActionError] = useState<string | null>(null)
  const [deleting, setDeleting] = useState(false)

  if (loading) {
    return <VehicleDetailSkeleton />
  }

  if (error !== null || vehicle === null) {
    return <ErrorText message={error ?? '차량을 찾을 수 없습니다.'} />
  }

  async function handleDelete() {
    // 함께 삭제되는 것을 모두 명시
    if (!window.confirm('이 차량과 정비 이력, 주유 기록이 모두 삭제됩니다. 계속할까요?')) {
      return
    }

    setDeleting(true)

    try {
      await deleteVehicle(id)
      navigate('/vehicles', { replace: true })
    } catch (caught) {
      setActionError(caught instanceof ApiError ? caught.message : '삭제에 실패했습니다.')
      // 성공 시 화면 이탈, 실패 시에만 복구
      setDeleting(false)
    }
  }

  return (
    // 머리말은 Page 담당
    <Page
      back={{ to: '/vehicles', label: '내 차량' }}
      // 차량 식별은 번호판
      eyebrow={vehicle.plateNumber}
      title={`${vehicle.manufacturer} ${vehicle.modelName}`}
      description={vehicle.modelYear === null ? '연식 미상' : `${vehicle.modelYear}년식`}
    >
      {/*
        왼쪽 현재 상태, 오른쪽 이력·다음 정비
        minmax(0,1fr): 긴 메모의 격자 넘침 방지
      */}
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
          />

          {/* 응답으로 바로 교체. 재조회 불필요 */}
          <VehicleInfoForm vehicle={vehicle} onUpdated={setVehicle} />

          {/* 되돌릴 수 없는 동작은 괘선 아래 맨 끝. 채우지 않은 빨간 버튼 */}
          <div className="flex flex-col gap-4 border-t border-border pt-8">
            {actionError !== null && <ErrorText message={actionError} />}

            <div className="flex flex-wrap items-center justify-between gap-3">
              <p className="text-caption text-muted-foreground">
                삭제하면 정비 이력과 주유 기록도 함께 사라집니다.
              </p>
              <Button
                variant="destructive"
                size="sm"
                className="shrink-0"
                disabled={deleting}
                onClick={handleDelete}
              >
                {deleting ? '삭제 중…' : '차량 삭제'}
              </Button>
            </div>
          </div>
        </div>

        <div className="flex min-w-0 flex-col gap-10">
          {/*
            key 변경으로 재생성
            접두사 필수: 세 카운터가 모두 0 에서 시작해 숫자만이면 형제 key 충돌
          */}
          <NextServiceCard key={`next-service-${maintenanceVersion}`} vehicleId={vehicle.id} />

          <MaintenanceSection
            vehicleId={vehicle.id}
            currentOdometer={vehicle.odometer}
            onChanged={() => {
              setMaintenanceVersion((current) => current + 1)
              // 서버가 올렸을 수 있는 차량 주행거리 재조회
              reloadVehicle()
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

          <FuelSection
            key={`fuel-list-${fuelListVersion}`}
            vehicleId={vehicle.id}
            currentOdometer={vehicle.odometer}
            onChanged={() => {
              setFuelVersion((current) => current + 1)
              // 서버가 올렸을 수 있는 차량 주행거리 재조회
              reloadVehicle()
            }}
          />
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
  const { value, running } = useCountUp(odometer)

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
        {formatNumber(value)}
        <span className="text-eyebrow text-muted-foreground uppercase">km</span>
      </p>
      <p className="sr-only" aria-live="polite" aria-atomic="true">
        주행거리 {formatKm(odometer)}
      </p>
    </div>
  )
}

function OdometerForm({
  vehicle,
  onUpdated,
}: {
  vehicle: VehicleResponse
  onUpdated: (vehicle: VehicleResponse) => void
}) {
  const [odometer, setOdometer] = useState(String(vehicle.odometer))
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    const next = Number(odometer)

    // 감소는 확인 후 force 로. 자리수 오타·계기판 교체의 유일한 복구 경로
    // 급증도 확인. 올라간 값은 force 정정으로만 복구
    if (looksBigJump(next, vehicle.odometer)) {
      const confirmed = window.confirm(
        `${formatKm(vehicle.odometer)} 에서 ${formatKm(next)} 로 크게 뜁니다.\n` +
          '자리수를 확인해 주세요.\n\n이대로 저장할까요?',
      )
      if (!confirmed) {
        return
      }
    }

    let force = false
    if (next < vehicle.odometer) {
      const confirmed = window.confirm(
        `현재 기록된 ${formatKm(vehicle.odometer)} 보다 낮습니다.\n` +
          '계기판을 교체했거나 잘못 입력한 값을 고치는 경우에만 진행하세요.',
      )
      if (!confirmed) {
        return
      }
      force = true
    }

    setPending(true)

    try {
      onUpdated(await updateOdometer(vehicle.id, { odometer: next, force }))
    } catch (caught) {
      // 409 = 다른 곳에서 값이 오른 경우. 현재 값 함께 표시
      const message =
        caught instanceof ApiError && caught.status === 409
          ? `${caught.message} (현재 ${formatKm(vehicle.odometer)})`
          : '주행거리 갱신에 실패했습니다.'
      setError(message)
    } finally {
      setPending(false)
    }
  }

  return (
    <Card size="sm">
      <CardHeader>
        <CardTitle>주행거리 갱신</CardTitle>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-3" onSubmit={handleSubmit}>
          {/* 값 하나짜리 폼이라 버튼을 입력칸 옆에 */}
          <Field label="현재 주행거리 (km)" htmlFor="odometer">
            <div className="flex gap-2">
              <Input
                id="odometer"
                type="number"
                required
                min={0}
                max={MAX_ODOMETER}
                className="flex-1 tabular-nums"
                value={odometer}
                onChange={(event) => setOdometer(event.target.value)}
              />
              <Button type="submit" variant="secondary" disabled={pending}>
                {pending ? '저장 중…' : '갱신'}
              </Button>
            </div>
          </Field>

          {error !== null && <ErrorText message={error} />}
        </form>
      </CardContent>
    </Card>
  )
}
