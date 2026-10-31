import { cleanup, fireEvent, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { ResetPasswordPage } from '@/features/auth/pages/ResetPasswordPage'
import type { UserResponse } from '@/features/auth/api/types'
import { renderWithProviders } from '@/shared/lib/renderWithProviders'

vi.mock('@/features/auth/api/endpoints', () => ({
  confirmPasswordReset: vi.fn(),
}))

// 로그인 여부와 로그아웃 결과만 바꿔 가며 봄
const auth = { user: null as UserResponse | null, logout: vi.fn() }
vi.mock('@/features/auth/context/AuthContext', () => ({
  useAuth: () => auth,
}))

const { confirmPasswordReset } = await import('@/features/auth/api/endpoints')

function renderPage() {
  renderWithProviders(
    <MemoryRouter initialEntries={['/reset-password?token=token123']}>
      <Routes>
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/login" element={<p>로그인 화면</p>} />
      </Routes>
    </MemoryRouter>,
  )
  fireEvent.change(screen.getByLabelText('새 비밀번호'), { target: { value: 'newpassword1' } })
  fireEvent.change(screen.getByLabelText('새 비밀번호 확인'), { target: { value: 'newpassword1' } })
  fireEvent.click(screen.getByRole('button', { name: '비밀번호 변경' }))
}

describe('ResetPasswordPage — 재설정 뒤 로그아웃이 실패하면', () => {
  afterEach(cleanup)

  beforeEach(() => {
    vi.mocked(confirmPasswordReset).mockReset().mockResolvedValue(undefined)
    auth.logout.mockReset().mockRejectedValue(new TypeError('Failed to fetch'))
  })

  it('로그인하지 않은 브라우저는 다른 계정 안내 없이 로그인 화면으로', async () => {
    auth.user = null
    renderPage()

    expect(await screen.findByText('로그인 화면')).toBeTruthy()
  })

  it('다른 계정으로 로그인한 브라우저는 그 계정을 로그아웃하라고 알린다', async () => {
    auth.user = { email: 'other@odolog.com' } as UserResponse
    renderPage()

    expect(await screen.findByText(/다른 계정으로 로그인되어 있는데/)).toBeTruthy()
  })
})
