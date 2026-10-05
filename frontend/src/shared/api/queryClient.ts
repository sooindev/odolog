import { QueryClient } from '@tanstack/react-query'

/**
 * 앱 전체의 조회 캐시
 * 재시도 없음(404·401 을 되풀이하지 않음), 창 포커스 재조회 없음(입력 중 값이 바뀌지 않게)
 * 401 은 client.ts 의 전역 처리기가 그대로 맡음
 */
export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        refetchOnWindowFocus: false,
      },
      mutations: {
        retry: false,
      },
    },
  })
}
