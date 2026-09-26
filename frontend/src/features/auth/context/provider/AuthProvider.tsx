import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

import { AuthContext } from '@/features/auth/context/definition/AuthContext'
import { setUnauthorizedHandler } from '@/shared/api/client/client'
import {
  fetchMe,
  login as requestLogin,
  logout as requestLogout,
  withdraw as requestWithdraw,
} from '@/features/auth/api/endpoints/endpoints'
import type {
  LoginRequest,
  UserResponse,
  WithdrawRequest,
} from '@/features/auth/api/types/types'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null)
  const [loading, setLoading] = useState(true)

  // 앱 기동 시 /me 한 번으로 세션 복구
  useEffect(() => {
    let cancelled = false

    async function restoreSession() {
      try {
        const me = await fetchMe()
        if (!cancelled) setUser(me)
      } catch {
        // 401 = 비로그인. 서버 다운도 로그아웃 상태로 시작
        if (!cancelled) setUser(null)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    restoreSession()

    return () => {
      cancelled = true
    }
  }, [])

  // 세션 만료 시 사용자 정보 비움 → ProtectedRoute 가 /login 으로
  useEffect(() => {
    setUnauthorizedHandler(() => setUser(null))
  }, [])

  const login = useCallback(async (request: LoginRequest) => {
    setUser(await requestLogin(request))
  }, [])

  // 실패해도 상태는 비움. 호출부의 이동이 막히지 않게
  const logout = useCallback(async () => {
    try {
      await requestLogout()
    } catch {
      // 서버 다운·네트워크 끊김도 로그아웃 처리
    } finally {
      setUser(null)
    }
  }, [])

  // 탈퇴 실패는 그대로 전달. 지워진 것처럼 보이는 문제 방지
  const withdraw = useCallback(async (request: WithdrawRequest) => {
    await requestWithdraw(request)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, loading, login, logout, withdraw, replaceUser: setUser }),
    [user, loading, login, logout, withdraw],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}
