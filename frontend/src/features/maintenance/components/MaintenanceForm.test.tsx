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

const { registerRecord } = await import('@/features/maintenance/api/endpoints')

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

  it('폼을 연 뒤 차량 주행거리가 바뀌어도 손대지 않은 칸은 보이는 값 그대로 보낸다', async () => {
    const onSaved = vi.fn()
    renderWithProviders(<OdometerChangesWhileOpen onSaved={onSaved} />)

    fireEvent.click(screen.getByRole('button', { name: '차량 주행거리 갱신' }))
    expect(screen.getByLabelText(/주행거리/)).toHaveProperty('value', '45000')
    fireEvent.click(screen.getByRole('button', { name: '등록' }))

    await vi.waitFor(() => expect(onSaved).toHaveBeenCalled())
    expect(vi.mocked(registerRecord).mock.calls[0][1].serviceOdometer).toBe(45000)
  })
})
