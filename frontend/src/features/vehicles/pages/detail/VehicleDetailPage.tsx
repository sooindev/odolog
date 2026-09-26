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
  // URL 파라미터는 언제나 문자열
  const { vehicleId } = useParams<{ vehicleId: string }>()
  const navigate = useNavigate()

  // 공개 id 문자열 그대로. 라우트가 :vehicleId 를 요구하므로 비는 일은 없지만 타입이 string | undefined 다
  const id = vehicleId ?? ''

  // 남의 차량도 없는 차량도 서버가 404 하나로 답함 — 존재 자체를 숨기는 쪽이 백엔드
  const load = useCallback(() => fetchVehicle(id), [id])
  const {
    data: vehicle,
    loading,
    error,
    reload: reloadVehicle,
    setData: setVehicle,
  } = useAsyncData(load, '차량을 불러오지 못했습니다.')

  // 정비 이력이 바뀌면 올려서 다음 정비 시점 재계산
  const [maintenanceVersion, setMaintenanceVersion] = useState(0)
  // 주유 요약 카드도 같은 방식으로 재생성
  const [fuelVersion, setFuelVersion] = useState(0)
  // 목록은 스스로 갱신하므로 평소엔 미사용
  // 예외는 연비 기준점 변경 — 각 행의 구간 연비까지 달라지는데 그 동작은 카드에서 일어남
  // 페이지가 1쪽으로 돌아가지만 드물게 누르는 동작이라 감수
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
    // 되돌릴 수 없는 동작이라 함께 사라지는 것을 빠짐없이 명시
    // 코드는 지우는데 말을 안 하면 남는 줄 알고 누르게 됨
    if (!window.confirm('이 차량과 정비 이력, 주유 기록이 모두 삭제됩니다. 계속할까요?')) {
      return
    }

    setDeleting(true)

    try {
      await deleteVehicle(id)
      navigate('/vehicles', { replace: true })
    } catch (caught) {
      setActionError(caught instanceof ApiError ? caught.message : '삭제에 실패했습니다.')
      // 성공하면 화면을 떠나므로 실패했을 때만 복구
      setDeleting(false)
    }
  }

  return (
    // 머리말은 Page 담당. 직접 그리다 제목 크기를 빠뜨린 전례가 있음
    <Page
      back={{ to: '/vehicles', label: '내 차량' }}
      // 차량을 식별하는 건 모델명이 아니라 번호판
      eyebrow={vehicle.plateNumber}
      title={`${vehicle.manufacturer} ${vehicle.modelName}`}
      description={vehicle.modelYear === null ? '연식 미상' : `${vehicle.modelYear}년식`}
    >
      {/*
        왼쪽은 지금 상태(주행거리), 오른쪽은 이력과 다음 정비
        minmax(0,1fr) 이 없으면 긴 메모 한 줄이 열을 밀어내 격자가 넘침 (grid 자식의 기본 min-width 가 auto)
      */}
      <div className="grid gap-10 lg:grid-cols-[21rem_minmax(0,1fr)] lg:gap-16">
        {/* self-start 가 없으면 칸이 옆 열 높이만큼 늘어나 sticky 가 안 걸림 */}
        <div className="flex flex-col gap-8 lg:sticky lg:top-28 lg:self-start lg:gap-10">
          <OdometerHero odometer={vehicle.odometer} />

          {/*
            ⚠️ key 로 차량 주행거리를 건다. 정비·주유를 기록하면 서버가 차량 쪽도 올리는데,
            이 폼의 입력칸은 useState 초기값이라 **옛 값에 머문다.** 그대로 저장하면
            감소 확인 창을 거쳐 주행거리가 되돌아갈 수 있다.
            props 로 state 를 파생시키는 대신 값이 바뀌면 폼을 새로 만든다.
            접두사는 형제 사이 key 충돌을 피하기 위한 것 — 숫자만 쓰면 다른 카운터와 만난다.
          */}
          <OdometerForm
            key={`odometer-form-${vehicle.odometer}`}
            vehicle={vehicle}
            onUpdated={setVehicle}
          />

          {/* setVehicle 을 그대로 전달 — 응답이 곧 최신 상태라 재조회 불필요
              머리말도 같은 객체를 보므로 함께 갱신됨 */}
          <VehicleInfoForm vehicle={vehicle} onUpdated={setVehicle} />

          {/* 되돌릴 수 없는 동작은 선으로 끊어 맨 아래
              빨갛게 채우면 가장 하면 안 되는 일이 화면에서 가장 강한 요소가 됨 */}
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
            key 가 바뀌면 React 가 새로 만들어 다음 정비 시점 재계산

            ⚠️ key 에 접두사가 붙어 있는 이유. 세 버전 값이 모두 0 에서 시작하는데,
            숫자만 쓰면 이 열의 형제 셋이 같은 key 를 갖는다. React 는 같은 부모 안에서
            key 로 자식을 짝짓기 때문에 그 순간 엉뚱한 컴포넌트를 재사용하거나 남겨 둔다 —
            주유 기록을 수정했을 때 연비 카드가 둘로 보이던 원인이 이것이다.
          */}
          <NextServiceCard key={`next-service-${maintenanceVersion}`} vehicleId={vehicle.id} />

          <MaintenanceSection
            vehicleId={vehicle.id}
            currentOdometer={vehicle.odometer}
            onChanged={() => {
              setMaintenanceVersion((current) => current + 1)
              // 서버가 차량 주행거리를 올렸을 수 있음
              // 다시 받아야 히어로 숫자가 맞고 굴러가는 연출도 거기서 나옴
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
              // 정비와 같은 이유 — 서버가 차량 주행거리를 올렸을 수 있음
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
 * 이 화면의 주인공 숫자. 앱 이름도 여기서 나옴
 * 화면을 열 때는 정지, 값이 실제로 바뀐 순간에만 굴러감
 * tabular-nums 는 굴러가는 동안에만 (useCountUp)
 */
function OdometerHero({ odometer }: { odometer: number }) {
  const { value, running } = useCountUp(odometer)

  return (
    <div className="flex flex-col gap-4 border-b border-border pb-8">
      <p className="text-eyebrow text-muted-foreground uppercase">Odometer</p>
      {/*
        굴러가는 숫자는 aria-hidden 으로 가린다. 매 프레임 값이 바뀌므로 그대로 읽히게 두면
        스크린리더가 지나가는 숫자를 수십 번 읽는다.
        대신 아래에 확정된 값만 aria-live 로 한 번 알린다 — 주행거리가 올랐다는 사실은
        화면에서는 움직임이 나르지만, 소리로는 아무 일도 없던 자리였다.
      */}
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

    /*
     * 낮추는 것은 기본적으로 막혀 있다. 다만 막기만 하면 자리수를 잘못 넣었을 때
     * 되돌릴 방법이 아예 없어진다 — 기록을 고쳐도 차량 값은 따라 내려오지 않기 때문이다.
     * 계기판 교체도 실제로 일어나는 일이라, 묻고 나서 force 를 실어 보낸다.
     */
    /*
     * 급증도 묻는다. 줄이는 쪽만 막던 시절에는 방향이 거꾸로였다 —
     * 줄이는 것은 force 로 되돌릴 수 있지만 올라간 값은 force 정정 말고는 길이 없다
     */
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
      // 409 는 다른 탭에서 값이 오른 경우. 현재 값을 같이 보여줘야 무엇이 잘못됐는지 앎
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
          {/* 값 하나짜리 폼이라 버튼을 아래로 내리지 않음. 폼이 실제보다 커 보임 */}
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
