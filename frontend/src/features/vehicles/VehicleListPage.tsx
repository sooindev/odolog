import { useCallback, useState } from 'react'
import { Link } from 'react-router'

import { Button } from '@/shared/ui/button'
import { Card, CardContent } from '@/shared/ui/card'
import { formatKm } from '@/shared/lib/format'
import { useAsyncData } from '@/shared/lib/useAsyncData'
import { fetchVehicles } from '@/features/vehicles/api'

export function VehicleListPage() {
  const [page, setPage] = useState(0)

  // page가 바뀔 때마다 새 함수가 만들어지고, 그걸 본 useAsyncData가 다시 불러온다.
  const load = useCallback(() => fetchVehicles(page), [page])
  const { data, loading, error } = useAsyncData(load, '차량 목록을 불러오지 못했습니다.')

  if (loading) {
    return <p className="text-muted-foreground text-sm">불러오는 중…</p>
  }

  if (error !== null) {
    return <p className="text-destructive text-sm">{error}</p>
  }

  if (data === null || data.totalElements === 0) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center gap-3 py-10 text-center">
          <p className="text-muted-foreground text-sm">아직 등록된 차량이 없습니다.</p>
          {/* render: 버튼 스타일을 <Link>에 입힌다. <button> 안에 <a>를 넣으면 잘못된 HTML이 된다. */}
          <Button render={<Link to="/vehicles/new" />}>첫 차량 등록하기</Button>
        </CardContent>
      </Card>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold">내 차량 {data.totalElements}대</h1>
        <Button size="sm" render={<Link to="/vehicles/new" />}>
          차량 등록
        </Button>
      </div>

      <ul className="flex flex-col gap-2">
        {data.items.map((vehicle) => (
          <li key={vehicle.id}>
            <Link to={`/vehicles/${vehicle.id}`}>
              <Card className="hover:bg-accent/50 transition-colors">
                <CardContent className="flex items-center justify-between">
                  <div>
                    <p className="font-medium">
                      {vehicle.manufacturer} {vehicle.modelName}
                    </p>
                    <p className="text-muted-foreground text-sm">
                      {vehicle.plateNumber} · {vehicle.modelYear}년식
                    </p>
                  </div>
                  <p className="text-sm">{formatKm(vehicle.odometer)}</p>
                </CardContent>
              </Card>
            </Link>
          </li>
        ))}
      </ul>

      {/* 페이지가 1장뿐이면 이동 UI 자체를 보여주지 않는다. */}
      {data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-3">
          <Button
            variant="outline"
            size="sm"
            disabled={data.page === 0}
            onClick={() => setPage((current) => current - 1)}
          >
            이전
          </Button>
          <span className="text-muted-foreground text-sm">
            {data.page + 1} / {data.totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={!data.hasNext}
            onClick={() => setPage((current) => current + 1)}
          >
            다음
          </Button>
        </div>
      )}
    </div>
  )
}
