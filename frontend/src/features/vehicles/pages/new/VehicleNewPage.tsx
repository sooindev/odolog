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
    // back: 목록에서 파고 들어온 화면이므로 되돌아갈 길을 머리에 둔다.
    // 폼 아래 "취소" 와 역할이 다르다. 취소는 입력을 버리는 것이고 이건 그냥 이동이다.
    <Page back={{ to: '/vehicles', label: '내 차량' }} eyebrow="Garage" title="차량 등록">
      {/* 넓은 화면에서 폼만 가운데 좁게 두면 양옆이 비어 허전하다.
          설명을 왼쪽 열로 빼면 남는 폭이 여백이 아니라 정보로 채워진다. */}
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
