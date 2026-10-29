import { cleanup, fireEvent, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { DateInput } from '@/shared/ui/form/date-input'
import { renderWithProviders } from '@/shared/lib/renderWithProviders'

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
