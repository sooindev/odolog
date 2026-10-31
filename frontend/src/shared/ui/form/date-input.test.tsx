import { cleanup, fireEvent, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { DateInput } from '@/shared/ui/form/date-input'
import { renderWithProviders } from '@/shared/lib/renderWithProviders'
import { todayString } from '@/shared/lib/format'

describe('DateInput 드럼 휠', () => {
  afterEach(cleanup)

  beforeEach(() => {
    // 터치 기기 흉내. 휠이 뜸
    vi.stubGlobal('matchMedia', () => ({
      matches: true,
      addEventListener: () => {},
      removeEventListener: () => {},
    }))
  })

  it('옵션을 탭하면 그 값으로 바뀐다 — 스크롤 없이 고르는 길(화면 낭독기의 두 번 탭)', () => {
    const onChange = vi.fn()
    renderWithProviders(<DateInput id="date" value="2025-10-01" onChange={onChange} />)

    fireEvent.click(screen.getByRole('button'))
    const year = screen.getAllByRole('option').find((option) => option.textContent?.startsWith('2024'))
    fireEvent.click(year!)

    expect(onChange).toHaveBeenCalledWith('2024-10-01')
  })

  it('각 칸이 지금 고른 옵션을 aria-activedescendant 로 가리킨다', () => {
    renderWithProviders(<DateInput id="date" value="2025-10-01" onChange={() => {}} />)

    fireEvent.click(screen.getByRole('button'))

    for (const listbox of screen.getAllByRole('listbox')) {
      const active = document.getElementById(listbox.getAttribute('aria-activedescendant') ?? '')
      expect(active?.getAttribute('aria-selected')).toBe('true')
    }
  })
})

describe('DateInput 데스크톱 칸의 상한', () => {
  afterEach(cleanup)

  beforeEach(() => {
    // 마우스 기기 흉내. 네이티브 date 칸이 뜸
    vi.stubGlobal('matchMedia', () => ({
      matches: false,
      addEventListener: () => {},
      removeEventListener: () => {},
    }))
  })

  it('처음 받은 날짜가 오늘보다 늦으면 그날이 상한 — 안 고친 날짜로 제출이 막히지 않게', () => {
    const { container } = renderWithProviders(<DateInput id="date" value="2999-01-01" onChange={() => {}} />)

    expect(container.querySelector('input')?.max).toBe('2999-01-01')
  })

  it('새 기록(빈 값)은 오늘이 상한', () => {
    const { container } = renderWithProviders(<DateInput id="date" value="" onChange={() => {}} />)

    expect(container.querySelector('input')?.max).toBe(todayString('Asia/Seoul'))
  })
})
