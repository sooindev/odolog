import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ApiError } from '@/lib/api'
import { registerVehicle } from '@/lib/vehicles'

export function VehicleNewPage() {
  const navigate = useNavigate()

  const [plateNumber, setPlateNumber] = useState('')
  const [manufacturer, setManufacturer] = useState('')
  const [modelName, setModelName] = useState('')
  // 숫자 입력도 상태는 문자열로 둔다. 입력 도중의 빈 문자열을 숫자로 표현할 방법이 없기 때문.
  const [modelYear, setModelYear] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setPending(true)

    try {
      const vehicle = await registerVehicle({
        plateNumber,
        manufacturer,
        modelName,
        // 보낼 때 숫자로 변환한다. 문자열 "2023"을 보내면 백엔드가 400을 준다.
        modelYear: Number(modelYear),
      })
      navigate(`/vehicles/${vehicle.id}`, { replace: true })
    } catch (caught) {
      // 409면 이미 등록된 번호판.
      setError(caught instanceof ApiError ? caught.message : '차량 등록에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    <Card className="mx-auto max-w-sm">
      <CardHeader>
        <CardTitle>차량 등록</CardTitle>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="plateNumber">차량 번호</Label>
            <Input
              id="plateNumber"
              required
              placeholder="12가3456"
              value={plateNumber}
              onChange={(event) => setPlateNumber(event.target.value)}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="manufacturer">제조사</Label>
            <Input
              id="manufacturer"
              required
              placeholder="현대"
              value={manufacturer}
              onChange={(event) => setManufacturer(event.target.value)}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="modelName">모델명</Label>
            <Input
              id="modelName"
              required
              placeholder="아반떼"
              value={modelName}
              onChange={(event) => setModelName(event.target.value)}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="modelYear">연식</Label>
            <Input
              id="modelYear"
              type="number"
              required
              min={1900}
              max={2100}
              placeholder="2023"
              value={modelYear}
              onChange={(event) => setModelYear(event.target.value)}
            />
          </div>

          {error !== null && <p className="text-destructive text-sm">{error}</p>}

          <div className="flex gap-2">
            <Button type="submit" disabled={pending} className="flex-1">
              {pending ? '등록 중…' : '등록'}
            </Button>
            <Button type="button" variant="outline" onClick={() => navigate(-1)}>
              취소
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}
