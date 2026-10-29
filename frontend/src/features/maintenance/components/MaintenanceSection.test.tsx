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

  it('새 기록을 적던 중 다른 행의 수정을 누르면 묻고, 취소하면 입력이 그대로 남는다', async () => {
    const confirm = vi.fn(() => false)
    vi.stubGlobal('confirm', confirm)
    renderWithProviders(<Harness />)

    const rows = await screen.findAllByRole('listitem')
    fireEvent.click(screen.getByRole('button', { name: '이력 추가' }))
    fireEvent.change(screen.getByLabelText(/메모/), { target: { value: '적던 중' } })
    fireEvent.click(within(rows[0]).getByRole('button', { name: /수정$/ }))

    expect(confirm).toHaveBeenCalledTimes(1)
    expect(screen.getByLabelText<HTMLTextAreaElement>(/메모/).value).toBe('적던 중')

    // 확인하면 그 행의 수정 폼으로
    confirm.mockReturnValue(true)
    fireEvent.click(within(rows[0]).getByRole('button', { name: /수정$/ }))
    expect(screen.getByLabelText<HTMLTextAreaElement>(/메모/).value).toBe('첫 번째 메모')
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

  /** 1쪽은 늘 성공, 2쪽은 처음 한 번 실패 */
  function failSecondPageOnce(firstItems: MaintenanceRecordResponse[]) {
    const firstPage = { items: firstItems, page: 0, size: 1, totalElements: 2, totalPages: 2, hasNext: true }
    let secondPageCalls = 0
    vi.mocked(fetchRecords)
      .mockReset()
      .mockImplementation(async (_vehicleId, page) => {
        if (page === 0) {
          return firstPage
        }
        secondPageCalls += 1
        if (secondPageCalls === 1) {
          throw new TypeError('Failed to fetch')
        }
        return { ...firstPage, items: [record('recordBBBBBB', '둘째 쪽 메모', '2026-08-01')], page: 1, hasNext: false }
      })
  }

  it('지운 뒤 재조회가 실패해도 지운 행은 화면에서 빠진다', async () => {
    vi.stubGlobal('confirm', () => true)
    vi.mocked(deleteRecord).mockResolvedValue(undefined)
    renderWithProviders(<Harness />)

    await screen.findByText('두 번째 메모')
    vi.mocked(fetchRecords).mockRejectedValue(new TypeError('blocked'))
    fireEvent.click(within(screen.getAllByRole('listitem')[1]).getByRole('button', { name: /삭제$/ }))

    await waitFor(() => expect(fetchRecords).toHaveBeenCalledTimes(2))
    // 남으면 지우지 못한 줄 알고 다시 누름
    await waitFor(() => expect(screen.queryByText('두 번째 메모')).toBeNull())
    expect(screen.getByText('첫 번째 메모')).toBeTruthy()
  })

  it('정비 종류 필터를 바꾸는 동안 앞 필터의 목록을 새 필터 이름 아래 보이지 않는다', async () => {
    renderWithProviders(<Harness />)
    await screen.findByText('첫 번째 메모')

    // 새 필터의 응답은 아직 오지 않음
    vi.mocked(fetchRecords).mockReturnValue(new Promise(() => {}))
    fireEvent.change(screen.getByLabelText('정비 종류로 거르기'), { target: { value: 'TIRE' } })

    await waitFor(() => expect(screen.queryByText('첫 번째 메모')).toBeNull())
  })

  it('다음 쪽 조회가 실패해도 받아 둔 쪽과 페이지 이동이 남고, 다시 누르면 그 쪽을 다시 읽는다', async () => {
    failSecondPageOnce([record('recordAAAAAA', '첫 쪽 메모', '2026-09-01')])
    renderWithProviders(<Harness />)

    await screen.findByText('첫 쪽 메모')
    fireEvent.click(screen.getByRole('button', { name: '다음 페이지' }))

    // 오류 문구 아래 1쪽 목록과 페이지 이동이 그대로
    expect((await screen.findByRole('alert')).textContent).toBe('정비 이력을 불러오지 못했습니다.')
    expect(screen.getByText('첫 쪽 메모')).toBeTruthy()
    // 보이는 쪽으로 되돌아가 1쪽을 다시 읽음
    await waitFor(() => expect(vi.mocked(fetchRecords).mock.calls.map((call) => call[1])).toEqual([0, 1, 0]))
    expect(screen.getByRole('alert')).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: '다음 페이지' }))
    await screen.findByText('둘째 쪽 메모')
    expect(vi.mocked(fetchRecords).mock.calls.map((call) => call[1])).toEqual([0, 1, 0, 1])
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('쪽 이동 실패 문구가 떠 있어도 삭제 실패 문구가 함께 보인다', async () => {
    vi.stubGlobal('confirm', () => true)
    vi.mocked(deleteRecord).mockRejectedValue(new TypeError('Failed to fetch'))
    failSecondPageOnce([record('recordAAAAAA', '첫 쪽 메모', '2026-09-01')])
    renderWithProviders(<Harness />)

    await screen.findByText('첫 쪽 메모')
    fireEvent.click(screen.getByRole('button', { name: '다음 페이지' }))
    await screen.findByRole('alert')

    fireEvent.click(screen.getByRole('button', { name: /삭제$/ }))

    // 조회 실패 하나만 보이면 삭제를 시도했는지 알 수 없음
    await waitFor(() => expect(screen.getAllByRole('alert')).toHaveLength(2))
    expect(screen.getByText('정비 이력을 불러오지 못했습니다.')).toBeTruthy()
  })

  it('쪽 이동이 실패한 뒤 보이는 쪽에서 지우면 그 쪽을 다시 읽는다', async () => {
    vi.stubGlobal('confirm', () => true)
    vi.mocked(deleteRecord).mockResolvedValue(undefined as never)
    failSecondPageOnce([record('recordAAAAAA', '첫 쪽 메모', '2026-09-01')])
    renderWithProviders(<Harness />)

    await screen.findByText('첫 쪽 메모')
    fireEvent.click(screen.getByRole('button', { name: '다음 페이지' }))
    await screen.findByRole('alert')
    await waitFor(() => expect(vi.mocked(fetchRecords).mock.calls).toHaveLength(3))

    fireEvent.click(screen.getByRole('button', { name: /삭제$/ }))

    // 실패한 2쪽이 아니라 보이는 1쪽을 다시 읽음. 2쪽을 읽으면 고르지 않은 쪽으로 넘어감
    await waitFor(() => expect(vi.mocked(fetchRecords).mock.calls).toHaveLength(4))
    expect(vi.mocked(fetchRecords).mock.calls[3][1]).toBe(0)
    expect(screen.queryByText('둘째 쪽 메모')).toBeNull()
  })
})
