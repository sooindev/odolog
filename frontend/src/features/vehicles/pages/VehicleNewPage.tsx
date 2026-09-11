import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router'

import { Button } from '@/shared/ui/button'
import { Field } from '@/shared/ui/field'
import { Input } from '@/shared/ui/input'
import { PageHeader } from '@/shared/ui/page-header'
import { ErrorText } from '@/shared/ui/state'
import { ApiError } from '@/shared/api/client'
import { registerVehicle } from '@/features/vehicles/api/endpoints'

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
    <div className="mx-auto flex max-w-[24rem] flex-col gap-10">
      <PageHeader eyebrow="Garage" title="차량 등록" />

      <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
        <Field label="차량 번호" htmlFor="plateNumber">
          <Input
            id="plateNumber"
            required
            placeholder="12가3456"
            value={plateNumber}
            onChange={(event) => setPlateNumber(event.target.value)}
          />
        </Field>

        {/* 제조사와 모델명은 함께 읽히는 한 쌍이라 좁은 화면에서만 위아래로 쌓는다. */}
        <div className="grid gap-5 sm:grid-cols-2">
          <Field label="제조사" htmlFor="manufacturer">
            <Input
              id="manufacturer"
              required
              placeholder="현대"
              value={manufacturer}
              onChange={(event) => setManufacturer(event.target.value)}
            />
          </Field>

          <Field label="모델명" htmlFor="modelName">
            <Input
              id="modelName"
              required
              placeholder="아반떼"
              value={modelName}
              onChange={(event) => setModelName(event.target.value)}
            />
          </Field>
        </div>

        <Field label="연식" htmlFor="modelYear">
          <Input
            id="modelYear"
            type="number"
            required
            min={1900}
            max={2100}
            placeholder="2023"
            // tabular-nums: 숫자 폭을 고정해 입력 중에 글자가 흔들리지 않게 한다.
            className="tabular-nums"
            value={modelYear}
            onChange={(event) => setModelYear(event.target.value)}
          />
        </Field>

        {error !== null && <ErrorText message={error} />}

        {/* 주 동작(등록)만 흰 버튼이다. 취소까지 채워 넣으면 둘 중 무엇이 기본인지 사라진다. */}
        <div className="mt-1 flex gap-2">
          <Button type="submit" size="lg" className="flex-1" disabled={pending}>
            {pending ? '등록 중…' : '등록'}
          </Button>
          <Button type="button" size="lg" variant="ghost" onClick={() => navigate(-1)}>
            취소
          </Button>
        </div>
      </form>
    </div>
  )
}
