import { useCallback, useState } from 'react'
import { ChevronRight } from 'lucide-react'
import { Link } from 'react-router'

import { Button } from '@/shared/ui/button'
import { GaugeMark } from '@/shared/ui/mark'
import { Pagination } from '@/shared/ui/pagination'
import { Page } from '@/shared/ui/page'
import { ErrorText, Skeleton } from '@/shared/ui/state'
import { formatNumber } from '@/shared/lib/format'
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
    <Page
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
    >
      {/* 격자와 페이지 이동은 한 덩어리다. Page 기본 간격(gap-10)으로 벌리면 너무 멀어진다. */}
      <div className="flex flex-col gap-6">

      {/*
        한 줄짜리 가로 행이 아니라 격자로 깐다. 넓은 화면에서 행은 오른쪽 60%가 통째로
        비어 버리는데, 격자는 같은 공간에 차량을 3배로 담으면서 카드 하나의 폭은
        오히려 적당해진다. lg에서 2열, xl에서 3열 — lg부터 3열로 가면 카드가 315px까지
        좁아져 "현대 아반떼" 같은 짧은 이름도 줄바꿈된다.
      */}
        <ul className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {data.items.map((vehicle) => (
            <li key={vehicle.id}>
              <Link
                to={`/vehicles/${vehicle.id}`}
                // h-full: 격자에서 카드 높이를 줄에 맞춰 늘린다. 없으면 메모가 긴 카드만 키가 커서
                // 줄이 들쭉날쭉해진다.
                // group: 이 링크에 마우스가 올라갔을 때 안쪽 화살표(group-hover)도 같이 반응시킨다.
                className="group flex h-full flex-col justify-between gap-8 rounded-2xl border border-border bg-card p-6 backdrop-blur-[20px] transition-all duration-200 ease-apple hover:border-border-strong hover:bg-card-hover active:scale-[0.995]"
              >
                <div className="flex flex-col gap-1.5">
                  <p className="truncate text-[0.6875rem] font-medium tracking-[0.16em] text-muted-foreground uppercase">
                    {vehicle.plateNumber}
                  </p>
                  <p className="truncate text-[1.0625rem] font-semibold tracking-[-0.02em] text-strong">
                    {vehicle.manufacturer} {vehicle.modelName}
                  </p>
                  <p className="text-[0.8125rem] text-muted-foreground">
                    {vehicle.modelYear === null ? '연식 미상' : `${vehicle.modelYear}년식`}
                  </p>
                </div>

                <div className="flex items-end justify-between gap-3">
                  <div className="flex flex-col gap-0.5">
                    <p className="text-[1.75rem] leading-none font-semibold tracking-[-0.035em] tabular-nums text-strong">
                      {formatNumber(vehicle.odometer)}
                      <span className="ml-1 text-sm font-normal tracking-normal text-muted-foreground">
                        km
                      </span>
                    </p>
                    <p className="text-[0.625rem] tracking-[0.14em] text-muted-foreground uppercase">
                      Odometer
                    </p>
                  </div>
                  {/* 2px만 움직인다. 화살표가 크게 미끄러지면 장난스러워진다. */}
                  <ChevronRight className="size-4 shrink-0 text-faint transition-transform duration-200 ease-apple group-hover:translate-x-0.5" />
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
    </Page>
  )
}

/**
 * 로딩 중에도 실제 목록과 같은 격자·높이의 덩어리를 깔아 둔다.
 * "불러오는 중…" 한 줄만 보여주면 데이터가 도착하는 순간 화면이 통째로 튀어 오른다.
 */
function VehicleListSkeleton() {
  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-2">
        <Skeleton className="h-3 w-16" />
        <Skeleton className="h-9 w-40" />
      </div>
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        {[0, 1, 2, 3, 4, 5].map((row) => (
          <Skeleton key={row} className="h-[11.5rem] rounded-2xl" />
        ))}
      </div>
    </div>
  )
}

/**
 * 빈 상태. 여백을 아주 크게 잡는다. 할 일이 하나뿐인 화면에서는
 * 공백 자체가 "여기를 누르라"는 안내가 된다.
 */
function EmptyGarage() {
  return (
    <div className="flex flex-col items-center gap-7 rounded-3xl border border-border bg-card px-8 py-24 text-center backdrop-blur-[20px]">
      <GaugeMark className="size-10 text-muted-foreground" />

      <div className="flex max-w-sm flex-col gap-2">
        <p className="text-[1.0625rem] font-semibold tracking-[-0.02em] text-strong">
          아직 등록된 차량이 없습니다
        </p>
        <p className="text-sm leading-relaxed text-muted-foreground">
          차량을 등록하면 정비 이력과 다음 정비 시점을 함께 관리할 수 있습니다.
        </p>
      </div>

      <Button render={<Link to="/vehicles/new" />}>첫 차량 등록하기</Button>
    </div>
  )
}
