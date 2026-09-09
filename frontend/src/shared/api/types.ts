/**
 * 어느 기능에도 속하지 않는, 백엔드 com.odolog.app.common.dto 에 대응하는 타입.
 *
 * 기능별 DTO(user/vehicle/maintenance)는 여기 두지 않는다.
 * shared 가 features 를 알면 의존 방향이 뒤집히기 때문이다.
 * 각각 features/<기능>/api/types.ts 에 있다.
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
