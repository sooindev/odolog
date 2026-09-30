import { useCallback, useState } from 'react'
import { Check } from 'lucide-react'

import { QuickServiceForm } from '@/features/maintenance/components/quick/QuickServiceForm'
import { fetchRecords } from '@/features/maintenance/api/endpoints/endpoints'
import { fetchFuelRecords } from '@/features/fuel/api/endpoints/endpoints'
import type { VehicleResponse } from '@/features/vehicles/api/types/types'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { useI18n } from '@/shared/i18n/context/I18nContext'
import { useAsyncData } from '@/shared/lib/hooks/useAsyncData'

/** 닫은 안내를 기억하는 키. 기기별 편의라 서버에 두지 않음 */
const HIDDEN_KEY = 'odolog-getting-started-hidden:'

function readHidden(vehicleId: string) {
  try {
    return localStorage.getItem(HIDDEN_KEY + vehicleId) === '1'
  } catch {
    return false
  }
}

type StepKey = 'odometer' | 'services' | 'fuel' | 'efficiency'

/**
 * 타던 차를 등록한 뒤의 첫 단계들. 실제 입력이 곧 안내
 * 완료 여부는 저장된 기록으로 판정(따로 저장하지 않음). 다 끝나면 사라짐
 */
export function GettingStartedCard({
  vehicle,
  version,
  onServicesSaved,
}: {
  vehicle: VehicleResponse
  /** 정비·주유가 바뀔 때마다 증가. 재조회 신호 */
  version: number
  onServicesSaved: () => void
}) {
  const { t } = useI18n()
  const [hidden, setHidden] = useState(() => readHidden(vehicle.id))
  const [quickOpen, setQuickOpen] = useState(false)

  // 건수만 필요해 한 건씩. version 을 의존성에 넣어 재조회(재생성하면 카드가 깜빡임)
  const load = useCallback(
    () =>
      Promise.all([fetchRecords(vehicle.id, 0, null, 1), fetchFuelRecords(vehicle.id, 0, 1)]).then(
        ([records, fuels]) => ({ services: records.totalElements, fuels: fuels.totalElements }),
      ),
    // version 은 본문에서 안 쓰지만 재조회 신호라 의존성에 둠
    // oxlint-disable-next-line react-hooks/exhaustive-deps
    [vehicle.id, version],
  )
  const { data } = useAsyncData(load, '')

  // 불러오기 전·실패 시에는 그리지 않음. 안내가 틀린 상태로 보이는 것보다 안 보이는 편이 낫다
  if (hidden || data === null) {
    return null
  }

  const done: Record<StepKey, boolean> = {
    odometer: vehicle.odometer > 0,
    services: data.services > 0,
    fuel: data.fuels >= 1,
    efficiency: data.fuels >= 2,
  }
  const order: StepKey[] = ['odometer', 'services', 'fuel', 'efficiency']
  const doneCount = order.filter((key) => done[key]).length

  if (doneCount === order.length) {
    return null
  }

  // 지금 할 단계 하나만 설명을 펼침. 나머지는 제목만
  const current = order.find((key) => !done[key])

  function hide() {
    try {
      localStorage.setItem(HIDDEN_KEY + vehicle.id, '1')
    } catch {
      // 저장 실패해도 이번 화면에서는 닫힘
    }
    setHidden(true)
  }

  function goTo(elementId: string) {
    const element = document.getElementById(elementId)
    element?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    element?.focus({ preventScroll: true })
  }

  function action(key: StepKey) {
    const steps = t.vehicles.gettingStarted.steps
    switch (key) {
      case 'odometer':
        return (
          <Button size="sm" variant="secondary" onClick={() => goTo('odometer')}>
            {steps.odometer.action}
          </Button>
        )
      case 'services':
        return quickOpen ? null : (
          <Button size="sm" variant="secondary" onClick={() => setQuickOpen(true)}>
            {steps.services.action}
          </Button>
        )
      case 'fuel':
        return (
          <Button size="sm" variant="secondary" onClick={() => goTo('fuel-section')}>
            {steps.fuel.action}
          </Button>
        )
      default:
        // 연비는 기다리는 단계. 누를 것이 없음
        return null
    }
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-start justify-between gap-4">
        <div className="flex min-w-0 flex-col gap-1.5">
          <CardTitle>{t.vehicles.gettingStarted.title}</CardTitle>
          <CardDescription>{t.vehicles.gettingStarted.description}</CardDescription>
        </div>
        <Button size="sm" variant="ghost" className="shrink-0" onClick={hide}>
          {t.vehicles.gettingStarted.hide}
        </Button>
      </CardHeader>

      <CardContent className="flex flex-col gap-5">
        <p className="text-caption tabular-nums text-muted-foreground">
          {t.vehicles.gettingStarted.progress(doneCount, order.length)}
        </p>

        <ol className="divide-y divide-border">
          {order.map((key, index) => {
            const step = t.vehicles.gettingStarted.steps[key]
            const isCurrent = key === current

            return (
              <li key={key} className="flex gap-4 py-4 first:pt-0 last:pb-0">
                {/* 끝난 단계는 체크, 남은 단계는 번호. 색이 아니라 모양으로 구분 */}
                <span
                  aria-hidden="true"
                  className={`flex size-6 shrink-0 items-center justify-center border text-unit tabular-nums ${
                    done[key] ? 'border-border text-muted-foreground' : 'border-strong/30 text-strong'
                  }`}
                >
                  {done[key] ? <Check className="size-3.5" /> : index + 1}
                </span>

                <div className="flex min-w-0 flex-1 flex-col gap-2">
                  <p className={`text-body ${done[key] ? 'text-muted-foreground' : 'font-medium text-strong'}`}>
                    {step.title}
                    {done[key] && <span className="sr-only"> · {t.vehicles.gettingStarted.done}</span>}
                  </p>

                  {isCurrent && (
                    <>
                      <p className="text-caption leading-relaxed text-muted-foreground">{step.body}</p>
                      <div>{action(key)}</div>

                      {key === 'services' && quickOpen && (
                        // 펼침 연출. 닫을 때는 없음
                        <div className="form-open">
                          <div>
                            <QuickServiceForm
                              vehicleId={vehicle.id}
                              onSaved={() => {
                                setQuickOpen(false)
                                onServicesSaved()
                              }}
                              onCancel={() => setQuickOpen(false)}
                            />
                          </div>
                        </div>
                      )}
                    </>
                  )}
                </div>
              </li>
            )
          })}
        </ol>
      </CardContent>
    </Card>
  )
}
