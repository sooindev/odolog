import type { ErrorResponse } from '@/shared/api/types/types'

const BASE_URL = import.meta.env.VITE_API_BASE_URL

/** 상태 코드를 들고 다니는 에러. 화면에서 401/409 구분용 */
export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

type Method = 'GET' | 'POST' | 'PATCH' | 'DELETE'

/**
 * 401 콜백. AuthProvider 가 등록
 * 이 파일은 React 밖이라 컨텍스트를 직접 못 읽어 함수를 건네받음
 */
let onUnauthorized: (() => void) | null = null

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

// 여기 경로의 401 은 "세션 끊김"이 아님. 전역 핸들러가 돌면 로그아웃되고 /login 으로 밀려남
//
//   /api/users/login          비밀번호 오류
//   /api/users/me (GET)       "로그인했나"를 묻는 요청이라 401 이 정상 응답
//   /api/users/me (DELETE)    탈퇴 시 비밀번호 오류
//   /api/users/me/password    현재 비밀번호 오류 — 오타 한 번에 로그아웃되면 안 됨
//
// includes 는 정확히 일치하는 문자열만 찾으므로 하위 경로는 따로 등록
const SKIP_UNAUTHORIZED_HANDLER = [
  '/api/users/login',
  '/api/users/me',
  '/api/users/me/password',
]

async function request<T>(method: Method, path: string, body?: unknown): Promise<T> {
  const hasBody = body !== undefined

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    // 세션 쿠키 필수. 빠지면 전부 401
    credentials: 'include',
    headers: hasBody ? { 'Content-Type': 'application/json' } : undefined,
    body: hasBody ? JSON.stringify(body) : undefined,
  })

  if (!response.ok) {
    if (response.status === 401 && !SKIP_UNAUTHORIZED_HANDLER.includes(path)) {
      onUnauthorized?.()
    }
    throw new ApiError(response.status, await readErrorMessage(response))
  }

  // 204 는 본문이 없어 json() 이 실패
  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

/** 백엔드 ErrorResponse.message 추출 */
async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ErrorResponse
    if (body.message) {
      return body.message
    }
  } catch {
    // 핸들러 없는 500 은 JSON 이 아닐 수 있음. 기본 메시지로
  }

  return `요청에 실패했습니다 (HTTP ${response.status})`
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  patch: <T>(path: string, body?: unknown) => request<T>('PATCH', path, body),
  // 탈퇴가 비밀번호를 본문에 실음. 쿼리 파라미터면 접근 로그·브라우저 기록에 평문으로 남음
  del: <T = void>(path: string, body?: unknown) => request<T>('DELETE', path, body),
}
