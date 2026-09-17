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
 * '/' 의 세 얼굴
 *   비로그인          → 소개 화면
 *   로그인 + 0대      → 등록 권유
 *   로그인 + 1대 이상 → 통계
 * 여러 기능의 데이터를 모으는 화면이라 app/ 소속
 */
export function HomePage() {
  const { user, loading } = useAuth()

  // 세션 복구 전에 판단하면 로그인 상태에서도 소개 화면이 한 번 깜빡임
  if (loading) {
    return <LoadingText className="justify-center py-24" />
  }

  if (user === null) {
    return <LandingPage />
  }

  return <Dashboard nickname={user.nickname} />
}

function Dashboard({ nickname }: { nickname: string }) {
  // 모듈 최상단의 고정 함수라 useCallback 불필요
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
        <Button size="sm" variant="secondary" render={<Link to="/vehicles" />}>
          내 차량
        </Button>
      }
    >
      <StatTiles data={data} />

      {/* 히어로 숫자를 품은 차트가 중심이라 전체 폭 */}
      <MonthlyCostChart monthly={data.monthly} />

      {/* 두 카드를 나란히. 세로로 쌓으면 넓은 화면에서 오른쪽 절반이 통째로 빔 */}
      <div className="grid gap-6 lg:grid-cols-2">
        <TypeCostChart byType={data.byType} />
        <RecentActivities recent={data.recent} />
      </div>

      <VehicleBreakdown vehicles={data.vehicles} />
    </Page>
  )
}

function StatTiles({ data }: { data: HomeData }) {
  // 전부 서버 집계값. exact 플래그와 "일부만 합산됨" 단서는 요약 API 와 함께 사라짐
  const tiles = [
    { label: 'Vehicles', value: formatNumber(data.vehicleCount), unit: '대', note: null },
    { label: 'Distance', value: formatNumber(data.totalOdometer), unit: 'km', note: null },
    { label: 'Records', value: formatNumber(data.recordCount), unit: '건', note: null },
    {
      label: 'Cost',
      value: formatNumber(data.totalCost),
      unit: '원',
      // 합계만으로는 어느 쪽이 큰지 모름. 유류비 비중이 커서 이 한 줄이 필요
      note: `정비 ${formatNumber(data.maintenanceCost)} · 주유 ${formatNumber(data.fuelCost)}`,
    },
  ]

  return (
    /*
      칸 사이 1px 틈으로 뒷배경(선 색)이 비침. 칸마다 border 면 맞닿는 자리가 2px 이 됨
      이 구조라 등장 연출 불가 — 칸이 투명한 동안 격자 전체가 선 색으로 번쩍임
    */
    <dl className="grid gap-px overflow-hidden border border-border bg-border sm:grid-cols-2 lg:grid-cols-4">
      {tiles.map(({ label, value, unit, note }) => (
        <div key={label} className="flex flex-col gap-4 bg-background p-5 sm:gap-5 sm:p-8">
          <dt className="text-eyebrow text-muted-foreground uppercase">{label}</dt>
          {/* 큰 숫자에는 tabular-nums 금지 — 세로로 맞출 상대가 있는 아래 목록에만
              굵기도 낮춤. 굵게 키우면 숫자가 뭉쳐 보임 */}
          <dd className="flex items-baseline gap-1.5 text-[clamp(2rem,1.2rem+3.2vw,2.75rem)] leading-[0.95] font-light tracking-[-0.045em] text-strong">
            {value}
            <span className="text-[0.8125rem] font-normal tracking-normal text-muted-foreground">
              {unit}
            </span>
          </dd>

          {note !== null && <p className="text-xs tabular-nums text-muted-foreground">{note}</p>}
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
                  <p className="truncate text-[0.9375rem] tracking-[-0.01em] text-strong">
                    {line.manufacturer} {line.modelName}
                  </p>
                  <p className="truncate text-xs text-muted-foreground">
                    {line.plateNumber} · 정비 {line.maintenanceCount}건
                    {/* 서버 계산값. 주유 2건 미만이면 null 이고 그때는 자리를 비움
                        '연비 —' 를 붙이면 없는 값이 자리를 차지함 */}
                    {line.averageEfficiency !== null &&
                      ` · ${line.averageEfficiency.toFixed(1)}km/L`}
                  </p>
                </div>

                <div className="shrink-0 text-right">
                  <p className="text-[0.9375rem] tabular-nums text-strong">
                    {formatKm(line.odometer)}
                  </p>
                  <p className="text-xs tabular-nums text-muted-foreground">
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
          <p className="py-4 text-sm text-muted-foreground">아직 등록된 기록이 없습니다.</p>
        ) : (
          <ul className="divide-y divide-border">
            {recent.map((item) => (
              /* key 에 kind 를 섞음 — 테이블이 달라 id 가 겹침 */
              <li
                key={`${item.kind}-${item.recordId}`}
                className="flex items-start justify-between gap-4 py-5 first:pt-0 last:pb-0"
              >
                <div className="flex min-w-0 flex-col gap-0.5">
                  <p className="text-[0.9375rem] tracking-[-0.01em] text-strong">
                    {item.type === null ? '주유' : SERVICE_TYPE_LABELS[item.type]}
                  </p>
                  <p className="truncate text-xs text-muted-foreground">
                    {item.vehicleName}
                    {/* 주유는 종류가 하나뿐이라 제목만으로 구분이 안 됨
                        넣은 양을 붙여 무슨 기록인지 한눈에 */}
                    {item.liters !== null && ` · ${item.liters.toFixed(2)}L`}
                  </p>
                </div>

                <div className="shrink-0 text-right">
                  <p className="text-[0.9375rem] tabular-nums text-strong">
                    {formatWon(item.cost)}
                  </p>
                  <p className="text-xs tabular-nums text-muted-foreground">
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

/**
 * 로그인 + 차량 0대
 * 차량 목록의 빈 상태와 문구가 다름 — 여기는 "앱을 시작하는 자리", 저기는 "목록이 빈 자리"
 */
function EmptyGarage() {
  return (
    <Page eyebrow="Garage" title="내 차고" description="차량을 등록하면 여기에 통계가 모입니다.">
      <div className="flex flex-col items-center gap-7 border-y border-border px-5 py-20 text-center sm:gap-8 sm:px-8 sm:py-32">
        <GaugeMark className="size-10 text-muted-foreground" />

        <div className="flex max-w-sm flex-col gap-2">
          <p className="text-section text-strong">
            아직 등록된 차량이 없습니다
          </p>
          <p className="text-sm leading-relaxed text-muted-foreground">
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
