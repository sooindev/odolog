import { cleanup, fireEvent, screen } from '@testing-library/react'
import { useState } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { MaintenanceForm } from '@/features/maintenance/components/MaintenanceForm'
import type { MaintenanceRecordResponse } from '@/features/maintenance/api/types'
import { renderWithProviders } from '@/shared/lib/renderWithProviders'

vi.mock('@/features/maintenance/api/endpoints', () => ({
  registerRecord: vi.fn(),
  updateRecord: vi.fn(),
}))

const { registerRecord, updateRecord } = await import('@/features/maintenance/api/endpoints')

const SAVED: MaintenanceRecordResponse = {
  id: 'mntAAAAAAAAA',
  type: 'ENGINE_OIL',
  description: null,
  cost: null,
  currency: 'KRW',
  serviceOdometer: 45000,
  serviceDate: '2026-09-01',
}

/** 폼이 열린 채 차량 주행거리가 바뀌는 상황. 다른 카드에서 주유·갱신을 저장한 경우 */
function OdometerChangesWhileOpen({ onSaved }: { onSaved: () => void }) {
  const [vehicleOdometer, setVehicleOdometer] = useState(45000)
  return (
    <>
      <button type="button" onClick={() => setVehicleOdometer(45500)}>
        차량 주행거리 갱신
      </button>
      <MaintenanceForm
        vehicleId="vehicle00001"
        record={null}
        defaultOdometer={vehicleOdometer}
        onSaved={onSaved}
        onCancel={() => {}}
      />
    </>
  )
}

describe('MaintenanceForm 등록', () => {
  // vitest globals 가 꺼져 있어 자동 정리가 없음
  afterEach(cleanup)

  beforeEach(() => {
    vi.mocked(registerRecord).mockReset().mockResolvedValue(SAVED)
    vi.stubGlobal('matchMedia', () => ({
      matches: false,
      addEventListener: () => {},
      removeEventListener: () => {},
    }))
  })

  it('폼을 연 뒤 차량 주행거리가 바뀌면 손대지 않은 칸은 새 값으로 따라가고 보이는 값 그대로 보낸다', async () => {
    const onSaved = vi.fn()
    renderWithProviders(<OdometerChangesWhileOpen onSaved={onSaved} />)

    fireEvent.click(screen.getByRole('button', { name: '차량 주행거리 갱신' }))
    expect(screen.getByLabelText(/주행거리/)).toHaveProperty('value', '45500')
    fireEvent.click(screen.getByRole('button', { name: '등록' }))

    await vi.waitFor(() => expect(onSaved).toHaveBeenCalled())
    expect(vi.mocked(registerRecord).mock.calls[0][1].serviceOdometer).toBe(45500)
  })

  it('직접 적은 주행거리는 그대로 두고, 바뀐 차량 값보다 작으면 과거 안내를 띄운다', () => {
    renderWithProviders(<OdometerChangesWhileOpen onSaved={() => {}} />)

    fireEvent.change(screen.getByLabelText(/주행거리/), { target: { value: '45200' } })
    expect(screen.queryByText(/보다 작습니다/)).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: '차량 주행거리 갱신' }))

    expect(screen.getByLabelText(/주행거리/)).toHaveProperty('value', '45200')
    expect(screen.getByText(/보다 작습니다/)).toBeTruthy()
  })
})

describe('MaintenanceForm 수정', () => {
  afterEach(cleanup)

  beforeEach(() => {
    vi.mocked(updateRecord).mockReset().mockResolvedValue(SAVED)
    vi.stubGlobal('matchMedia', () => ({
      matches: false,
      addEventListener: () => {},
      removeEventListener: () => {},
    }))
  })

  it('비용이 비어 있던 기록은 지금 통화로 받는다 — 옛 통화(USD)의 소수 자리로 바꾸지 않음', async () => {
    const onSaved = vi.fn()
    const { container } = renderWithProviders(
      <MaintenanceForm
        vehicleId="vehicle00001"
        record={{ ...SAVED, currency: 'USD' }}
        defaultOdometer={45000}
        onSaved={onSaved}
        onCancel={() => {}}
      />,
    )

    // 사용자 통화는 원화. 라벨도 원화
    expect(screen.getByLabelText(/KRW/)).toBeTruthy()
    fireEvent.change(screen.getByLabelText(/KRW/), { target: { value: '50000' } })
    fireEvent.submit(container.querySelector('form') as HTMLFormElement)

    await vi.waitFor(() => expect(onSaved).toHaveBeenCalled())
    // 달러로 바꾸면 5,000,000(센트)이 됨
    expect(vi.mocked(updateRecord).mock.calls[0][2].cost).toBe(50000)
  })
})
