import { useCallback, useState } from 'react'
import { ChevronRight } from 'lucide-react'
import { Link } from 'react-router'

import { Button } from '@/shared/ui/button'
import { Pagination } from '@/shared/ui/pagination'
import { PageHeader } from '@/shared/ui/page-header'
import { ErrorText, Skeleton } from '@/shared/ui/state'
import { formatKm } from '@/shared/lib/format'
import { useAsyncData } from '@/shared/lib/useAsyncData'
import { fetchVehicles } from '@/features/vehicles/api/endpoints'

export function VehicleListPage() {
  const [page, setPage] = useState(0)

  // page가 바뀔 때마다 새 함수가 만들어지고, 그걸 본 useAsyncData가 다시 불러온다.
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
    <div className="flex flex-col gap-8">
      <PageHeader
        eyebrow="Garage"
        title="내 차량"
        // 대수는 제목에 붙이지 않고 설명 줄로 내린다. 제목이 숫자 때문에 길어지면
        // 대수가 바뀔 때마다 제목의 무게중심이 흔들린다.
        description={`${data.totalElements}대를 관리 중입니다.`}
        action={
          // render: 버튼 스타일을 <Link>에 입힌다. <button> 안에 <a>를 넣으면 잘못된 HTML이 된다.
          <Button size="sm" render={<Link to="/vehicles/new" />}>
            차량 등록
          </Button>
        }
      />

      <ul className="flex flex-col gap-2.5">
        {data.items.map((vehicle) => (
          <li key={vehicle.id}>
            <Link
              to={`/vehicles/${vehicle.id}`}
              // group: 이 링크에 마우스가 올라갔을 때 안쪽 화살표(group-hover)도 같이 반응시킨다.
              // 호버에서 커지거나 색이 변하지 않는다. 배경 농도 3% → 5.5%, 테두리 8% → 14%.
              className="group flex items-center justify-between gap-4 rounded-2xl border border-border bg-card px-6 py-5 backdrop-blur-[20px] transition-all duration-200 ease-apple hover:border-border-strong hover:bg-card-hover active:scale-[0.995]"
            >
              <div className="flex min-w-0 flex-col gap-1">
                <p className="truncate text-[0.9375rem] font-medium tracking-[-0.01em] text-strong">
                  {vehicle.manufacturer} {vehicle.modelName}
                </p>
                <p className="truncate text-[0.8125rem] text-muted-foreground">
                  {vehicle.plateNumber}
                  {vehicle.modelYear !== null && ` · ${vehicle.modelYear}년식`}
                </p>
              </div>

              <div className="flex shrink-0 items-center gap-3.5">
                <div className="text-right">
                  <p className="text-[0.9375rem] tracking-[-0.01em] tabular-nums text-strong">
                    {formatKm(vehicle.odometer)}
                  </p>
                  <p className="text-[0.625rem] tracking-[0.14em] text-muted-foreground uppercase">Odometer</p>
                </div>
                {/* 2px만 움직인다. 화살표가 크게 미끄러지면 장난스러워진다. */}
                <ChevronRight className="size-4 text-faint transition-transform duration-200 ease-apple group-hover:translate-x-0.5" />
              </div>
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
  )
}

/**
 * 로딩 중에도 실제 목록과 같은 높이·간격의 덩어리를 깔아 둔다.
 * "불러오는 중…" 한 줄만 보여주면 데이터가 도착하는 순간 화면이 통째로 튀어 오른다.
 */
function VehicleListSkeleton() {
  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-2">
        <Skeleton className="h-3 w-16" />
        <Skeleton className="h-9 w-40" />
      </div>
      <div className="flex flex-col gap-2.5">
        {[0, 1, 2].map((row) => (
          <Skeleton key={row} className="h-[5.75rem] rounded-2xl" />
        ))}
      </div>
    </div>
  )
}

/**
 * 빈 상태. 여백을 아주 크게(py-20) 잡는다. 할 일이 하나뿐인 화면에서는
 * 공백 자체가 "여기를 누르라"는 안내가 된다.
 */
function EmptyGarage() {
  return (
    <div className="flex flex-col items-center gap-7 rounded-3xl border border-border bg-card px-8 py-20 text-center backdrop-blur-[20px]">
      <svg viewBox="0 0 32 32" className="size-10 text-faint" aria-hidden="true">
        <path
          d="M8 20a8 8 0 1 1 16 0"
          fill="none"
          stroke="currentColor"
          strokeOpacity="0.5"
          strokeWidth="2"
          strokeLinecap="round"
        />
        <path
          d="M16 20 21 13"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
        />
      </svg>

      <div className="flex flex-col gap-2">
        <p className="text-[1.0625rem] font-semibold tracking-[-0.02em] text-strong">
          아직 등록된 차량이 없습니다
        </p>
        <p className="text-sm text-muted-foreground">
          차량을 등록하면 정비 이력과 다음 정비 시점을 함께 관리할 수 있습니다.
        </p>
      </div>

      <Button render={<Link to="/vehicles/new" />}>첫 차량 등록하기</Button>
    </div>
  )
}
