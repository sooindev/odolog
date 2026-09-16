import { createContext, useContext } from 'react'

import type {
  LoginRequest,
  UserResponse,
  WithdrawRequest,
} from '@/features/auth/api/types/types'

export interface AuthContextValue {
  /** 로그인하지 않았으면 null */
  user: UserResponse | null
  /** 최초 세션 복구가 끝나기 전에는 true */
  loading: boolean
  login: (request: LoginRequest) => Promise<void>
  logout: () => Promise<void>
  /**
   * 탈퇴. 로그아웃과 같은 자리에 두는 이유는 "성공하면 로그인 상태가 사라진다"가
   * 화면의 사정이 아니라 이 동작의 정의이기 때문이다.
   * 실패는 그대로 던진다 — 로그아웃과 달리 "실패해도 비운다"가 성립하지 않는다.
   */
  withdraw: (request: WithdrawRequest) => Promise<void>
  /** 프로필 수정처럼 서버가 새 사용자 정보를 돌려줄 때 갱신용 */
  replaceUser: (user: UserResponse) => void
}

// 기본값을 null로 두면 <AuthProvider>로 감싸는 걸 빠뜨렸을 때 조용히 동작하지 않고 바로 터진다.
export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth() {
  const value = useContext(AuthContext)

  if (value === null) {
    throw new Error('useAuth()는 <AuthProvider> 안에서만 사용할 수 있습니다.')
  }

  return value
}
