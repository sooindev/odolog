import type { ErrorResponse } from '@/shared/api/types/types'

// 백엔드 주소. 개발은 .env.development 의 http://localhost:8080
// 없으면 빈 문자열 = 같은 출처 상대 경로. undefined 가 주소에 섞이는 문제 방지
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

/**
 * 서버에 닿지 못한 요청의 status. 어떤 HTTP 상태와도 겹치지 않는 0
 * 서버 다운이 입력 오류 문구로 보이는 문제 방지
 */
export const NETWORK_ERROR_STATUS = 0

/** 상태 코드를 담은 에러. 401/409 구분용 */
export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

type Method = 'GET' | 'POST' | 'PATCH' | 'DELETE'

// CSRF 토큰. XSRF-TOKEN 쿠키 값을 쓰기 요청 헤더로 되돌림(double submit)
// HttpOnly 가 아닌 유일한 쿠키
const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'

function readCsrfToken() {
  const found = document.cookie
    .split('; ')
    .find((entry) => entry.startsWith(`${CSRF_COOKIE}=`))

  return found === undefined ? null : decodeURIComponent(found.slice(CSRF_COOKIE.length + 1))
}

/**
 * 401 콜백. AuthProvider 가 등록
 * React 밖이라 함수로 전달받음
 */
let onUnauthorized: (() => void) | null = null

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

// 전역 401 처리 제외 목록. 세션 만료가 아닌 401
// POST /login: 비밀번호 오류 · GET /me: 비로그인 확인 · DELETE /me: 탈퇴 비밀번호 오류 · PATCH /me/password: 현재 비밀번호 오류
// 메서드까지 비교. PATCH /me 의 세션 만료는 전역 처리 대상
const SKIP_UNAUTHORIZED_HANDLER = [
  'POST /api/users/login',
  'GET /api/users/me',
  'DELETE /api/users/me',
  'PATCH /api/users/me/password',
]

async function request<T>(method: Method, path: string, body?: unknown): Promise<T> {
  const hasBody = body !== undefined

  const headers: Record<string, string> = {}
  if (hasBody) {
    headers['Content-Type'] = 'application/json'
  }

  // GET 에도 붙임. 무해
  const csrfToken = readCsrfToken()
  if (csrfToken !== null) {
    headers[CSRF_HEADER] = csrfToken
  }

  let response: Response

  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method,
      // 세션 쿠키 필수
      credentials: 'include',
      headers,
      body: hasBody ? JSON.stringify(body) : undefined,
    })
  } catch {
    // 서버 다운·네트워크 끊김. 입력 오류와 구분
    throw new ApiError(
      NETWORK_ERROR_STATUS,
      '서버에 연결하지 못했습니다. 네트워크와 백엔드 실행 상태를 확인해 주세요.',
    )
  }

  if (!response.ok) {
    if (response.status === 401 && !SKIP_UNAUTHORIZED_HANDLER.includes(`${method} ${path}`)) {
      onUnauthorized?.()
    }
    throw new ApiError(response.status, await readErrorMessage(response))
  }

  // 204 는 본문 없음
  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

/** ErrorResponse.message 추출 */
async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ErrorResponse
    if (body.message) {
      return body.message
    }
  } catch {
    // JSON 이 아닌 응답은 기본 문구
  }

  return `요청에 실패했습니다 (HTTP ${response.status})`
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  patch: <T>(path: string, body?: unknown) => request<T>('PATCH', path, body),
  // 본문 있는 DELETE. 탈퇴 비밀번호용
  del: <T = void>(path: string, body?: unknown) => request<T>('DELETE', path, body),
}
