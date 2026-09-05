import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router'

import { MaintenanceSection } from '@/features/maintenance/MaintenanceSection'
import { NextServiceCard } from '@/features/maintenance/NextServiceCard'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { ApiError } from '@/shared/api/client'
import { formatKm } from '@/shared/lib/format'
import { deleteVehicle, fetchVehicle, updateOdometer } from '@/features/vehicles/api'
import type { VehicleResponse } from '@/shared/api/types'

export function VehicleDetailPage() {
  // URL의 :vehicleId 는 항상 문자열로 들어온다.
  const { vehicleId } = useParams<{ vehicleId: string }>()
  const navigate = useNavigate()

  const [vehicle, setVehicle] = useState<VehicleResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  // 정비 이력이 바뀌면 이 값을 올려 "다음 정비 시점"을 다시 계산하게 한다.
  const [maintenanceVersion, setMaintenanceVersion] = useState(0)

  const id = Number(vehicleId)

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        const result = await fetchVehicle(id)
        if (cancelled) return
        setVehicle(result)
        setError(null)
      } catch (caught) {
        if (cancelled) return
        // 404(없음)와 403(남의 차)을 구분해 보여주지 않는다.
        // 남의 차량이 "존재한다"는 사실 자체를 알리지 않기 위해서다.
        setError(caught instanceof ApiError ? caught.message : '차량을 불러오지 못했습니다.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    load()

    return () => {
      cancelled = true
    }
  }, [id])

  if (loading) {
    return <p className="text-muted-foreground text-sm">불러오는 중…</p>
  }

  if (error !== null || vehicle === null) {
    return <p className="text-destructive text-sm">{error ?? '차량을 찾을 수 없습니다.'}</p>
  }

  async function handleDelete() {
    // 되돌릴 수 없는 동작이라 정비 이력까지 사라진다는 걸 명시한다.
    if (!window.confirm('이 차량과 정비 이력이 모두 삭제됩니다. 계속할까요?')) {
      return
    }

    try {
      await deleteVehicle(id)
      navigate('/vehicles', { replace: true })
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : '삭제에 실패했습니다.')
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardHeader>
          <CardTitle>
            {vehicle.manufacturer} {vehicle.modelName}
          </CardTitle>
        </CardHeader>
        <CardContent className="text-sm">
          <dl className="grid grid-cols-[6rem_1fr] gap-y-2">
            <dt className="text-muted-foreground">차량 번호</dt>
            <dd>{vehicle.plateNumber}</dd>
            <dt className="text-muted-foreground">연식</dt>
            <dd>{vehicle.modelYear}년식</dd>
            <dt className="text-muted-foreground">주행거리</dt>
            <dd>{formatKm(vehicle.odometer)}</dd>
          </dl>
        </CardContent>
      </Card>

      <OdometerForm vehicle={vehicle} onUpdated={setVehicle} />

      <NextServiceCard vehicleId={vehicle.id} reloadKey={maintenanceVersion} />

      <MaintenanceSection
        vehicleId={vehicle.id}
        currentOdometer={vehicle.odometer}
        onChanged={() => setMaintenanceVersion((current) => current + 1)}
      />

      <div className="flex justify-end">
        <Button variant="destructive" onClick={handleDelete}>
          차량 삭제
        </Button>
      </div>
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
    setPending(true)

    try {
      onUpdated(await updateOdometer(vehicle.id, { odometer: Number(odometer) }))
    } catch (caught) {
      // 백엔드는 주행거리가 줄면 409를 준다. 현재 값을 같이 보여줘야 뭘 잘못했는지 안다.
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
    <Card>
      <CardHeader>
        <CardTitle className="text-base">주행거리 갱신</CardTitle>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-3" onSubmit={handleSubmit}>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="odometer">현재 주행거리 (km)</Label>
            <Input
              id="odometer"
              type="number"
              required
              min={0}
              value={odometer}
              onChange={(event) => setOdometer(event.target.value)}
            />
          </div>

          {error !== null && <p className="text-destructive text-sm">{error}</p>}

          <Button type="submit" disabled={pending} className="self-start">
            {pending ? '저장 중…' : '갱신'}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
