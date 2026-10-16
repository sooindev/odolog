import { useState } from 'react'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { MaintenanceSection } from '@/features/maintenance/components/MaintenanceSection'
import { ALL_MAINTENANCE } from '@/features/maintenance/components/maintenanceView'
import type { MaintenanceRecordResponse } from '@/features/maintenance/api/types'
import { renderWithProviders } from '@/shared/lib/renderWithProviders'
import { queryKeys } from '@/shared/api/queryKeys'

vi.mock('@/features/maintenance/api/endpoints', () => ({
  fetchRecords: vi.fn(),
  deleteRecord: vi.fn(),
  registerRecord: vi.fn(),
  updateRecord: vi.fn(),
}))

const { fetchRecords, deleteRecord } = await import('@/features/maintenance/api/endpoints')

function record(id: string, description: string, serviceDate: string): MaintenanceRecordResponse {
  return {
    id,
    type: 'ENGINE_OIL',
    description,
    cost: 50000,
    currency: 'KRW',
    serviceOdometer: 12000,
    serviceDate,
  }
}

function Harness() {
  const [view, setView] = useState(ALL_MAINTENANCE)
  return (
    <MaintenanceSection
      vehicleId="vehicle00001"
      currentOdometer={20000}
      view={view}
      onViewChange={setView}
      onChanged={() => {}}
    />
  )
}

describe('MaintenanceSection', () => {
  // vitest globals 가 꺼져 있어 자동 정리가 없음
  afterEach(cleanup)

  beforeEach(() => {
    // 날짜 칸이 matchMedia 로 휠·네이티브를 고름. jsdom 에 없음
    vi.stubGlobal('matchMedia', () => ({
      matches: false,
      addEventListener: () => {},
      removeEventListener: () => {},
    }))
    vi.mocked(fetchRecords).mockReset().mockResolvedValue({
      items: [record('recordAAAAAA', '첫 번째 메모', '2026-09-01'), record('recordBBBBBB', '두 번째 메모', '2026-08-01')],
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
      hasNext: false,
    })
  })

  it('폼을 연 채 다른 행의 수정을 누르면 그 행의 값으로 바뀐다', async () => {
    renderWithProviders(<Harness />)

    const rows = await screen.findAllByRole('listitem')
    fireEvent.click(within(rows[0]).getByRole('button', { name: /수정$/ }))
    expect(screen.getByLabelText<HTMLTextAreaElement>(/메모/).value).toBe('첫 번째 메모')

    fireEvent.change(screen.getByLabelText(/메모/), { target: { value: '고치던 중' } })
    fireEvent.click(within(rows[1]).getByRole('button', { name: /수정$/ }))

    // 이전 입력이 남으면 다른 기록을 덮어씀
    expect(screen.getByLabelText<HTMLTextAreaElement>(/메모/).value).toBe('두 번째 메모')
  })

  it('행마다 같은 수정·삭제 버튼도 화면 낭독기가 어느 기록인지 구분할 수 있다', async () => {
    renderWithProviders(<Harness />)

    await screen.findAllByRole('listitem')
    expect(screen.getByRole('button', { name: '엔진오일 2026. 9. 1. 수정' })).toBeTruthy()
    expect(screen.getByRole('button', { name: '엔진오일 2026. 8. 1. 삭제' })).toBeTruthy()
  })

  it('다른 행을 지워도 작성 중이던 새 기록 폼은 입력 그대로 남는다', async () => {
    vi.stubGlobal('confirm', () => true)
    vi.mocked(deleteRecord).mockResolvedValue(undefined)
    renderWithProviders(<Harness />)

    const rows = await screen.findAllByRole('listitem')
    fireEvent.click(screen.getByRole('button', { name: '이력 추가' }))
    fireEvent.change(screen.getByLabelText(/메모/), { target: { value: '적던 중' } })
    fireEvent.click(within(rows[1]).getByRole('button', { name: /삭제$/ }))

    await waitFor(() => expect(deleteRecord).toHaveBeenCalledWith('vehicle00001', 'recordBBBBBB'))
    // 삭제 뒤 재조회까지 끝나도 폼과 입력이 남음
    await waitFor(() => expect(fetchRecords).toHaveBeenCalledTimes(2))
    expect(screen.getByLabelText<HTMLTextAreaElement>(/메모/).value).toBe('적던 중')
  })

  it('수정 중인 행을 지우면 그 폼은 닫힌다', async () => {
    vi.stubGlobal('confirm', () => true)
    vi.mocked(deleteRecord).mockResolvedValue(undefined)
    renderWithProviders(<Harness />)

    const rows = await screen.findAllByRole('listitem')
    fireEvent.click(within(rows[0]).getByRole('button', { name: /수정$/ }))
    fireEvent.click(within(rows[0]).getByRole('button', { name: /삭제$/ }))

    // 없어진 기록을 고치는 폼이 남으면 저장 때 404
    await waitFor(() => expect(screen.queryByLabelText(/메모/)).toBeNull())
  })

  it('다른 카드가 기록을 만들어 목록을 다시 읽어도 열어 둔 수정 폼은 그대로다', async () => {
    const { queryClient } = renderWithProviders(<Harness />)

    const rows = await screen.findAllByRole('listitem')
    fireEvent.click(within(rows[1]).getByRole('button', { name: /수정$/ }))
    fireEvent.change(screen.getByLabelText(/메모/), { target: { value: '고치던 중' } })

    // 빠른 정비 저장과 같은 무효화
    await queryClient.invalidateQueries({ queryKey: queryKeys.maintenance('vehicle00001') })

    expect(fetchRecords).toHaveBeenCalledTimes(2)
    expect(screen.getByLabelText<HTMLTextAreaElement>(/메모/).value).toBe('고치던 중')
  })
})
