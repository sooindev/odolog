import { useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useQuery, useQueryClient } from '@tanstack/react-query'

import { MaintenanceSection } from '@/features/maintenance/components/MaintenanceSection'
import { ALL_MAINTENANCE } from '@/features/maintenance/components/maintenanceView'
import type { MaintenanceView } from '@/features/maintenance/components/maintenanceView'
import { NextServiceCard } from '@/features/maintenance/components/NextServiceCard'
import { VehicleInfoForm } from '@/features/vehicles/components/VehicleInfoForm'
import { GettingStartedCard } from '@/features/vehicles/components/GettingStartedCard'
import { OdometerHero } from '@/features/vehicles/components/OdometerHero'
import { OdometerForm } from '@/features/vehicles/components/OdometerForm'
import type { OdometerConflict } from '@/features/vehicles/components/OdometerForm'
import { FuelSection } from '@/features/fuel/components/FuelSection'
import { FuelSummaryCard } from '@/features/fuel/components/FuelSummaryCard'
import { Button } from '@/shared/ui/base/button'
import { Page } from '@/shared/ui/layout/page'
import { ErrorText, Skeleton } from '@/shared/ui/state'
import { ApiError } from '@/shared/api/client'
import { invalidateAfterMaintenance, queryKeys } from '@/shared/api/queryKeys'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import { deleteVehicle, fetchVehicle } from '@/features/vehicles/api/endpoints'
import type { VehicleResponse } from '@/features/vehicles/api/types'

export function VehicleDetailPage() {
  // URL 파라미터는 문자열
  const { vehicleId } = useParams<{ vehicleId: string }>()
  const navigate = useNavigate()
  const { t } = useI18n()
  const queryClient = useQueryClient()

  // 공개 id 문자열 그대로
  const id = vehicleId ?? ''

  // 남의 차량·없는 차량 모두 404
  const { data: vehicle, isPending, error } = useQuery({
    queryKey: queryKeys.vehicleDetail(id),
    queryFn: () => fetchVehicle(id),
  })

  // 주행거리 409 문구와 그때의 값. 폼은 주행거리 key 로 재생성되므로 재조회 뒤에도 남게 여기 보관
  const [odometerConflict, setOdometerConflict] = useState<OdometerConflict | null>(null)
  // 정비 이력 목록의 페이지·필터. 빠른 정비가 저장되면 첫 장·전체로
  const [maintenanceView, setMaintenanceView] = useState<MaintenanceView>(ALL_MAINTENANCE)
  const [actionError, setActionError] = useState<string | null>(null)
  const [deleting, setDeleting] = useState(false)

  // 응답으로 받은 차량으로 캐시 교체. 재조회 불필요
  function replaceVehicle(updated: VehicleResponse) {
    queryClient.setQueryData(queryKeys.vehicleDetail(id), updated)
  }

  if (isPending) {
    return <VehicleDetailSkeleton />
  }

  // 처음부터 못 불러온 경우·지워진 차량은 화면 전체를 오류로. 그 밖의 재조회 실패는 직전 값을 두고 위에 한 줄
  const notFound = error instanceof ApiError && error.status === 404
  if (vehicle === undefined || notFound) {
    return (
      <Page
        back={{ to: '/vehicles', label: t.vehicles.myVehicles }}
        eyebrow="Garage"
        title={t.vehicles.detail.loadFailed}
      >
        <ErrorText message={errorMessage(error, t, t.vehicles.detail.notFound)} />
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

  // 정비·주유가 차량 값을 바꿨을 수 있음. 주행거리 409 문구는 그 순간 낡으므로 지움
  function clearOdometerConflict() {
    setOdometerConflict(null)
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
      {error !== null && <ErrorText message={errorMessage(error, t, t.vehicles.detail.loadFailed)} />}

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
            onUpdated={(updated) => {
              replaceVehicle(updated)
              // 지남 판정이 차량의 현재 주행거리 기준
              void queryClient.invalidateQueries({ queryKey: queryKeys.nextServices(id) })
            }}
            conflict={odometerConflict}
            onConflict={(message) => {
              setOdometerConflict(message === null ? null : { message, staleOdometer: vehicle.odometer })
              // 다른 곳에서 값이 오름. 최신 값을 받아 입력 기준도 맞춤
              if (message !== null) {
                void queryClient.invalidateQueries({ queryKey: queryKeys.vehicleDetail(id) })
                void queryClient.invalidateQueries({ queryKey: queryKeys.nextServices(id) })
              }
            }}
          />

          {/* 응답으로 바로 교체. 재조회 불필요 */}
          <VehicleInfoForm vehicle={vehicle} onUpdated={replaceVehicle} />

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
            재조회는 캐시 무효화. 카드를 다시 만들지 않아 열어 둔 폼·필터·페이지 유지
            타던 차의 첫 단계 안내. 다 끝나면 스스로 사라짐
          */}
          <GettingStartedCard
            vehicle={vehicle}
            onServicesSaved={() => {
              clearOdometerConflict()
              // 다른 종류로 저장된 기록이 안 보이면 실패로 오해. 첫 장·전체로
              setMaintenanceView(ALL_MAINTENANCE)
              // 그때 주행거리를 적었다면 차량 값이 올랐을 수 있음
              void invalidateAfterMaintenance(queryClient, id)
            }}
          />

          <NextServiceCard vehicleId={vehicle.id} />

          <MaintenanceSection
            vehicleId={vehicle.id}
            currentOdometer={vehicle.odometer}
            view={maintenanceView}
            onViewChange={setMaintenanceView}
            onChanged={clearOdometerConflict}
          />

          <FuelSummaryCard vehicleId={vehicle.id} />

          {/* 시작하기 카드가 스크롤해 오는 자리 */}
          {/* tabIndex -1: 스크롤과 함께 포커스도 옮기려면 필요 */}
          <div id="fuel-section" tabIndex={-1} className="scroll-mt-28 outline-none">
            <FuelSection
              vehicleId={vehicle.id}
              currentOdometer={vehicle.odometer}
              onChanged={clearOdometerConflict}
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
