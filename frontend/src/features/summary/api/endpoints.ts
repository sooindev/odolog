import { api } from '@/shared/api/client'
import type { HomeData } from '@/features/summary/api/types'

/** 홈 요약 한 번에. 정비·주유를 함께 담아 차량의 하위 자원이 아님 */
export function fetchSummary() {
  return api.get<HomeData>('/api/summary')
}
