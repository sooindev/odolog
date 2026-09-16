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

  // 새로고침하면 React 상태는 사라지지만 세션 쿠키는 남는다.
  // 앱이 뜰 때 /me 를 한 번 불러 서버에 "누구냐"를 되묻는 것이 로그인 유지의 정체다.
  useEffect(() => {
    let cancelled = false

    async function restoreSession() {
      try {
        const me = await fetchMe()
        if (!cancelled) setUser(me)
      } catch {
        // 401이면 로그인 안 한 상태다. 서버가 꺼져 있는 경우도 로그아웃 상태로 시작한다.
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

  // 세션이 끊기면 사용자 정보를 비운다. 그러면 ProtectedRoute가 알아서 /login으로 보낸다.
  useEffect(() => {
    setUnauthorizedHandler(() => setUser(null))
  }, [])

  const login = useCallback(async (request: LoginRequest) => {
    setUser(await requestLogin(request))
  }, [])

  // 요청이 실패해도 상태는 비운다. 그대로 던지면 호출한 쪽의 navigate 까지 막혀서
  // 사용자 눈에는 버튼이 고장 난 것으로 보인다.
  // 서버 세션이 남아 새로고침 때 /me 로 복구될 수는 있지만, 그건 실제로 로그인된 상태다.
  const logout = useCallback(async () => {
    try {
      await requestLogout()
    } catch {
      // 서버가 꺼졌거나 네트워크가 끊긴 경우. 화면에서는 로그아웃으로 다룬다.
    } finally {
      setUser(null)
    }
  }, [])

  // 로그아웃과 달리 실패를 삼키지 않는다. 로그아웃은 실패해도 "화면에서는 나간 것"으로
  // 다루면 되지만, 탈퇴가 실패했는데 로그아웃된 것처럼 보이면 계정이 지워졌다고 믿게 된다.
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
