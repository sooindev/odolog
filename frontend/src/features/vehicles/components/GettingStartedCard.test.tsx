import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { GettingStartedCard } from '@/features/vehicles/components/GettingStartedCard'
import type { VehicleResponse } from '@/features/vehicles/api/types'
import { queryKeys } from '@/shared/api/queryKeys'
import { renderWithProviders } from '@/shared/lib/renderWithProviders'

vi.mock('@/features/maintenance/api/endpoints', () => ({
  fetchRecords: vi.fn(),
  registerRecord: vi.fn(),
}))
vi.mock('@/features/fuel/api/endpoints', () => ({
  fetchFuelRecords: vi.fn(),
}))

const { fetchRecords, registerRecord } = await import('@/features/maintenance/api/endpoints')
const { fetchFuelRecords } = await import('@/features/fuel/api/endpoints')

const vehicle: VehicleResponse = {
  id: 'vehicle00001',
  plateNumber: '12가3456',
  manufacturer: '기아',
  modelName: '카니발',
  modelYear: 2020,
  odometer: 30000,
  overdueServiceCount: null,
  dueSoonServiceCount: null,
}

function page(totalElements: number) {
  return { items: [], page: 0, size: 1, totalElements, totalPages: totalElements, hasNext: false }
}

describe('GettingStartedCard', () => {
  afterEach(cleanup)

  beforeEach(() => {
    vi.mocked(fetchRecords).mockReset().mockResolvedValue(page(0))
    vi.mocked(fetchFuelRecords).mockReset().mockResolvedValue(page(0))
    vi.mocked(registerRecord).mockReset()
  })

  it('빠른 정비가 일부 저장된 뒤 건수를 다시 읽어도 폼과 실패 문구가 남는다', async () => {
    // 엔진오일은 저장, 타이어는 실패
    vi.mocked(registerRecord)
      .mockResolvedValueOnce(undefined as never)
      .mockRejectedValueOnce(new Error('network'))
    const { queryClient } = renderWithProviders(<GettingStartedCard vehicle={vehicle} onServicesSaved={() => {}} />)

    fireEvent.click(await screen.findByRole('button', { name: '적기' }))
    fireEvent.click(within(screen.getByRole('group', { name: '엔진오일' })).getByRole('button', { name: '3개월 전' }))
    fireEvent.click(within(screen.getByRole('group', { name: '타이어' })).getByRole('button', { name: '3개월 전' }))
    fireEvent.click(screen.getByRole('button', { name: '저장' }))
    await screen.findByText(/1건은 저장했고/)

    // 창으로 돌아오는 등 재조회. 이제 정비 1건이라 그 단계는 끝난 것으로 바뀜
    vi.mocked(fetchRecords).mockResolvedValue(page(1))
    await queryClient.invalidateQueries({ queryKey: queryKeys.gettingStarted(vehicle.id) })
    await waitFor(() => expect(screen.getByText('4단계 중 2단계 완료')).toBeTruthy())

    expect(screen.getByText(/1건은 저장했고/)).toBeTruthy()
    expect(screen.getByRole('group', { name: '타이어' })).toBeTruthy()
  })

  it('다시 읽힌 뒤에도 남은 폼의 닫기로 부모에게 알려 정비 목록이 새 기록을 읽게 한다', async () => {
    vi.mocked(registerRecord)
      .mockResolvedValueOnce(undefined as never)
      .mockRejectedValueOnce(new Error('network'))
    const onServicesSaved = vi.fn()
    const { queryClient } = renderWithProviders(
      <GettingStartedCard vehicle={vehicle} onServicesSaved={onServicesSaved} />,
    )

    fireEvent.click(await screen.findByRole('button', { name: '적기' }))
    fireEvent.click(within(screen.getByRole('group', { name: '엔진오일' })).getByRole('button', { name: '3개월 전' }))
    fireEvent.click(within(screen.getByRole('group', { name: '타이어' })).getByRole('button', { name: '3개월 전' }))
    fireEvent.click(screen.getByRole('button', { name: '저장' }))
    await screen.findByText(/1건은 저장했고/)

    vi.mocked(fetchRecords).mockResolvedValue(page(1))
    await queryClient.invalidateQueries({ queryKey: queryKeys.gettingStarted(vehicle.id) })
    await waitFor(() => expect(screen.getByText('4단계 중 2단계 완료')).toBeTruthy())

    fireEvent.click(screen.getByRole('button', { name: '취소' }))
    expect(onServicesSaved).toHaveBeenCalledTimes(1)
  })
})
