import { cleanup, fireEvent, screen } from '@testing-library/react'
import { useState } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { FuelForm } from '@/features/fuel/components/FuelForm'
import type { FuelRecordResponse } from '@/features/fuel/api/types'
import { renderWithProviders } from '@/shared/lib/renderWithProviders'

vi.mock('@/features/fuel/api/endpoints', () => ({
  registerFuelRecord: vi.fn(),
  updateFuelRecord: vi.fn(),
}))

const { registerFuelRecord, updateFuelRecord } = await import('@/features/fuel/api/endpoints')

const RECORD: FuelRecordResponse = {
  id: 'fuelAAAAAAAA',
  fueledAt: '2026-09-01',
  odometer: 15000,
  liters: 32.45,
  totalCost: 55000,
  currency: 'KRW',
  memo: '처음 메모',
  resetPoint: false,
  fullTank: true,
  litersUse: 'CLOSING',
  efficiencyGap: null,
  pricePerLiter: 1695,
  distance: 500,
  efficiency: 15.4,
  efficiencySuspicious: false,
  missingRecordSuspected: false,
  dateBeforePrevious: false,
}

/** 실제로 실린 필드만. undefined 는 JSON 에서 빠짐 */
function sentFields() {
  const request = vi.mocked(updateFuelRecord).mock.calls[0][2]
  return Object.fromEntries(Object.entries(request).filter(([, value]) => value !== undefined))
}

function renderEdit() {
  const onSaved = vi.fn()
  renderWithProviders(
    <FuelForm vehicleId="vehicle00001" record={RECORD} defaultOdometer={20000} onSaved={onSaved} onCancel={() => {}} />,
  )
  return onSaved
}

describe('FuelForm 수정', () => {
  // vitest globals 가 꺼져 있어 자동 정리가 없음
  afterEach(cleanup)

  beforeEach(() => {
    vi.mocked(updateFuelRecord).mockReset().mockResolvedValue(RECORD)
    vi.stubGlobal('matchMedia', () => ({
      matches: false,
      addEventListener: () => {},
      removeEventListener: () => {},
    }))
    vi.stubGlobal('confirm', vi.fn(() => true))
  })

  it('메모만 고치면 메모만 보낸다. 주유량·금액이 지워지지 않는다', async () => {
    const onSaved = renderEdit()

    fireEvent.change(screen.getByLabelText(/메모/), { target: { value: '고친 메모' } })
    fireEvent.click(screen.getByRole('button', { name: '저장' }))

    await vi.waitFor(() => expect(onSaved).toHaveBeenCalled())
    expect(sentFields()).toEqual({ memo: '고친 메모' })
    expect(window.confirm).not.toHaveBeenCalled()
  })

  it('주유량을 비우면 clearLiters 만 실린다', async () => {
    const onSaved = renderEdit()

    fireEvent.change(screen.getByLabelText(/주유량/), { target: { value: '' } })
    fireEvent.click(screen.getByRole('button', { name: '저장' }))

    await vi.waitFor(() => expect(onSaved).toHaveBeenCalled())
    expect(sentFields()).toEqual({ clearLiters: true })
    // 비운 칸은 저장 전에 한 번 확인
    expect(window.confirm).toHaveBeenCalledTimes(1)
  })

  it('금액이 비어 있던 기록은 지금 통화로 받는다 — 옛 통화(USD)의 소수 자리로 바꾸지 않음', async () => {
    const onSaved = vi.fn()
    renderWithProviders(
      <FuelForm
        vehicleId="vehicle00001"
        record={{ ...RECORD, totalCost: null, pricePerLiter: null, currency: 'USD' }}
        defaultOdometer={20000}
        onSaved={onSaved}
        onCancel={() => {}}
      />,
    )

    // 사용자 통화는 원화. 라벨도 원화
    fireEvent.change(screen.getByLabelText(/KRW/), { target: { value: '50000' } })
    fireEvent.click(screen.getByRole('button', { name: '저장' }))

    await vi.waitFor(() => expect(onSaved).toHaveBeenCalled())
    // 달러로 바꾸면 5,000,000(센트)이 됨
    expect(sentFields()).toEqual({ totalCost: 50000 })
  })
})

/** 폼이 열린 채 차량 주행거리가 바뀌는 상황. 다른 카드에서 주유·갱신을 저장한 경우 */
function OdometerChangesWhileOpen({ onSaved }: { onSaved: () => void }) {
  const [vehicleOdometer, setVehicleOdometer] = useState(20000)
  return (
    <>
      <button type="button" onClick={() => setVehicleOdometer(20500)}>
        차량 주행거리 갱신
      </button>
      <FuelForm
        vehicleId="vehicle00001"
        record={null}
        defaultOdometer={vehicleOdometer}
        onSaved={onSaved}
        onCancel={() => {}}
      />
    </>
  )
}

describe('FuelForm 등록', () => {
  afterEach(cleanup)

  beforeEach(() => {
    vi.mocked(registerFuelRecord).mockReset().mockResolvedValue(RECORD)
    vi.stubGlobal('matchMedia', () => ({
      matches: false,
      addEventListener: () => {},
      removeEventListener: () => {},
    }))
    vi.stubGlobal('confirm', vi.fn(() => true))
  })

  it('폼을 연 뒤 차량 주행거리가 바뀌어도 손대지 않은 칸은 보이는 값 그대로 보낸다', async () => {
    const onSaved = vi.fn()
    renderWithProviders(<OdometerChangesWhileOpen onSaved={onSaved} />)

    fireEvent.click(screen.getByRole('button', { name: '차량 주행거리 갱신' }))
    expect(screen.getByLabelText(/주행거리/)).toHaveProperty('value', '20000')
    fireEvent.click(screen.getByRole('button', { name: '등록' }))

    await vi.waitFor(() => expect(onSaved).toHaveBeenCalled())
    expect(vi.mocked(registerFuelRecord).mock.calls[0][1].odometer).toBe(20000)
  })
})
