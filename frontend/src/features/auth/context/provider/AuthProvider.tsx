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

  // 새로고침하면 React 상태는 사라지지만 세션 쿠키는 남음
  // 앱이 뜰 때 /me 를 한 번 부르는 것이 로그인 유지의 정체
  useEffect(() => {
    let cancelled = false

    async function restoreSession() {
      try {
        const me = await fetchMe()
        if (!cancelled) setUser(me)
      } catch {
        // 401 = 비로그인. 서버가 꺼진 경우도 로그아웃 상태로 시작
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

  // 세션이 끊기면 사용자 정보 비우기 → ProtectedRoute 가 /login 으로
  useEffect(() => {
    setUnauthorizedHandler(() => setUser(null))
  }, [])

  const login = useCallback(async (request: LoginRequest) => {
    setUser(await requestLogin(request))
  }, [])

  // 실패해도 상태는 비움. 그대로 던지면 호출부의 navigate 까지 막혀 버튼이 고장 난 것처럼 보임
  // 서버 세션이 남아 새로고침 때 복구될 수는 있지만 그건 실제로 로그인된 상태
  const logout = useCallback(async () => {
    try {
      await requestLogout()
    } catch {
      // 서버 다운·네트워크 끊김. 화면에서는 로그아웃으로 처리
    } finally {
      setUser(null)
    }
  }, [])

  // 로그아웃과 달리 실패를 삼키지 않음
  // 탈퇴가 실패했는데 로그아웃된 것처럼 보이면 계정이 지워졌다고 믿게 됨
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
