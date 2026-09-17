import { createContext, useContext } from 'react'

import type {
  LoginRequest,
  UserResponse,
  WithdrawRequest,
} from '@/features/auth/api/types/types'

export interface AuthContextValue {
  /** 로그인하지 않았으면 null */
  user: UserResponse | null
  /** 최초 세션 복구 전에는 true */
  loading: boolean
  login: (request: LoginRequest) => Promise<void>
  logout: () => Promise<void>
  /**
   * 탈퇴. 로그아웃과 같은 자리인 이유 — "성공하면 로그인 상태가 사라짐"이 화면의 사정이 아니라 이 동작의 정의
   * 실패는 그대로 전달. 로그아웃과 달리 "실패해도 비움"이 성립하지 않음
   */
  withdraw: (request: WithdrawRequest) => Promise<void>
  /** 서버가 새 사용자 정보를 돌려줄 때 갱신용 */
  replaceUser: (user: UserResponse) => void
}

// 기본값 null — Provider 를 빠뜨리면 조용히 넘어가지 않고 바로 터짐
export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth() {
  const value = useContext(AuthContext)

  if (value === null) {
    throw new Error('useAuth()는 <AuthProvider> 안에서만 사용할 수 있습니다.')
  }

  return value
}
