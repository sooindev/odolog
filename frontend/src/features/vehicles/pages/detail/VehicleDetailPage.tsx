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
import { deleteVehicle, fetchVehicle, updateOdometer } from '@/features/vehicles/api/endpoints/endpoints'
import type { VehicleResponse } from '@/features/vehicles/api/types/types'

export function VehicleDetailPage() {
  // URL의 :vehicleId 는 항상 문자열로 들어온다.
  const { vehicleId } = useParams<{ vehicleId: string }>()
  const navigate = useNavigate()

  const id = Number(vehicleId)

  // 404(없음)와 403(남의 차)을 구분해 보여주지 않는다. 남의 차량이 존재한다는
  // 사실 자체를 알리지 않기 위해서.
  const load = useCallback(() => fetchVehicle(id), [id])
  const {
    data: vehicle,
    loading,
    error,
    reload: reloadVehicle,
    setData: setVehicle,
  } = useAsyncData(load, '차량을 불러오지 못했습니다.')

  // 정비 이력이 바뀌면 이 값을 올려 "다음 정비 시점"을 다시 계산하게 한다.
  const [maintenanceVersion, setMaintenanceVersion] = useState(0)
  // 주유 기록도 같은 방식으로 요약 카드를 재생성시킨다.
  const [fuelVersion, setFuelVersion] = useState(0)
  const [actionError, setActionError] = useState<string | null>(null)
  const [deleting, setDeleting] = useState(false)

  if (loading) {
    return <VehicleDetailSkeleton />
  }

  if (error !== null || vehicle === null) {
    return <ErrorText message={error ?? '차량을 찾을 수 없습니다.'} />
  }

  async function handleDelete() {
    // 되돌릴 수 없는 동작이라 정비 이력까지 사라진다는 걸 명시한다.
    if (!window.confirm('이 차량과 정비 이력이 모두 삭제됩니다. 계속할까요?')) {
      return
    }

    setDeleting(true)

    try {
      await deleteVehicle(id)
      navigate('/vehicles', { replace: true })
    } catch (caught) {
      setActionError(caught instanceof ApiError ? caught.message : '삭제에 실패했습니다.')
      // 성공하면 이 화면을 떠나므로 실패했을 때만 되돌린다.
      setDeleting(false)
    }
  }

  return (
    // 머리말은 Page 에 맡긴다. 예전에 여기만 손으로 그리다 제목 크기를 빠뜨린 적이 있다.
    <Page
      back={{ to: '/vehicles', label: '내 차량' }}
      // 차량을 식별하는 건 모델명이 아니라 번호판이라 eyebrow 자리에 올린다.
      eyebrow={vehicle.plateNumber}
      title={`${vehicle.manufacturer} ${vehicle.modelName}`}
      description={vehicle.modelYear === null ? '연식 미상' : `${vehicle.modelYear}년식`}
    >
      {/*
        왼쪽은 지금 상태(주행거리), 오른쪽은 이력과 다음 정비.
        minmax(0,1fr) 이 없으면 긴 메모 한 줄이 열을 밀어내 격자가 넘친다
        (grid 자식의 기본 min-width 가 auto 라서).
      */}
      <div className="grid gap-10 lg:grid-cols-[21rem_minmax(0,1fr)] lg:gap-16">
        {/* self-start 가 없으면 칸이 옆 열 높이만큼 늘어나 sticky 가 걸리지 않는다. */}
        <div className="flex flex-col gap-8 lg:sticky lg:top-28 lg:self-start lg:gap-10">
          <OdometerHero odometer={vehicle.odometer} />

          <OdometerForm vehicle={vehicle} onUpdated={setVehicle} />

          {/* setVehicle 을 그대로 넘긴다 — 응답이 곧 최신 상태라 다시 조회할 필요가 없고,
              머리말(번호판·제조사·모델·연식)도 같은 객체를 보므로 함께 갱신된다. */}
          <VehicleInfoForm vehicle={vehicle} onUpdated={setVehicle} />

          {/* 되돌릴 수 없는 동작은 선으로 끊어 맨 아래에. 버튼을 빨갛게 채우면
              가장 하면 안 되는 일이 화면에서 가장 강한 요소가 된다. */}
          <div className="flex flex-col gap-4 border-t border-border pt-8">
            {actionError !== null && <ErrorText message={actionError} />}

            <div className="flex flex-wrap items-center justify-between gap-3">
              <p className="text-[0.8125rem] text-muted-foreground">
                삭제하면 정비 이력도 함께 사라집니다.
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
          {/* key 가 바뀌면 React 가 새로 만들어서 다음 정비 시점이 다시 계산된다. */}
          <NextServiceCard key={maintenanceVersion} vehicleId={vehicle.id} />

          <MaintenanceSection
            vehicleId={vehicle.id}
            currentOdometer={vehicle.odometer}
            onChanged={() => setMaintenanceVersion((current) => current + 1)}
          />

          <FuelSummaryCard key={fuelVersion} vehicleId={vehicle.id} />

          <FuelSection
            vehicleId={vehicle.id}
            currentOdometer={vehicle.odometer}
            onChanged={() => {
              setFuelVersion((current) => current + 1)
              // 주유 기록의 주행거리가 더 크면 서버가 차량 쪽도 올린다. 다시 받아 와야
              // 위의 히어로 숫자가 맞고, 값이 바뀐 만큼 굴러가는 연출도 거기서 나온다.
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
 * 이 화면의 주인공 숫자. 이 앱에서 가장 자주 확인하는 값이고 앱 이름도 여기서 왔다.
 * 화면을 열 때는 움직이지 않고, 아래 폼으로 값이 바뀐 순간에만 굴러간다.
 * tabular-nums 는 굴러가는 동안에만 붙인다(useCountUp 참고).
 */
function OdometerHero({ odometer }: { odometer: number }) {
  const { value, running } = useCountUp(odometer)

  return (
    <div className="flex flex-col gap-4 border-b border-border pb-8">
      <p className="text-eyebrow text-muted-foreground uppercase">Odometer</p>
      <p
        className={`flex items-baseline gap-3 text-display text-strong ${
          running ? 'tabular-nums' : ''
        }`}
      >
        {formatNumber(value)}
        <span className="text-eyebrow text-muted-foreground uppercase">km</span>
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
    setPending(true)

    try {
      onUpdated(await updateOdometer(vehicle.id, { odometer: Number(odometer) }))
    } catch (caught) {
      // 백엔드는 주행거리가 줄면 409 를 준다. 현재 값을 같이 보여줘야 뭘 잘못했는지 안다.
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
          {/* 값 하나만 고치는 폼이라 버튼을 아래로 내리지 않는다. 폼이 실제보다 커 보인다. */}
          <Field label="현재 주행거리 (km)" htmlFor="odometer">
            <div className="flex gap-2">
              <Input
                id="odometer"
                type="number"
                required
                min={0}
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
