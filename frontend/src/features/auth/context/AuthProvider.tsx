import { startTransition, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'

import { AuthContext } from '@/features/auth/context/AuthContext'
import { ApiError, setUnauthorizedHandler } from '@/shared/api/client'
import {
  fetchMe,
  login as requestLogin,
  logout as requestLogout,
  withdraw as requestWithdraw,
} from '@/features/auth/api/endpoints'
import type {
  LoginRequest,
  UserResponse,
  WithdrawRequest,
} from '@/features/auth/api/types'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [sessionEnded, setSessionEnded] = useState(false)
  // 끊긴 세션의 이메일. 다른 계정으로 다시 로그인하면 앞 계정의 주소로 돌아가지 않게
  const [endedEmail, setEndedEmail] = useState<string | null>(null)
  // 401 처리기가 끊기기 직전의 사용자를 알도록
  const userRef = useRef<UserResponse | null>(null)
  useEffect(() => {
    userRef.current = user
  }, [user])
  // 로그인·로그아웃·탈퇴가 먼저 끝났는지. 늦게 온 첫 /me 가 그 결과를 덮지 않게
  const sessionDecided = useRef(false)
  // 계정이 바뀌는 모든 순간에 비움. 다른 계정의 차량·기록이 캐시로 보이지 않게
  const queryClient = useQueryClient()

  // 앱 기동 시 /me 한 번으로 세션 복구
  useEffect(() => {
    let cancelled = false

    async function restoreSession() {
      try {
        const me = await fetchMe()
        if (!cancelled && !sessionDecided.current) setUser(me)
      } catch {
        // 401 = 비로그인. 서버 다운도 로그아웃 상태로 시작
        if (!cancelled && !sessionDecided.current) setUser(null)
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
    setUnauthorizedHandler(() => {
      setEndedEmail(userRef.current?.email ?? null)
      setSessionEnded(true)
      setUser(null)
      queryClient.clear()
    })
  }, [queryClient])

  const login = useCallback(async (request: LoginRequest) => {
    const me = await requestLogin(request)
    sessionDecided.current = true
    // 다른 계정으로 바꿔 로그인해도 앞 계정의 캐시가 남지 않게
    queryClient.clear()
    setSessionEnded(false)
    setEndedEmail(null)
    setUser(me)
  }, [queryClient])

  // 서버 세션이 남았을 수 있으면(네트워크 등) 던지고 로그인 상태 유지. 화면만 비우면 새로고침 때 다시 로그인된 채
  // 401 은 이미 끊긴 세션이라 성공으로 봄
  // 이동과 상태 지우기는 탈퇴와 같은 transition. 따로면 보호 라우트가 지금 주소를 from 으로 남김
  const logout = useCallback(async (leave: () => void) => {
    try {
      await requestLogout()
    } catch (caught) {
      if (!(caught instanceof ApiError && caught.status === 401)) {
        throw caught
      }
    }
    sessionDecided.current = true
    queryClient.clear()
    startTransition(() => {
      leave()
      setSessionEnded(false)
      setEndedEmail(null)
      setUser(null)
    })
  }, [queryClient])

  // 탈퇴 실패는 그대로 전달. 지워진 것처럼 보이는 문제 방지
  // 이동과 로그인 상태 지우기를 같은 transition 으로. 라우터 이동은 transition 이라
  // 상태 지우기만 먼저 그려지면 아직 /me 인 보호 라우트가 /login 으로 보냄(E2E 로 재현)
  const withdraw = useCallback(async (request: WithdrawRequest, leave: () => void) => {
    await requestWithdraw(request)
    sessionDecided.current = true
    startTransition(() => {
      leave()
      setSessionEnded(false)
      setEndedEmail(null)
      setUser(null)
    })
    queryClient.clear()
  }, [queryClient])

  // 로그인 화면이 안내를 한 번 보여 준 뒤. 안 끄면 로그인 없이 나갔다 와도 끊김 안내가 다시 뜸
  const acknowledgeSessionEnded = useCallback(() => {
    setSessionEnded(false)
  }, [])

  const value = useMemo(
    () => ({
      user,
      loading,
      sessionEnded,
      endedEmail,
      login,
      logout,
      withdraw,
      acknowledgeSessionEnded,
      replaceUser: setUser,
    }),
    [user, loading, sessionEnded, endedEmail, login, logout, withdraw, acknowledgeSessionEnded],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}
