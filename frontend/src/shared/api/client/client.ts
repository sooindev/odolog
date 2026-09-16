import type { ErrorResponse } from '@/shared/api/types/types'

const BASE_URL = import.meta.env.VITE_API_BASE_URL

/** HTTP 상태 코드를 함께 들고 다니는 에러. 화면에서 401/409 등을 구분하는 데 쓴다. */
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
 * 401 이 돌아왔을 때 호출할 함수. AuthProvider 가 등록한다.
 * 이 파일은 React 밖이라 컨텍스트를 직접 읽을 수 없어서 함수를 건네받는다.
 */
let onUnauthorized: (() => void) | null = null

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

// 여기 있는 경로의 401 은 "세션이 끊겼다"가 아니다. 전역 핸들러가 돌면 사용자 정보를 비우고
// ProtectedRoute 가 /login 으로 보내 버리는데, 이 경로들에서는 그게 오답이다.
//
//   /api/users/login          비밀번호가 틀린 것
//   /api/users/me (GET)       "로그인했나"를 묻는 요청이라 401 이 정상적인 답
//   /api/users/me (DELETE)    탈퇴 시 비밀번호가 틀린 것 — 여기서 쫓아내면 안 된다
//   /api/users/me/password    현재 비밀번호가 틀린 것. 오타 한 번에 로그아웃되면 안 된다
//
// includes 는 정확히 일치하는 문자열만 찾으므로 하위 경로는 따로 적어야 한다.
const SKIP_UNAUTHORIZED_HANDLER = [
  '/api/users/login',
  '/api/users/me',
  '/api/users/me/password',
]

async function request<T>(method: Method, path: string, body?: unknown): Promise<T> {
  const hasBody = body !== undefined

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    // 세션 쿠키를 주고받으려면 필수. 빠지면 전부 401 이 난다.
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

  // 204 는 본문이 없어 json() 이 실패한다.
  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

/** 백엔드 GlobalExceptionHandler가 내려주는 ErrorResponse.message를 꺼낸다. */
async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ErrorResponse
    if (body.message) {
      return body.message
    }
  } catch {
    // 핸들러가 없는 500 은 JSON 이 아닐 수 있다. 기본 메시지로 넘어간다.
  }

  return `요청에 실패했습니다 (HTTP ${response.status})`
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  patch: <T>(path: string, body?: unknown) => request<T>('PATCH', path, body),
  // 탈퇴가 비밀번호를 실어 보낸다. 쿼리 파라미터에 넣으면 접근 로그와 브라우저 기록에
  // 평문으로 남는다. request() 는 원래 모든 메서드에서 본문을 지원하고 있었다.
  del: <T = void>(path: string, body?: unknown) => request<T>('DELETE', path, body),
}
