import { useCallback, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router'

import { MaintenanceSection } from '@/features/maintenance/components/section/MaintenanceSection'
import { NextServiceCard } from '@/features/maintenance/components/next-service/NextServiceCard'
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

  // 404(없음)와 403(남의 차)을 구분해 보여주지 않는다.
  // 남의 차량이 "존재한다"는 사실 자체를 알리지 않기 위해서다.
  const load = useCallback(() => fetchVehicle(id), [id])
  const {
    data: vehicle,
    loading,
    error,
    setData: setVehicle,
  } = useAsyncData(load, '차량을 불러오지 못했습니다.')

  // 정비 이력이 바뀌면 이 값을 올려 "다음 정비 시점"을 다시 계산하게 한다.
  const [maintenanceVersion, setMaintenanceVersion] = useState(0)
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
    // 머리말(번호판·모델명·연식)을 사이드바 안에서 꺼내 다른 화면들과 같은 자리에 뒀다.
    // 예전에는 여기만 머리말을 손으로 그려서 sm:text-[2rem] 이 빠져 있었다.
    <Page
      back={{ to: '/vehicles', label: '내 차량' }}
      // 차량을 식별하는 건 모델명이 아니라 번호판이다. 그래서 eyebrow 자리에 올린다.
      eyebrow={vehicle.plateNumber}
      title={`${vehicle.manufacturer} ${vehicle.modelName}`}
      description={vehicle.modelYear === null ? '연식 미상' : `${vehicle.modelYear}년식`}
    >
      {/*
        넓은 화면에서 2단으로 나눈다. 왼쪽은 "이 차가 지금 어떤 상태인가"(주행거리),
        오른쪽은 "무엇을 했고 무엇을 할 것인가"(이력과 다음 정비).
        한 줄로 쌓으면 정비 이력을 보려고 스크롤할 때마다 주행거리가 화면 밖으로 사라진다.

        minmax(0,1fr): 오른쪽 열이 내용보다 작아질 수 있게 한다. 이게 없으면 긴 메모 한 줄이
        열을 밀어내 격자 전체가 넘친다(grid 자식의 기본 min-width는 auto라서).
      */}
      <div className="grid gap-12 lg:grid-cols-[21rem_minmax(0,1fr)] lg:gap-16">
        {/*
          lg:sticky + self-start: 오른쪽 이력을 길게 스크롤해도 주행거리가 따라온다.
          self-start 가 없으면 격자 칸이 오른쪽 높이만큼 늘어나 sticky 가 걸리지 않는다.
          top-24 = 헤더 높이(64px) + 32px 숨통.
        */}
        <div className="flex flex-col gap-10 lg:sticky lg:top-28 lg:self-start">
          {/*
            주행거리를 표(dl) 한 줄이 아니라 이 열의 주인공으로 올렸다.
            이 앱에서 사용자가 가장 자주 확인하는 숫자 하나이고, 앱 이름도 여기서 왔다.
            숫자만 크게 두고 단위(km)는 작게 붙여 "값"과 "단위"의 위계를 나눈다.
          */}
          <OdometerHero odometer={vehicle.odometer} />

          <OdometerForm vehicle={vehicle} onUpdated={setVehicle} />

          {/*
            파괴적인 동작은 선 하나로 끊어 맨 아래에 둔다. 버튼을 빨갛게 채우지 않고
            글자만 빨갛게 두는 이유: 채운 빨강은 화면에서 가장 강한 요소가 되어,
            가장 하면 안 되는 일이 가장 먼저 눈에 들어온다.
          */}
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
          {/* key 가 바뀌면 React 가 이 컴포넌트를 버리고 새로 만든다 → 자동으로 다시 계산된다. */}
          <NextServiceCard key={maintenanceVersion} vehicleId={vehicle.id} />

          <MaintenanceSection
            vehicleId={vehicle.id}
            currentOdometer={vehicle.odometer}
            onChanged={() => setMaintenanceVersion((current) => current + 1)}
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

      <div className="grid gap-12 lg:grid-cols-[21rem_minmax(0,1fr)] lg:gap-16">
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
 * 이 화면의 주인공 숫자.
 *
 * **화면을 열 때는 움직이지 않는다.** 0에서 굴러 오르는 연출은 처음 한 번만 근사하고,
 * 두 번째부터는 값을 읽기까지 기다리는 시간이 된다. 아래 주행거리 갱신으로 **값이 실제로
 * 바뀐 순간에만** 직전 값에서 새 값으로 굴러간다 — 그 움직임이 곧 "얼마나 올랐는지"를
 * 말해 준다. 연출이 정보를 나르는 경우다.
 *
 * 굵기는 300이다. 크기가 이미 강조를 다 하고 있어서 굵기까지 올리면 숫자가 둔해지고,
 * 얇게 두면 같은 크기라도 훨씬 정밀해 보인다.
 *
 * tabular-nums 는 **굴러가는 동안에만** 붙인다. 큰 숫자에 항상 붙이면 맞출 상대가 없는데
 * 폭만 벌어져 헐거워 보이지만(디자인 시스템 7번), 매 프레임 숫자가 바뀌는 동안 폭이
 * 들쭉날쭉하면 숫자 전체가 덜덜 떨린다. 멈추면 원래 비례 숫자로 돌아간다.
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
      // 백엔드는 주행거리가 줄면 409를 준다. 현재 값을 같이 보여줘야 뭘 잘못했는지 안다.
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
          {/* 입력과 버튼을 한 줄에 둔다. 값 하나만 고치는 폼에서 버튼을 아래로 내리면
              폼이 실제 하는 일보다 커 보인다. */}
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
