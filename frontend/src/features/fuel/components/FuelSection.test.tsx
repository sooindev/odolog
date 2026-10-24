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
    fullTank: true,
    litersUse: 'CLOSING',
    pricePerLiter: 1667,
    distance: 500,
    efficiency: 16.67,
    efficiencySuspicious: false,
    missingRecordSuspected: false,
    dateBeforePrevious: false,
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
    fireEvent.click(within(rows[0]).getByRole('button', { name: /수정$/ }))
    expect(screen.getByLabelText<HTMLTextAreaElement>(/메모/).value).toBe('첫 번째 메모')

    fireEvent.change(screen.getByLabelText(/메모/), { target: { value: '고치던 중' } })
    fireEvent.click(within(rows[1]).getByRole('button', { name: /수정$/ }))

    // 이전 입력이 남으면 다른 기록을 덮어씀
    expect(screen.getByLabelText<HTMLTextAreaElement>(/메모/).value).toBe('두 번째 메모')
  })

  it('덜 채운 주유 행은 서버가 알린 구간대로 안내한다', async () => {
    const partial = { fullTank: false, distance: null, efficiency: null }
    vi.mocked(fetchFuelRecords).mockResolvedValue({
      items: [
        { ...record('fuelAAAAAAAA', 46000, '다음에 합산'), ...partial, litersUse: 'NEXT' },
        // 출발 주행거리에서 더 넣은 양. 어느 구간에도 안 들어감
        { ...record('fuelBBBBBBBB', 45500, '출발 전'), ...partial, litersUse: 'NONE' },
      ],
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
      hasNext: false,
    })
    renderWithProviders(<FuelSection vehicleId="vehicle00001" currentOdometer={46000} onChanged={() => {}} />)

    const rows = await screen.findAllByRole('listitem')
    expect(rows[0].textContent).toContain('다음 가득 주유의 연비에 합산')
    expect(rows[1].textContent).not.toContain('다음 가득 주유의 연비에 합산')
    expect(rows[1].textContent).toContain('연비 계산 안 됨')
  })

  it('덜 채운 주유가 연비 기준점이면 그 행에 기준점이라고 밝힌다', async () => {
    vi.mocked(fetchFuelRecords).mockResolvedValue({
      items: [
        record('fuelAAAAAAAA', 46000, '가득'),
        {
          ...record('fuelBBBBBBBB', 45500, '초기화한 주유'),
          fullTank: false,
          resetPoint: true,
          distance: null,
          efficiency: null,
          litersUse: 'NONE',
        },
      ],
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
      hasNext: false,
    })
    renderWithProviders(<FuelSection vehicleId="vehicle00001" currentOdometer={46000} onChanged={() => {}} />)

    const rows = await screen.findAllByRole('listitem')
    // 덜 채움 문구만 보이면 어디서 초기화했는지 목록에서 사라짐
    expect(rows[1].textContent).toContain('연비 기준점')
    expect(rows[1].textContent).toContain('다음 가득 주유부터 계산')
  })
})
