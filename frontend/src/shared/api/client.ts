import type { ErrorResponse } from '@/shared/api/types'

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
 * 세션이 끊겨 401이 돌아왔을 때 호출할 함수. AuthProvider가 등록한다.
 * api.ts는 React 밖의 평범한 모듈이라 컨텍스트를 직접 읽을 수 없어서, 이렇게 함수를 건네받는다.
 */
let onUnauthorized: (() => void) | null = null

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

// 이 두 경로의 401은 "세션 만료"가 아니다.
// - 로그인: 비밀번호가 틀린 것 → 폼 에러로 보여줘야 한다
// - /me: 앱 시작 시 "로그인했나?"를 물어보는 요청 → 401이 정상적인 답이다
const SKIP_UNAUTHORIZED_HANDLER = ['/api/users/login', '/api/users/me']

async function request<T>(method: Method, path: string, body?: unknown): Promise<T> {
  const hasBody = body !== undefined

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    // 세션 쿠키(JSESSIONID)를 보내고 받기 위해 필수. 빠지면 전부 401이 난다.
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

  // 204 No Content(로그아웃·삭제)는 본문이 없어 json() 호출이 실패한다.
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
    // 핸들러가 없는 500 등은 JSON이 아닐 수 있다. 아래 기본 메시지로 넘어간다.
  }

  return `요청에 실패했습니다 (HTTP ${response.status})`
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  patch: <T>(path: string, body?: unknown) => request<T>('PATCH', path, body),
  del: <T = void>(path: string) => request<T>('DELETE', path),
}
