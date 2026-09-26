import { Link } from 'react-router'

import { useAuth } from '@/features/auth/context/definition/AuthContext'
import { SERVICE_TYPE_LABELS } from '@/features/maintenance/api/types/types'
import { LandingPage } from '@/app/landing/LandingPage'
import { MonthlyCostChart, TypeCostChart } from '@/app/home/charts/HomeCharts'
import { loadHomeData } from '@/app/home/stats/homeStats'
import type { HomeData } from '@/app/home/stats/homeStats'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { GaugeMark } from '@/shared/ui/brand/mark'
import { Page } from '@/shared/ui/layout/page'
import { ErrorText, LoadingText, Skeleton } from '@/shared/ui/feedback/state'
import { formatDate, formatKm, formatNumber, formatWon } from '@/shared/lib/format/format'
import { useAsyncData } from '@/shared/lib/hooks/useAsyncData'

/**
 * '/' 의 세 얼굴: 비로그인 → 소개, 0대 → 등록 권유, 1대 이상 → 통계
 * 여러 기능의 데이터를 모으는 화면이라 app/ 소속
 */
export function HomePage() {
  const { user, loading } = useAuth()

  // 세션 복구 전 판단 금지. 소개 화면 깜빡임 방지
  if (loading) {
    return <LoadingText className="justify-center py-24" />
  }

  if (user === null) {
    return <LandingPage />
  }

  return <Dashboard nickname={user.nickname} />
}

function Dashboard({ nickname }: { nickname: string }) {
  // 모듈 최상단 함수라 useCallback 불필요
  const { data, loading, error } = useAsyncData(loadHomeData, '차고 정보를 불러오지 못했습니다.')

  if (loading) {
    return <DashboardSkeleton />
  }

  if (error !== null) {
    return <ErrorText message={error} />
  }

  if (data === null || data.vehicleCount === 0) {
    return <EmptyGarage />
  }

  return (
    <Page
      eyebrow="Overview"
      title={`${nickname}님의 차고`}
      description="차량과 정비·주유 기록, 들어간 유지비를 한눈에 봅니다."
      action={
        // 한 대면 그 차로 바로 이동, 여러 대면 목록으로
        <Button
          size="sm"
          variant="secondary"
          render={
            <Link to={data.vehicles.length === 1 ? `/vehicles/${data.vehicles[0].id}` : '/vehicles'} />
          }
        >
          {data.vehicles.length === 1 ? '기록하러 가기' : '내 차량'}
        </Button>
      }
    >
      <StatTiles data={data} />

      {/* 히어로 숫자가 있는 차트라 전체 폭 */}
      <MonthlyCostChart monthly={data.monthly} />

      {/* 두 카드 나란히 */}
      <div className="grid gap-6 lg:grid-cols-2">
        <TypeCostChart byType={data.byType} />
        <RecentActivities recent={data.recent} />
      </div>

      <VehicleBreakdown vehicles={data.vehicles} />
    </Page>
  )
}

function StatTiles({ data }: { data: HomeData }) {
  // 전부 서버 집계값
  const tiles = [
    { label: 'Vehicles', value: formatNumber(data.vehicleCount), unit: '대', note: null },
    { label: 'Distance', value: formatNumber(data.totalOdometer), unit: 'km', note: null },
    { label: 'Records', value: formatNumber(data.recordCount), unit: '건', note: null },
    {
      label: 'Cost',
      value: formatNumber(data.totalCost),
      unit: '원',
      // 정비·주유 구성 표시
      note: `정비 ${formatNumber(data.maintenanceCost)} · 주유 ${formatNumber(data.fuelCost)}`,
    },
  ]

  return (
    // gap-px 격자. 1px 틈으로 선 색이 비침
    // 칸이 투명해지는 등장 연출 금지
    <dl className="grid gap-px overflow-hidden border border-border bg-border sm:grid-cols-2 lg:grid-cols-4">
      {tiles.map(({ label, value, unit, note }) => (
        <div key={label} className="flex flex-col gap-4 bg-background p-5 sm:gap-5 sm:p-8">
          <dt className="text-eyebrow text-muted-foreground uppercase">{label}</dt>
          {/* 큰 숫자라 tabular-nums 미사용, 굵기 낮춤 */}
          <dd className="flex items-baseline gap-1.5 text-[clamp(2rem,1.2rem+3.2vw,2.75rem)] leading-[0.95] font-light tracking-[-0.045em] text-strong">
            {value}
            <span className="text-caption font-normal tracking-normal text-muted-foreground">
              {unit}
            </span>
          </dd>

          {note !== null && <p className="text-caption tabular-nums text-muted-foreground">{note}</p>}
        </div>
      ))}
    </dl>
  )
}

function VehicleBreakdown({ vehicles }: { vehicles: HomeData['vehicles'] }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>차량별</CardTitle>
      </CardHeader>
      <CardContent>
        <ul className="divide-y divide-border">
          {vehicles.map((line) => (
            <li key={line.id} className="py-5 first:pt-0 last:pb-0">
              <Link
                to={`/vehicles/${line.id}`}
                className="flex items-center justify-between gap-4 transition-opacity duration-200 ease-apple hover:opacity-70"
              >
                <div className="flex min-w-0 flex-col gap-0.5">
                  <p className="flex min-w-0 items-baseline gap-2">
                    <span className="truncate text-body text-strong">
                      {line.manufacturer} {line.modelName}
                    </span>
                    {/* 지난 정비 표시. 빨강 대신 테두리(NextServiceCard 참고) */}
                    {line.overdueServiceCount > 0 && (
                      <span className="shrink-0 border border-strong/30 px-1.5 py-0.5 text-unit font-medium text-strong">
                        정비 {line.overdueServiceCount}건 지남
                      </span>
                    )}
                  </p>
                  <p className="truncate text-caption text-muted-foreground">
                    {line.plateNumber} · 정비 {line.maintenanceCount}건
                    {/* 평균 연비. 없으면 자리 자체를 비움 */}
                    {line.averageEfficiency !== null &&
                      ` · ${line.averageEfficiency.toFixed(1)}km/L`}
                  </p>
                </div>

                <div className="shrink-0 text-right">
                  <p className="text-body tabular-nums text-strong">
                    {formatKm(line.odometer)}
                  </p>
                  <p className="text-caption tabular-nums text-muted-foreground">
                    {line.lastServiceDate === null
                      ? '정비 이력 없음'
                      : formatDate(line.lastServiceDate)}
                  </p>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}

function RecentActivities({ recent }: { recent: HomeData['recent'] }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>최근 활동</CardTitle>
      </CardHeader>
      <CardContent>
        {recent.length === 0 ? (
          <p className="py-4 text-caption text-muted-foreground">아직 등록된 기록이 없습니다.</p>
        ) : (
          <ul className="divide-y divide-border">
            {recent.map((item) => (
              // key 에 kind 포함. 테이블이 달라 id 충돌 가능
              <li
                key={`${item.kind}-${item.recordId}`}
                className="flex items-start justify-between gap-4 py-5 first:pt-0 last:pb-0"
              >
                <div className="flex min-w-0 flex-col gap-0.5">
                  <p className="text-body text-strong">
                    {item.type === null ? '주유' : SERVICE_TYPE_LABELS[item.type]}
                  </p>
                  <p className="truncate text-caption text-muted-foreground">
                    {item.vehicleName}
                    {/* 주유는 넣은 양 표시 */}
                    {item.liters !== null && ` · ${item.liters.toFixed(2)}L`}
                  </p>
                </div>

                <div className="shrink-0 text-right">
                  <p className="text-body tabular-nums text-strong">
                    {/* 금액 없음은 — 원. 주유 목록과 같은 표기 */}
                    {item.cost === null ? '— 원' : formatWon(item.cost)}
                  </p>
                  <p className="text-caption tabular-nums text-muted-foreground">
                    {formatDate(item.date)}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

/** 로그인 + 차량 0대. 앱을 시작하는 자리의 문구 */
function EmptyGarage() {
  return (
    <Page eyebrow="Garage" title="내 차고" description="차량을 등록하면 여기에 통계가 모입니다.">
      <div className="flex flex-col items-center gap-7 border-y border-border px-5 py-20 text-center sm:gap-8 sm:px-8 sm:py-32">
        <GaugeMark className="size-10 text-muted-foreground" />

        <div className="flex max-w-sm flex-col gap-2">
          <p className="text-section text-strong">
            아직 등록된 차량이 없습니다
          </p>
          <p className="text-caption leading-relaxed text-muted-foreground">
            차량을 등록하면 주행거리와 정비 기록, 들어간 비용이 이 화면에 모입니다.
          </p>
        </div>

        <Button render={<Link to="/vehicles/new" />}>첫 차량 등록하기</Button>
      </div>
    </Page>
  )
}

function DashboardSkeleton() {
  return (
    <div className="flex flex-col gap-10">
      <div className="flex flex-col gap-2">
        <Skeleton className="h-3 w-20" />
        <Skeleton className="h-9 w-56" />
        <Skeleton className="h-4 w-64" />
      </div>
      <Skeleton className="h-28" />
      <Skeleton className="h-96" />
      <div className="grid gap-6 lg:grid-cols-2">
        <Skeleton className="h-72" />
        <Skeleton className="h-72" />
      </div>
      <Skeleton className="h-48" />
    </div>
  )
}
