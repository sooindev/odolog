import { cleanup, fireEvent, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { FuelSection } from '@/features/fuel/components/FuelSection'
import type { FuelRecordResponse } from '@/features/fuel/api/types'
import { renderWithProviders } from '@/shared/lib/renderWithProviders'

vi.mock('@/features/fuel/api/endpoints', () => ({
  fetchFuelRecords: vi.fn(),
  deleteFuelRecord: vi.fn(),
  registerFuelRecord: vi.fn(),
  updateFuelRecord: vi.fn(),
}))

const { fetchFuelRecords } = await import('@/features/fuel/api/endpoints')

function record(id: string, odometer: number, memo: string): FuelRecordResponse {
  return {
    id,
    fueledAt: '2026-09-01',
    odometer,
    liters: 30,
    totalCost: 50000,
    currency: 'KRW',
    memo,
    resetPoint: false,
    pricePerLiter: 1667,
    distance: 500,
    efficiency: 16.67,
    efficiencySuspicious: false,
    missingRecordSuspected: false,
  }
}

describe('FuelSection', () => {
  // vitest globals 가 꺼져 있어 자동 정리가 없음
  afterEach(cleanup)

  beforeEach(() => {
    // 날짜 칸이 matchMedia 로 휠·네이티브를 고름. jsdom 에 없음
    vi.stubGlobal('matchMedia', () => ({
      matches: false,
      addEventListener: () => {},
      removeEventListener: () => {},
    }))
    vi.mocked(fetchFuelRecords).mockReset().mockResolvedValue({
      items: [record('fuelAAAAAAAA', 46000, '첫 번째 메모'), record('fuelBBBBBBBB', 45500, '두 번째 메모')],
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
      hasNext: false,
    })
  })

  it('폼을 연 채 다른 행의 수정을 누르면 그 행의 값으로 바뀐다', async () => {
    renderWithProviders(<FuelSection vehicleId="vehicle00001" currentOdometer={46000} onChanged={() => {}} />)

    const rows = await screen.findAllByRole('listitem')
    fireEvent.click(within(rows[0]).getByRole('button', { name: '수정' }))
    expect(screen.getByLabelText<HTMLTextAreaElement>(/메모/).value).toBe('첫 번째 메모')

    fireEvent.change(screen.getByLabelText(/메모/), { target: { value: '고치던 중' } })
    fireEvent.click(within(rows[1]).getByRole('button', { name: '수정' }))

    // 이전 입력이 남으면 다른 기록을 덮어씀
    expect(screen.getByLabelText<HTMLTextAreaElement>(/메모/).value).toBe('두 번째 메모')
  })
})
