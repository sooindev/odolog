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
      {/* 목록과 페이지 이동은 한 덩어리다. Page 기본 간격으로 벌리면 너무 멀어진다. */}
      <div className="flex flex-col gap-10">
        {/*
          카드 격자가 아니라 **괘선으로 나눈 행**이다. 카드를 하나씩 씌우면 차량 수만큼
          상자가 늘어나 화면이 상자 목록이 된다. 행으로 깔면 번호판은 번호판끼리,
          주행거리는 주행거리끼리 세로로 정렬되어 **여러 대를 훑어 비교할 수 있다** —
          목록에서 실제로 하는 일이 그것이다.

          넓은 화면에서 행이 길어지는 문제는 폭이 아니라 정렬로 푼다. 왼쪽은 이름,
          오른쪽은 수치로 밀어 두면 가운데 여백이 둘을 갈라 주는 홈이 된다.
          (디자인 시스템 11번의 "700px 넘기지 않는다"는 **글이 담기는 열**에 대한 규칙이고,
          이런 표 형태의 행은 그 예외다.)
        */}
        <ul className="border-t border-border">
          {data.items.map((vehicle) => (
            <li key={vehicle.id} className="border-b border-border">
              <Link
                to={`/vehicles/${vehicle.id}`}
                // group: 행 전체에 마우스가 올라갔을 때 안쪽 화살표도 같이 반응시킨다.
                // 행 전체가 판정 영역이라 -mx/px 로 좌우에 여유를 준다.
                //
                // before:*: 행 왼쪽 끝에 1px 표식이 **위에서 아래로 그어진다.**
                // 배경만 옅게 바뀌면 어느 행에 있는지 훑어봐야 알 수 있는데, 왼쪽 끝에 선이
                // 하나 서면 눈이 그 자리를 바로 찾는다. 목록의 가로 괘선과 같은 1px 이라
                // 새로운 요소가 아니라 **이미 있던 선 하나가 세로로 서는 것**으로 보인다.
                // 세로로 자라게 한 이유: 가로로 늘리면 글자를 밀어내는 것처럼 보인다.
                className="group relative -mx-4 flex items-center gap-6 px-4 py-7 transition-colors duration-200 ease-apple before:absolute before:top-0 before:bottom-0 before:left-0 before:w-px before:origin-top before:scale-y-0 before:bg-strong before:transition-transform before:duration-300 before:ease-apple hover:bg-wash hover:before:scale-y-100 focus-visible:before:scale-y-100 sm:gap-10"
              >
                <div className="flex min-w-0 flex-1 flex-col gap-2">
                  <p className="truncate text-eyebrow text-muted-foreground uppercase">
                    {vehicle.plateNumber}
                  </p>
                  <p className="truncate text-section text-strong">
                    {vehicle.manufacturer} {vehicle.modelName}
                  </p>
                  <p className="text-[0.8125rem] text-muted-foreground">
                    {vehicle.modelYear === null ? '연식 미상' : `${vehicle.modelYear}년식`}
                  </p>
                </div>

                {/* 수치는 오른쪽 끝에 고정한다. tabular-nums 로 자릿수가 달라도 줄이 안 떨린다. */}
                <div className="flex shrink-0 flex-col items-end gap-2">
                  <p className="text-eyebrow text-faint uppercase">Odometer</p>
                  <p className="text-figure tabular-nums text-strong">
                    {formatNumber(vehicle.odometer)}
                    <span className="ml-1.5 text-[0.8125rem] tracking-normal text-muted-foreground">
                      km
                    </span>
                  </p>
                </div>

                {/* 2px만 움직인다. 화살표가 크게 미끄러지면 장난스러워진다. */}
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

/**
 * 로딩 중에도 실제 목록과 같은 격자·높이의 덩어리를 깔아 둔다.
 * "불러오는 중…" 한 줄만 보여주면 데이터가 도착하는 순간 화면이 통째로 튀어 오른다.
 */
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

/**
 * 빈 상태. 여백을 아주 크게 잡는다. 할 일이 하나뿐인 화면에서는
 * 공백 자체가 "여기를 누르라"는 안내가 된다.
 */
function EmptyGarage() {
  return (
    <div className="flex flex-col items-center gap-8 border-y border-border px-8 py-32 text-center">
      <GaugeMark className="size-10 text-muted-foreground" />

      <div className="flex max-w-sm flex-col gap-2">
        <p className="text-section text-strong">아직 등록된 차량이 없습니다</p>
        <p className="text-sm leading-relaxed text-muted-foreground">
          차량을 등록하면 정비 이력과 다음 정비 시점을 함께 관리할 수 있습니다.
        </p>
      </div>

      <Button render={<Link to="/vehicles/new" />}>첫 차량 등록하기</Button>
    </div>
  )
}
