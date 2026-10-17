import { cleanup, screen } from '@testing-library/react'
import { Suspense, lazy } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { ScreenErrorBoundary } from '@/app/ScreenErrorBoundary'
import { renderWithProviders } from '@/shared/lib/renderWithProviders'

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

describe('ScreenErrorBoundary', () => {
  it('화면 조각을 못 받으면 흰 화면 대신 안내와 새로고침 버튼', async () => {
    // React 가 잡은 오류를 콘솔에 찍는 것은 예상된 일
    vi.spyOn(console, 'error').mockImplementation(() => {})
    const Missing = lazy(() => Promise.reject(new TypeError('Failed to fetch dynamically imported module')))

    renderWithProviders(
      <ScreenErrorBoundary>
        <Suspense fallback={null}>
          <Missing />
        </Suspense>
      </ScreenErrorBoundary>,
    )

    expect((await screen.findByRole('alert')).textContent).toContain('화면을 불러오지 못했습니다')
    expect(screen.getByRole('button', { name: '새로고침' })).toBeTruthy()
  })

  it('실패가 없으면 자식을 그대로', () => {
    renderWithProviders(
      <ScreenErrorBoundary>
        <p>본문</p>
      </ScreenErrorBoundary>,
    )

    expect(screen.getByText('본문')).toBeTruthy()
    expect(screen.queryByRole('alert')).toBeNull()
  })
})
