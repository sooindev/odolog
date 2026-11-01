import { cleanup, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import { MonthlyCostChart } from '@/features/summary/components/charts/HomeCharts'
import type { MonthlyCost } from '@/features/summary/api/types'
import { renderWithProviders } from '@/shared/lib/renderWithProviders'

function month(value: string, maintenanceCost: number, fuelCost: number, count: number): MonthlyCost {
  return { month: value, cost: maintenanceCost + fuelCost, maintenanceCost, fuelCost, count }
}

describe('MonthlyCostChart', () => {
  // vitest globals 가 꺼져 있어 자동 정리가 없음
  afterEach(cleanup)

  it('Tab 으로 지나가는 달 칸마다 화면 낭독기가 읽을 이름이 있다', () => {
    renderWithProviders(
      <MonthlyCostChart
        monthly={[month('2026-09', 30000, 20000, 3), month('2026-10', 0, 0, 0)]}
        currency="KRW"
      />,
    )

    // 이름이 없으면 낭독기는 빈 칸을 지나감
    const columns = screen.getAllByRole('img')
    expect(columns).toHaveLength(2)
    expect(columns[0].getAttribute('aria-label')).toBe('2026년 9월 · 3건, 50,000원, 정비 30,000원 · 주유 20,000원')
    expect(columns[1].getAttribute('aria-label')).toBe('2026년 10월 · 0건, 0원')
  })
})
