import { Link } from 'react-router'

import { MonthlyCostChart, TypeCostChart } from '@/features/summary/components/charts/HomeCharts'
import type { HomeData } from '@/features/summary/api/types'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { Page } from '@/shared/ui/layout/page'
import { Skeleton } from '@/shared/ui/state'
import { useI18n } from '@/shared/i18n/I18nContext'

/** 홈 통계. 차량이 한 대 이상일 때의 얼굴 */
export function Dashboard({ data, nickname }: { data: HomeData; nickname: string }) {
  const { t } = useI18n()

  return (
    <Page
      eyebrow="Overview"
      title={t.home.title(nickname)}
      description={t.home.description}
      action={
        // 한 대면 그 차로 바로 이동, 여러 대면 목록으로
        <Button
          size="sm"
          variant="secondary"
          render={
            <Link to={data.vehicles.length === 1 ? `/vehicles/${data.vehicles[0].id}` : '/vehicles'} />
          }
        >
          {data.vehicles.length === 1 ? t.home.goRecord : t.home.myVehicles}
        </Button>
      }
    >
      <StatTiles data={data} />

      {/* 통화가 다른 기록은 더할 수 없어 뺌. 말없이 빼지 않음 */}
      {data.otherCurrencyRecordCount > 0 && (
        <p className="-mt-4 text-caption text-muted-foreground sm:-mt-8">
          {t.home.otherCurrency(data.otherCurrencyRecordCount, data.currency)}
        </p>
      )}

      {/* 히어로 숫자가 있는 차트라 전체 폭 */}
      <MonthlyCostChart monthly={data.monthly} currency={data.currency} />

      {/* 두 카드 나란히 */}
      <div className="grid gap-6 lg:grid-cols-2">
        <TypeCostChart byType={data.byType} currency={data.currency} />
        <RecentActivities recent={data.recent} />
      </div>

      <VehicleBreakdown vehicles={data.vehicles} />
    </Page>
  )
}

function StatTiles({ data }: { data: HomeData }) {
  const { t, f } = useI18n()
  const cost = f.moneyParts(data.totalCost, data.currency)

  // 전부 서버 집계값
  const tiles = [
    { label: 'Vehicles', value: f.number(data.vehicleCount), unit: t.home.tiles.vehicles, note: null },
    { label: 'Distance', value: f.distanceNumber(data.totalOdometer), unit: f.distanceUnit, note: null },
    { label: 'Records', value: f.number(data.recordCount), unit: t.home.tiles.records, note: null },
    {
      label: 'Cost',
      value: cost.value,
      unit: cost.unit,
      // 정비·주유 구성 표시
      note: t.home.tiles.breakdown(
        f.moneyParts(data.maintenanceCost, data.currency).value,
        f.moneyParts(data.fuelCost, data.currency).value,
      ),
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
  const { t, f } = useI18n()

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t.home.vehicles.title}</CardTitle>
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
                        {t.vehicles.overdue(line.overdueServiceCount)}
                      </span>
                    )}
                    {line.dueSoonServiceCount > 0 && (
                      <span className="shrink-0 border border-border px-1.5 py-0.5 text-unit text-muted-foreground">
                        {t.vehicles.dueSoon(line.dueSoonServiceCount)}
                      </span>
                    )}
                  </p>
                  <p className="truncate text-caption text-muted-foreground">
                    {line.plateNumber} · {t.home.vehicles.maintenanceCount(line.maintenanceCount)}
                    {/* 평균 연비. 없으면 자리 자체를 비움 */}
                    {line.averageEfficiency !== null && ` · ${f.efficiency(line.averageEfficiency, 1)}`}
                  </p>
                </div>

                <div className="shrink-0 text-right">
                  <p className="text-body tabular-nums text-strong">
                    {f.distance(line.odometer)}
                  </p>
                  <p className="text-caption tabular-nums text-muted-foreground">
                    {line.lastServiceDate === null ? t.home.vehicles.noService : f.date(line.lastServiceDate)}
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
  const { t, f } = useI18n()

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t.home.recent.title}</CardTitle>
      </CardHeader>
      <CardContent>
        {recent.length === 0 ? (
          <p className="py-4 text-caption text-muted-foreground">{t.home.recent.empty}</p>
        ) : (
          <ul className="divide-y divide-border">
            {recent.map((item) => (
              // key 에 kind 포함. 테이블이 달라 id 충돌 가능
              <li key={`${item.kind}-${item.recordId}`} className="py-5 first:pt-0 last:pb-0">
                {/* 그 차량 상세로. 차량별 목록과 같은 모양 */}
                <Link
                  to={`/vehicles/${item.vehicleId}`}
                  className="flex items-start justify-between gap-4 transition-opacity duration-200 ease-apple hover:opacity-70"
                >
                <div className="flex min-w-0 flex-col gap-0.5">
                  <p className="text-body text-strong">
                    {item.type === null ? t.home.recent.fuel : t.serviceTypes[item.type]}
                  </p>
                  <p className="truncate text-caption text-muted-foreground">
                    {item.vehicleName}
                    {/* 주유는 넣은 양 표시 */}
                    {item.liters !== null && ` · ${f.volume(item.liters)}`}
                  </p>
                </div>

                <div className="shrink-0 text-right">
                  <p className="text-body tabular-nums text-strong">
                    {/* 금액 없음은 —. 주유 목록과 같은 표기. 기록마다 자기 통화 */}
                    {item.cost === null ? '—' : f.money(item.cost, item.currency)}
                  </p>
                  <p className="text-caption tabular-nums text-muted-foreground">
                    {f.date(item.date)}
                  </p>
                </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

export function DashboardSkeleton() {
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
