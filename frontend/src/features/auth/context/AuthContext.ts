import { createContext, useContext } from 'react'

import type {
  LoginRequest,
  UserResponse,
  WithdrawRequest,
} from '@/features/auth/api/types'

export interface AuthContextValue {
  /** 비로그인이면 null */
  user: UserResponse | null
  /** 최초 세션 복구 전에는 true */
  loading: boolean
  /** 서버가 세션을 끊어(401) 로그아웃된 상태. 로그인 화면 안내용 */
  sessionEnded: boolean
  /** 끊긴 세션의 이메일. 다른 계정으로 로그인하면 앞 계정의 주소로 돌아가지 않게 */
  endedEmail: string | null
  /** 로그인 화면이 끊김 안내를 보여 준 뒤 끔 */
  acknowledgeSessionEnded: () => void
  login: (request: LoginRequest) => Promise<void>
  /**
   * 로그아웃. 성공 시 leave 로 이동하며 로그인 상태 제거
   * 서버 세션을 못 끊었으면(네트워크 등) 던지고 로그인 상태 유지
   */
  logout: (leave: () => void) => Promise<void>
  /**
   * 탈퇴. 성공 시 로그인 상태 제거
   * 실패는 그대로 전달
   */
  withdraw: (request: WithdrawRequest, leave: () => void) => Promise<void>
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
