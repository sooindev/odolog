import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router'

import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { Field } from '@/shared/ui/form/field'
import { Input } from '@/shared/ui/base/input'
import { FormActions, Page } from '@/shared/ui/layout/page'
import { Section } from '@/shared/ui/layout/section'
import { ErrorText } from '@/shared/ui/feedback/state'
import { ApiError } from '@/shared/api/client/client'
import { registerVehicle } from '@/features/vehicles/api/endpoints/endpoints'

export function VehicleNewPage() {
  const navigate = useNavigate()

  const [plateNumber, setPlateNumber] = useState('')
  const [manufacturer, setManufacturer] = useState('')
  const [modelName, setModelName] = useState('')
  // 입력 중 빈 값 표현을 위해 문자열 보관
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
        // 전송 직전 숫자 변환
        modelYear: Number(modelYear),
      })
      navigate(`/vehicles/${vehicle.id}`, { replace: true })
    } catch (caught) {
      // 409 = 이미 등록된 번호판
      setError(caught instanceof ApiError ? caught.message : '차량 등록에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    // back: 목록으로 이동. 입력을 버리는 취소와 별개
    <Page back={{ to: '/vehicles', label: '내 차량' }} eyebrow="Garage" title="차량 등록">
      {/* 왼쪽 설명 / 오른쪽 폼 2단 */}
      <Section
        title="차량 정보"
        description="번호판은 내 차량 안에서만 중복되지 않으면 됩니다. 다른 사람이 같은 번호판을 등록해 두었더라도 상관없습니다."
      >
        <Card>
          <CardContent>
            <form className="flex max-w-lg flex-col gap-5" onSubmit={handleSubmit}>
              <Field label="차량 번호" htmlFor="plateNumber">
                <Input
                  id="plateNumber"
                  required
                  placeholder="12가3456"
                  value={plateNumber}
                  onChange={(event) => setPlateNumber(event.target.value)}
                />
              </Field>

              {/* 제조사·모델명 한 줄. 좁은 화면만 위아래 */}
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
                  // tabular-nums: 입력 중 흔들림 방지
                  className="tabular-nums"
                  value={modelYear}
                  onChange={(event) => setModelYear(event.target.value)}
                />
              </Field>

              {error !== null && <ErrorText message={error} />}

              <FormActions>
                <Button type="submit" disabled={pending}>
                  {pending ? '등록 중…' : '등록'}
                </Button>
                <Button type="button" variant="ghost" onClick={() => navigate(-1)}>
                  취소
                </Button>
              </FormActions>
            </form>
          </CardContent>
        </Card>
      </Section>
    </Page>
  )
}
