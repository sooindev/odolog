import { useCallback, useState } from 'react'
import { ChevronRight } from 'lucide-react'
import { Link } from 'react-router'

import { Button } from '@/shared/ui/base/button'
import { GaugeMark } from '@/shared/ui/brand/mark'
import { Pagination } from '@/shared/ui/nav/pagination'
import { Page } from '@/shared/ui/layout/page'
import { ErrorText, Skeleton } from '@/shared/ui/feedback/state'
import { formatNumber } from '@/shared/lib/format/format'
import { useAsyncData } from '@/shared/lib/hooks/useAsyncData'
import { fetchVehicles } from '@/features/vehicles/api/endpoints/endpoints'

export function VehicleListPage() {
  const [page, setPage] = useState(0)

  // page 변경 시 재조회
  const load = useCallback(() => fetchVehicles(page), [page])
  const { data, loading, error } = useAsyncData(load, '차량 목록을 불러오지 못했습니다.')

  if (loading) {
    return <VehicleListSkeleton />
  }

  if (error !== null) {
    return <ErrorText message={error} />
  }

  if (data === null || data.totalElements === 0) {
    return <EmptyGarage />
  }

  return (
    <Page
      eyebrow="Garage"
      title="내 차량"
      // 대수는 설명에. 제목 길이 고정
      description={`${data.totalElements}대를 관리 중입니다.`}
      action={
        // render: 버튼 스타일을 Link 에. button 안 a 금지
        <Button size="sm" render={<Link to="/vehicles/new" />}>
          차량 등록
        </Button>
      }
    >
      {/* 목록과 페이지 이동 한 덩어리 */}
      <div className="flex flex-col gap-10">
        {/* 카드 대신 괘선 행. 번호판·주행거리 세로 정렬로 여러 대 비교 */}
        <ul className="border-t border-border">
          {data.items.map((vehicle) => (
            <li key={vehicle.id} className="border-b border-border">
              <Link
                to={`/vehicles/${vehicle.id}`}
                // 행 전체가 판정 영역
                // before: 호버 시 왼쪽 1px 세로 표식
                className="group relative -mx-3 flex items-center gap-4 px-3 py-5 transition-colors duration-200 ease-apple sm:-mx-4 sm:gap-10 sm:px-4 sm:py-7 before:absolute before:top-0 before:bottom-0 before:left-0 before:w-px before:origin-top before:scale-y-0 before:bg-strong before:transition-transform before:duration-300 before:ease-apple hover:bg-wash hover:before:scale-y-100 focus-visible:before:scale-y-100"
              >
                <div className="flex min-w-0 flex-1 flex-col gap-2">
                  <p className="truncate text-eyebrow text-muted-foreground uppercase">
                    {vehicle.plateNumber}
                  </p>
                  <p className="flex min-w-0 items-baseline gap-2">
                    <span className="truncate text-section text-strong">
                      {vehicle.manufacturer} {vehicle.modelName}
                    </span>
                    {/* 지난 정비 표시. 빨강 대신 테두리(NextServiceCard 참고) */}
                    {vehicle.overdueServiceCount !== null && vehicle.overdueServiceCount > 0 && (
                      <span className="shrink-0 border border-strong/30 px-1.5 py-0.5 text-unit font-medium text-strong">
                        정비 {vehicle.overdueServiceCount}건 지남
                      </span>
                    )}
                  </p>
                  <p className="text-caption text-muted-foreground">
                    {vehicle.modelYear === null ? '연식 미상' : `${vehicle.modelYear}년식`}
                  </p>
                </div>

                {/* 수치는 오른쪽 끝, tabular-nums */}
                <div className="flex shrink-0 flex-col items-end gap-2">
                  {/* 좁은 화면에서는 숨김. 아래 km 과 중복, 넓은 자간이 폭 차지 */}
                  <p className="hidden text-eyebrow text-faint uppercase sm:block">Odometer</p>
                  <p className="text-figure tabular-nums text-strong">
                    {formatNumber(vehicle.odometer)}
                    <span className="ml-1.5 text-caption tracking-normal text-muted-foreground">
                      km
                    </span>
                  </p>
                </div>

                {/* 2px 이동만 */}
                <ChevronRight className="size-4 shrink-0 text-faint transition-transform duration-200 ease-apple group-hover:translate-x-0.5" />
              </Link>
            </li>
          ))}
        </ul>

        <Pagination
          page={data.page}
          totalPages={data.totalPages}
          hasNext={data.hasNext}
          onChange={setPage}
        />
      </div>
    </Page>
  )
}

/** 실제 목록과 같은 높이 확보. 도착 시 흔들림 방지 */
function VehicleListSkeleton() {
  return (
    <div className="flex flex-col gap-12">
      <div className="flex flex-col gap-4">
        <Skeleton className="h-3 w-16" />
        <Skeleton className="h-12 w-56" />
      </div>
      <div className="border-t border-border">
        {[0, 1, 2, 3].map((row) => (
          <div key={row} className="border-b border-border py-7">
            <Skeleton className="h-12" />
          </div>
        ))}
      </div>
    </div>
  )
}

/** 빈 상태 */
function EmptyGarage() {
  return (
    <div className="flex flex-col items-center gap-7 border-y border-border px-5 py-20 text-center sm:gap-8 sm:px-8 sm:py-32">
      <GaugeMark className="size-10 text-muted-foreground" />

      <div className="flex max-w-sm flex-col gap-2">
        <p className="text-section text-strong">아직 등록된 차량이 없습니다</p>
        <p className="text-caption leading-relaxed text-muted-foreground">
          차량을 등록하면 정비 이력과 다음 정비 시점, 주유 기록과 연비를 함께 관리할 수 있습니다.
        </p>
      </div>

      <Button render={<Link to="/vehicles/new" />}>첫 차량 등록하기</Button>
    </div>
  )
}
