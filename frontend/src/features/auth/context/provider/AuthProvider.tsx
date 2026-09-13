import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

import { AuthContext } from '@/features/auth/context/definition/AuthContext'
import { setUnauthorizedHandler } from '@/shared/api/client/client'
import {
  fetchMe,
  login as requestLogin,
  logout as requestLogout,
} from '@/features/auth/api/endpoints/endpoints'
import type { LoginRequest, UserResponse } from '@/features/auth/api/types/types'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null)
  const [loading, setLoading] = useState(true)

  // 새로고침하면 React 상태는 전부 사라지지만 세션 쿠키는 브라우저에 남아 있다.
  // 앱이 뜰 때 /me를 한 번 호출해 "누구로 로그인돼 있는지"를 서버에 되묻는 것이 로그인 유지의 정체다.
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

  const logout = useCallback(async () => {
    await requestLogout()
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, loading, login, logout, replaceUser: setUser }),
    [user, loading, login, logout],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}
