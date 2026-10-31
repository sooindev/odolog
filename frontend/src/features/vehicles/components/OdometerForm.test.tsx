import { cleanup, fireEvent, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { OdometerForm } from '@/features/vehicles/components/OdometerForm'
import type { VehicleResponse } from '@/features/vehicles/api/types'
import { renderWithProviders } from '@/shared/lib/renderWithProviders'

vi.mock('@/features/vehicles/api/endpoints', () => ({
  updateOdometer: vi.fn(),
}))

const { updateOdometer } = await import('@/features/vehicles/api/endpoints')

const vehicle: VehicleResponse = {
  id: 'vehicle00001',
  plateNumber: '12가3456',
  manufacturer: '기아',
  modelName: '카니발',
  modelYear: 2020,
  odometer: 45000,
  overdueServiceCount: null,
  dueSoonServiceCount: null,
}

describe('OdometerForm — 앞서 뜬 409 안내', () => {
  afterEach(cleanup)

  beforeEach(() => {
    vi.mocked(updateOdometer).mockReset().mockResolvedValue(vehicle)
  })

  function submit(value: string, onConflict: (message: string | null) => void) {
    renderWithProviders(
      <OdometerForm
        vehicle={vehicle}
        onUpdated={() => {}}
        conflict={{ message: '다른 곳에서 먼저 바뀌었습니다.', staleOdometer: 45000 }}
        onConflict={onConflict}
      />,
    )
    fireEvent.change(screen.getByLabelText(/주행거리/), { target: { value } })
    fireEvent.submit(screen.getByLabelText(/주행거리/).closest('form')!)
  }

  it('확인 창에서 취소하면 아무것도 안 보냈으니 남는다', () => {
    vi.stubGlobal('confirm', () => false)
    const onConflict = vi.fn()

    // 감소 → 확인 창
    submit('40000', onConflict)

    expect(updateOdometer).not.toHaveBeenCalled()
    expect(onConflict).not.toHaveBeenCalled()
  })

  it('확인하고 보내면 지운다', () => {
    vi.stubGlobal('confirm', () => true)
    const onConflict = vi.fn()

    submit('40000', onConflict)

    expect(onConflict).toHaveBeenCalledWith(null)
    expect(updateOdometer).toHaveBeenCalledWith('vehicle00001', { odometer: 40000, force: true })
  })
})
