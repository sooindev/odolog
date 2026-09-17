/**
 * 어느 기능에도 속하지 않는 타입. 백엔드 com.odolog.app.common.dto 대응
 * 기능별 DTO 는 여기 두지 않음 — shared 가 features 를 알면 의존 방향이 뒤집힘
 */

/** 백엔드 common.dto.response.PageResponse */
export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}

/** 백엔드 common.dto.response.ErrorResponse */
export interface ErrorResponse {
  message: string
}
