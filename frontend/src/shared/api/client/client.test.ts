import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError, NETWORK_ERROR_STATUS, api } from './client'

/*
 * 이 테스트가 도는 환경에는 VITE_API_BASE_URL 이 없다 (.env.development 는 dev 전용)
 * 즉 프로덕션 빌드와 같은 조건이라, 대비책이 빠지면 여기서 바로 드러난다
 */

// typeof fetch 를 붙이지 않으면 인자 없는 함수로 추론되어 calls[0][1] 을 읽을 수 없다
function mockFetch(status = 200, body: unknown = {}) {
  const spy = vi.fn<typeof fetch>(
    async () => new Response(status === 204 ? null : JSON.stringify(body), { status }),
  )
  vi.stubGlobal('fetch', spy)
  return spy
}

describe('api 클라이언트의 요청 주소', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('BASE_URL 이 없으면 같은 출처의 상대 경로로 나간다', async () => {
    // ?? '' 가 빠지면 'undefined/api/users/me' 가 된다 — 빌드는 통과하고 앱만 죽는다
    const spy = mockFetch()
    await api.get('/api/users/me')

    expect(spy.mock.calls[0][0]).toBe('/api/users/me')
    expect(String(spy.mock.calls[0][0])).not.toContain('undefined')
  })

  it('세션 쿠키를 항상 싣는다', async () => {
    const spy = mockFetch()
    await api.get('/api/users/me')

    expect(spy.mock.calls[0][1]).toMatchObject({ credentials: 'include' })
  })

  it('본문이 없으면 Content-Type 을 붙이지 않는다', async () => {
    const spy = mockFetch()
    await api.post('/api/users/logout')

    expect(spy.mock.calls[0][1]?.headers).not.toHaveProperty('Content-Type')
  })
})

describe('CSRF 토큰', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; path=/'
  })

  it('쿠키에 있으면 헤더로 되돌려준다', async () => {
    document.cookie = 'XSRF-TOKEN=token-value; path=/'
    const spy = mockFetch()

    await api.post('/api/vehicles', { plateNumber: '12가3456' })

    expect(spy.mock.calls[0][1]?.headers).toMatchObject({ 'X-XSRF-TOKEN': 'token-value' })
  })

  it('쿠키가 없으면 헤더를 붙이지 않는다', async () => {
    // 첫 요청은 아직 토큰을 받기 전이다. 빈 값을 보내면 서버가 쿠키와 비교해 막는다
    const spy = mockFetch()

    await api.get('/api/users/me')

    expect(spy.mock.calls[0][1]?.headers).not.toHaveProperty('X-XSRF-TOKEN')
  })

  it('다른 쿠키가 섞여 있어도 값을 정확히 집는다', async () => {
    document.cookie = 'other=1; path=/'
    document.cookie = 'XSRF-TOKEN=abc%2Fdef; path=/'
    const spy = mockFetch()

    await api.post('/api/users/logout')

    // 쿠키 값은 인코딩되어 저장될 수 있다
    expect(spy.mock.calls[0][1]?.headers).toMatchObject({ 'X-XSRF-TOKEN': 'abc/def' })
  })
})

describe('응답 처리', () => {
  beforeEach(() => {
    vi.unstubAllGlobals()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('204 는 본문을 읽지 않는다', async () => {
    // json() 을 부르면 빈 본문이라 실패한다
    mockFetch(204)
    await expect(api.del('/api/users/me')).resolves.toBeUndefined()
  })
})

describe('서버에 닿지 못했을 때', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('입력 오류와 구분되는 ApiError 로 바꾼다', async () => {
    // fetch 가 던지는 TypeError 는 ApiError 가 아니라, 화면의 폴백 문구로 떨어졌다
    // 백엔드가 꺼져 있어도 '로그인에 실패했습니다' 가 뜨던 자리
    vi.stubGlobal('fetch', vi.fn(async () => {
      throw new TypeError('Failed to fetch')
    }))

    const caught = await api.get('/api/users/me').catch((error: unknown) => error)

    expect(caught).toBeInstanceOf(ApiError)
    expect((caught as ApiError).status).toBe(NETWORK_ERROR_STATUS)
    expect((caught as ApiError).message).toContain('연결하지 못했습니다')
  })

  it('네트워크 실패를 세션 만료로 오해하지 않는다', async () => {
    // status 가 401 이 아니므로 전역 핸들러가 돌면 안 된다 — 돌면 로그아웃된다
    vi.stubGlobal('fetch', vi.fn(async () => {
      throw new TypeError('Failed to fetch')
    }))

    const caught = await api.get('/api/vehicles').catch((error: unknown) => error)

    expect((caught as ApiError).status).not.toBe(401)
  })
})
