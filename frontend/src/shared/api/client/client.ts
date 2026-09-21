import type { ErrorResponse } from '@/shared/api/types/types'

/*
 * 백엔드 주소. 개발은 .env.development 가 http://localhost:8080 을 준다
 * ?? '' 가 없으면 그 파일이 안 걸리는 빌드에서 undefined 가 문자열로 이어붙어
 * 모든 요청이 <출처>/undefined/api/... 로 나간다 — 빌드는 통과하고 앱만 죽는다
 * 빈 문자열이면 같은 출처의 상대 경로가 되어, 프런트와 API 를 한 출처에 두는 배포에서 맞다
 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

/**
 * 요청이 서버에 닿지도 못했을 때의 status
 * fetch 는 이때 TypeError 를 던지는데 그건 ApiError 가 아니라서, 화면마다 걸어 둔
 * `e instanceof ApiError ? e.message : '…에 실패했습니다'` 의 뒤쪽으로 떨어졌다
 * 결과적으로 백엔드가 꺼져 있어도 '로그인에 실패했습니다' 가 떠서 비밀번호를 다시 치게 됐다
 * 0 을 쓰는 이유는 어떤 HTTP 상태와도 겹치지 않아 status 비교를 건드리지 않기 때문
 */
export const NETWORK_ERROR_STATUS = 0

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

/*
 * CSRF 토큰. 백엔드가 XSRF-TOKEN 쿠키로 내려주고, 바꾸는 요청에는 같은 값을 헤더로 되돌려준다
 * 다른 출처의 페이지는 이 쿠키를 읽을 수 없고(JS 접근은 같은 출처만),
 * 커스텀 헤더는 CORS 사전 요청을 통과해야 붙는다 — 그래서 값을 알 수도 실을 수도 없다
 * HttpOnly 가 아닌 유일한 쿠키다. 세션 쿠키(JSESSIONID)는 여전히 JS 가 못 읽는다
 */
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

  const headers: Record<string, string> = {}
  if (hasBody) {
    headers['Content-Type'] = 'application/json'
  }

  // GET 은 서버가 검사하지 않지만, 붙어도 무해하므로 메서드를 가르지 않는다
  const csrfToken = readCsrfToken()
  if (csrfToken !== null) {
    headers[CSRF_HEADER] = csrfToken
  }

  let response: Response

  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method,
      // 세션 쿠키 필수. 빠지면 전부 401
      credentials: 'include',
      headers,
      body: hasBody ? JSON.stringify(body) : undefined,
    })
  } catch {
    // 서버가 안 떠 있거나 네트워크가 끊긴 경우. 입력이 틀린 것과 구분해서 말해 준다
    throw new ApiError(
      NETWORK_ERROR_STATUS,
      '서버에 연결하지 못했습니다. 네트워크와 백엔드 실행 상태를 확인해 주세요.',
    )
  }

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
