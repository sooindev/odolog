import { createContext, useContext } from 'react'

import type {
  LoginRequest,
  UserResponse,
  WithdrawRequest,
} from '@/features/auth/api/types/types'

export interface AuthContextValue {
  /** 비로그인이면 null */
  user: UserResponse | null
  /** 최초 세션 복구 전에는 true */
  loading: boolean
  login: (request: LoginRequest) => Promise<void>
  logout: () => Promise<void>
  /**
   * 탈퇴. 성공 시 로그인 상태 제거
   * 실패는 그대로 전달
   */
  withdraw: (request: WithdrawRequest) => Promise<void>
  /** 서버가 돌려준 사용자 정보로 교체 */
  replaceUser: (user: UserResponse) => void
}

// 기본값 null. Provider 누락 시 즉시 오류
export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth() {
  const value = useContext(AuthContext)

  if (value === null) {
    throw new Error('useAuth()는 <AuthProvider> 안에서만 사용할 수 있습니다.')
  }

  return value
}
